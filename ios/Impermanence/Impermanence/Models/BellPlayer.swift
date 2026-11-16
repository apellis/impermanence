//
//  BellPlayer.swift
//  Impermanence
//
//  BellEngine is the only code that makes bell sound. It runs one timing
//  session at a time (an active day or a Quick Sit) and follows the rules in
//  docs/BEHAVIOR.md; the timing decisions themselves are in BellSchedule.swift.
//
//  How bells always ring:
//  * The audio session uses the .playback category, which ignores the
//    Ring/Silent switch and is not affected by Focus.
//  * While a session runs, a silent loop keeps the audio session playing, so
//    with the "audio" background mode the app keeps running with the screen
//    off and rings each chime itself, starting it on the exact instant.
//  * As a safety net in case iOS ends the process anyway, every upcoming chime
//    (next 24 h, at most 60) is also a time-sensitive local notification with
//    the bell sound. The engine removes a chime's notification just before it
//    plays that chime itself, so a bell never rings twice.
//  * The session is persisted. If iOS ends the process, the next launch
//    resumes the same session (and its screen). Leaving the screen, or
//    swiping the app away, ends the session and removes every notification.
//

import Foundation
import AVFoundation
import UserNotifications
import UIKit

@MainActor
final class BellEngine: NSObject, ObservableObject {
    static let shared = BellEngine()

    /// The running session, if any.
    @Published private(set) var session: TimingSession?
    /// A session resumed at launch, for the UI to navigate to. Cleared by the UI.
    @Published var resumedSession: TimingSession?
    /// False when the user has turned notifications off: bells still ring
    /// while the app runs, but the safety net is gone.
    @Published private(set) var notificationsAllowed = true
    /// System output volume, 0...1.
    @Published private(set) var outputVolume: Float = 1

    /// Only maintained in UI tests (launch argument --uitesting): a summary of
    /// the engine's state that UI tests can read.
    @Published private(set) var debugStatus = "idle pending=0"
    static let isUITesting = ProcessInfo.processInfo.arguments.contains("--uitesting")

    /// Looks up a day plan by id. Set by the app once days are loaded.
    var dayProvider: (UUID) -> Day? = { _ in nil }

    private struct ScheduledChime {
        let player: AVAudioPlayer
        let playAt: Date
    }

    private var handled: Set<String> = []
    private var tickTimer: Timer?
    private var keepAlivePlayer: AVAudioPlayer?
    private var scheduled: [ScheduledChime] = []
    private var registeredNotifications: [String: Date] = [:]
    private var criticalAlertsAllowed = false
    private var observers: [NSObjectProtocol] = []
    private var volumeObservation: NSKeyValueObservation?

    private let defaults = UserDefaults.standard
    private static let sessionKey = "BellEngine.session"
    private static let handledKey = "BellEngine.handledChimes"
    static let notificationPrefix = "impermanence.chime."
    private static let tickInterval: TimeInterval = 0.25

    private override init() {
        super.init()
        observeSystemEvents()
    }

    // MARK: - Sessions

    /// Starts timing `day`, or resumes the running session if it is already
    /// timing this day. Returns the occurrence date being timed.
    @discardableResult
    func startOrResumeDay(_ day: Day, loopDays: Bool, now: Date = .now) -> CalendarDay {
        if case .day(var s)? = session, s.dayID == day.id {
            if s.loopDays != loopDays {
                s.loopDays = loopDays
                session = .day(s)
                persistSession()
                tick()
            }
            return s.anchor
        }
        let anchor = DaySchedule.initialOccurrence(of: day, at: now, calendar: .autoupdatingCurrent).date
        begin(.day(.init(dayID: day.id, openedAt: now, anchor: anchor, loopDays: loopDays)), handled: [])
        return anchor
    }

    func setLoopDays(_ loopDays: Bool) {
        guard case .day(var s)? = session, s.loopDays != loopDays else { return }
        s.loopDays = loopDays
        session = .day(s)
        persistSession()
        tick()
    }

    func startQuickSit(minutes: Int, soundId: Int, startChimes: Int, endChimes: Int, now: Date = .now) -> TimingSession.QuickSitSession {
        let s = TimingSession.QuickSitSession(startedAt: now, durationSeconds: max(1, minutes) * 60, soundId: soundId,
                                              startChimes: startChimes, endChimes: endChimes)
        begin(.quickSit(s), handled: [])
        return s
    }

    /// The user left the session's screen: no bell of this session rings from
    /// now on. A chime that is already sounding finishes.
    func endSession() {
        guard session != nil else { return }
        session = nil
        handled = []
        persistSession()
        persistHandled()
        stopMachinery()
        removeAllChimeNotifications()
        updateDebugStatus(force: true)
    }

    /// Plays a bell right away, outside any schedule ("Ring now").
    func ringNow(_ bell: Bell, now: Date = .now) {
        activateAudioSession()
        let b = bell.sanitized()
        for ring in 0..<b.numRings {
            play(soundId: b.soundId, at: now.addingTimeInterval(TimeInterval(ring) * DaySchedule.chimeSpacing))
        }
    }

    /// Called at launch once day plans are loaded: resumes a session that the
    /// system interrupted, or, with no session, removes any stray
    /// notifications left by an earlier version or an abrupt exit.
    func resumeOrCleanUp() async {
        guard session == nil else { return }
        if Self.isUITesting, ProcessInfo.processInfo.arguments.contains("--uitesting-reset-session") {
            persistSession(nil)
        }
        guard let data = defaults.data(forKey: Self.sessionKey),
              let saved = try? JSONDecoder().decode(TimingSession.self, from: data) else {
            removeAllChimeNotifications(includingLegacy: true)
            return
        }
        if case .day(let s) = saved, dayProvider(s.dayID) == nil {
            persistSession(nil)
            removeAllChimeNotifications(includingLegacy: true)
            return
        }
        if saved.isFinished(at: .now, day: day(for: saved), calendar: .autoupdatingCurrent) {
            persistSession(nil)
            removeAllChimeNotifications(includingLegacy: true)
            return
        }
        // Chimes the safety net already rang while the process was gone must
        // not ring again as a catch-up.
        var alreadyRung = Set(defaults.stringArray(forKey: Self.handledKey) ?? [])
        let delivered = await UNUserNotificationCenter.current().deliveredNotifications()
        for n in delivered where n.request.identifier.hasPrefix(Self.notificationPrefix) {
            alreadyRung.insert(String(n.request.identifier.dropFirst(Self.notificationPrefix.count)))
        }
        begin(saved, handled: alreadyRung)
        resumedSession = saved
    }

    /// The app is being terminated (for example swiped away in the app
    /// switcher): treat it as leaving the screen.
    func appWillTerminate() {
        endSession()
    }

    private func begin(_ newSession: TimingSession, handled initial: Set<String>) {
        stopMachinery()
        removeAllChimeNotifications()
        session = newSession
        handled = initial
        persistSession()
        persistHandled()
        activateAudioSession()
        startKeepAlive()
        startTicking()
        refreshNotificationSettings(requestIfNeeded: true)
        tick()
    }

    /// Every bell of a finite session has started: stop the machinery but let
    /// the last chimes sound out.
    private func finishNaturally() {
        session = nil
        handled = []
        persistSession()
        persistHandled()
        tickTimer?.invalidate()
        tickTimer = nil
        keepAlivePlayer?.stop()
        keepAlivePlayer = nil
        removeAllChimeNotifications()
        updateDebugStatus(force: true)
    }

    private func day(for session: TimingSession) -> Day? {
        if case .day(let s) = session { return dayProvider(s.dayID) }
        return nil
    }

    // MARK: - Ticking

    private func startTicking() {
        tickTimer?.invalidate()
        let timer = Timer(timeInterval: Self.tickInterval, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.tick() }
        }
        timer.tolerance = 0.05
        // .common keeps ticking while the user scrolls.
        RunLoop.main.add(timer, forMode: .common)
        tickTimer = timer
    }

    private func tick(now: Date = .now) {
        guard let session else { return }
        let calendar = Calendar.autoupdatingCurrent
        let day = day(for: session)
        if case .day = session, day == nil {
            endSession()
            return
        }
        let window = now.addingTimeInterval(-(ChimePlanner.catchUpLimit + 120))...now.addingTimeInterval(ChimePlanner.notificationHorizon + 60)
        let events = session.events(day: day, window: window, calendar: calendar)
        let chimes = DaySchedule.chimes(for: events)

        let decision = ChimePlanner.decide(chimes: chimes, handled: handled, sessionStart: session.openedAt, now: now)
        if !decision.play.isEmpty {
            activateAudioSession()
            for playback in decision.play {
                removeNotifications(ids: [playback.chime.id])
                play(soundId: playback.chime.soundId, at: playback.playAt)
            }
        }
        let windowIDs = Set(chimes.map(\.id))
        let newHandled = handled.intersection(windowIDs).union(decision.handled)
        if newHandled != handled {
            handled = newHandled
            persistHandled()
        }

        reconcileNotifications(events: events, chimes: chimes, session: session, day: day, now: now)

        if session.isFinished(at: now, day: day, calendar: calendar) {
            finishNaturally()
        }
        updateDebugStatus()
    }

    private var lastDebugUpdate = Date.distantPast

    private func updateDebugStatus(force: Bool = false) {
        guard Self.isUITesting, force || Date.now.timeIntervalSince(lastDebugUpdate) > 1 else { return }
        lastDebugUpdate = .now
        let kind: String
        switch session {
        case .day?: kind = "day"
        case .quickSit?: kind = "quicksit"
        case nil: kind = "idle"
        }
        UNUserNotificationCenter.current().getPendingNotificationRequests { requests in
            let count = requests.count
            Task { @MainActor in self.debugStatus = "\(kind) pending=\(count)" }
        }
    }

    // MARK: - Audio

    private func activateAudioSession() {
        let audio = AVAudioSession.sharedInstance()
        do {
            // .playback ignores the Ring/Silent switch; mixing keeps the
            // user's own audio playing and keeps other apps from
            // interrupting this session.
            if audio.category != .playback || !audio.categoryOptions.contains(.mixWithOthers) {
                try audio.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            }
            try audio.setActive(true)
        } catch {
            NSLog("BellEngine: audio session activation failed: \(error)")
        }
        outputVolume = audio.outputVolume
    }

    private func startKeepAlive() {
        guard keepAlivePlayer == nil,
              let url = Bundle.main.url(forResource: "silence", withExtension: "caf"),
              let player = try? AVAudioPlayer(contentsOf: url) else { return }
        player.numberOfLoops = -1
        player.volume = 1 // the file itself is silent
        player.prepareToPlay()
        player.play()
        keepAlivePlayer = player
    }

    private func play(soundId: Int, at instant: Date) {
        guard let url = BellCatalog.sound(for: soundId).url,
              let player = try? AVAudioPlayer(contentsOf: url) else {
            NSLog("BellEngine: missing sound \(soundId)")
            return
        }
        player.delegate = self
        player.prepareToPlay()
        let delay = instant.timeIntervalSinceNow
        if delay > 0.005 {
            player.play(atTime: player.deviceCurrentTime + delay)
        } else {
            player.play()
        }
        scheduled.append(ScheduledChime(player: player, playAt: instant))
    }

    /// Stops ticking and the keep-alive loop and cancels chimes that have not
    /// started yet. Chimes already sounding play out.
    private func stopMachinery(now: Date = .now) {
        tickTimer?.invalidate()
        tickTimer = nil
        keepAlivePlayer?.stop()
        keepAlivePlayer = nil
        for s in scheduled where s.playAt > now {
            s.player.stop()
        }
        scheduled.removeAll { $0.playAt > now || !$0.player.isPlaying }
    }

    // MARK: - Notification safety net

    private func reconcileNotifications(events: [BellEvent], chimes: [Chime], session: TimingSession, day: Day?, now: Date) {
        guard notificationsAllowed else { return }
        let desired = ChimePlanner.notificationChimes(chimes: chimes, handled: handled, sessionStart: session.openedAt, now: now)
        let desiredInstants = Dictionary(desired.map { ($0.id, $0.instant) }, uniquingKeysWith: { a, _ in a })
        let stale = registeredNotifications.filter { desiredInstants[$0.key] != $0.value }.map(\.key)
        if !stale.isEmpty { removeNotifications(ids: stale) }

        let eventsByID = Dictionary(events.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let title: String
        switch session {
        case .day: title = day?.name.isEmpty == false ? day!.name : "Impermanence"
        case .quickSit: title = "Quick Sit"
        }
        let center = UNUserNotificationCenter.current()
        for chime in desired where registeredNotifications[chime.id] != chime.instant {
            let content = UNMutableNotificationContent()
            content.title = title
            content.body = eventsByID[chime.eventID]?.label ?? "Bell"
            content.threadIdentifier = "impermanence.bells"
            content.relevanceScore = 1
            let soundName = UNNotificationSoundName(BellCatalog.sound(for: chime.soundId).fileName)
            if criticalAlertsAllowed {
                // Requires Apple's Critical Alerts entitlement; until it is
                // granted this branch is never taken.
                content.sound = .criticalSoundNamed(soundName, withAudioVolume: 1)
                content.interruptionLevel = .critical
            } else {
                content.sound = UNNotificationSound(named: soundName)
                content.interruptionLevel = .timeSensitive
            }
            let trigger = UNTimeIntervalNotificationTrigger(timeInterval: max(1, chime.instant.timeIntervalSince(now)), repeats: false)
            center.add(UNNotificationRequest(identifier: Self.notificationPrefix + chime.id, content: content, trigger: trigger))
            registeredNotifications[chime.id] = chime.instant
        }
    }

    private func removeNotifications(ids: [String]) {
        let present = ids.filter { registeredNotifications[$0] != nil }
        guard !present.isEmpty else { return }
        let full = present.map { Self.notificationPrefix + $0 }
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: full)
        for id in present { registeredNotifications[id] = nil }
    }

    /// Removes every pending notification. The app schedules no notifications
    /// other than bells, so this also removes the repeating ones that earlier
    /// versions scheduled. It is one synchronous call, so it is ordered before
    /// any request added afterwards.
    private func removeAllChimeNotifications(includingLegacy: Bool = false) {
        registeredNotifications = [:]
        UNUserNotificationCenter.current().removeAllPendingNotificationRequests()
        if includingLegacy {
            defaults.removeObject(forKey: "BellNotificationScheduler.quickSitIDs")
            defaults.removeObject(forKey: "BellNotificationScheduler.dayIDs")
        }
    }

    private func refreshNotificationSettings(requestIfNeeded: Bool) {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            let status = settings.authorizationStatus
            let critical = settings.criticalAlertSetting == .enabled
            Task { @MainActor in
                if status == .notDetermined && requestIfNeeded {
                    let granted = (try? await center.requestAuthorization(options: [.alert, .sound, .criticalAlert])) ?? false
                    let after = await center.notificationSettings()
                    // Anything registered before the answer was refused: redo it.
                    self.applyNotificationSettings(allowed: granted, critical: after.criticalAlertSetting == .enabled, force: true)
                } else {
                    self.applyNotificationSettings(allowed: status == .authorized || status == .provisional || status == .ephemeral,
                                                   critical: critical)
                }
            }
        }
    }

    private func applyNotificationSettings(allowed: Bool, critical: Bool, force: Bool = false) {
        let changed = force || allowed != notificationsAllowed || critical != criticalAlertsAllowed
        notificationsAllowed = allowed
        criticalAlertsAllowed = critical
        if changed {
            registeredNotifications = [:]
            removeAllChimeNotifications()
            tick()
        }
    }

    // MARK: - Persistence

    private func persistSession() { persistSession(session) }

    private func persistSession(_ value: TimingSession?) {
        if let value, let data = try? JSONEncoder().encode(value) {
            defaults.set(data, forKey: Self.sessionKey)
        } else {
            defaults.removeObject(forKey: Self.sessionKey)
        }
    }

    private func persistHandled() {
        defaults.set(Array(handled), forKey: Self.handledKey)
    }

    // MARK: - System events

    private func observeSystemEvents() {
        let nc = NotificationCenter.default
        func on(_ name: Notification.Name, _ action: @escaping @MainActor (Notification) -> Void) {
            observers.append(nc.addObserver(forName: name, object: nil, queue: .main) { note in
                MainActor.assumeIsolated { action(note) }
            })
        }
        on(AVAudioSession.interruptionNotification) { [weak self] note in
            guard let self, self.session != nil,
                  let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                  AVAudioSession.InterruptionType(rawValue: raw) == .ended else { return }
            // Resume regardless of the shouldResume hint: bells must ring.
            self.activateAudioSession()
            self.keepAlivePlayer?.play()
            self.tick()
        }
        on(AVAudioSession.mediaServicesWereResetNotification) { [weak self] _ in
            guard let self, self.session != nil else { return }
            self.keepAlivePlayer = nil
            self.scheduled.removeAll()
            self.activateAudioSession()
            self.startKeepAlive()
            self.tick()
        }
        // Clock or time zone changes: recompute right away.
        for name in [UIApplication.significantTimeChangeNotification,
                     Notification.Name.NSSystemTimeZoneDidChange,
                     Notification.Name.NSSystemClockDidChange] {
            on(name) { [weak self] _ in
                NSTimeZone.resetSystemTimeZone()
                self?.tick()
            }
        }
        on(UIApplication.willEnterForegroundNotification) { [weak self] _ in
            self?.refreshNotificationSettings(requestIfNeeded: false)
            self?.tick()
        }
        on(UIApplication.didEnterBackgroundNotification) { [weak self] _ in
            self?.tick()
        }
        volumeObservation = AVAudioSession.sharedInstance().observe(\.outputVolume, options: [.new]) { [weak self] _, change in
            let value = change.newValue ?? 1
            Task { @MainActor in self?.outputVolume = value }
        }
    }
}

extension BellEngine: AVAudioPlayerDelegate {
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.scheduled.removeAll { $0.player === player }
        }
    }
}
