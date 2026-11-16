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
//  * As a backup in case iOS stops the process anyway, upcoming bells (next
//    2 h) are also time-sensitive local notifications with the bell sound,
//    one per bell (up to 6 chimes in one sound file). The engine removes a
//    bell's notification just before it plays that bell itself, so normally
//    no notification ever fires. Fired ones are cleared from Notification
//    Center as soon as the app runs again; in the foreground they only sound.
//  * While another audio source has interrupted the session (a phone call,
//    Siri, an alarm), the app can't play, so it leaves the notifications in
//    place and lets iOS ring them.
//  * The session is persisted. If iOS ends the process, the next launch
//    resumes the same session (and its screen). Leaving the screen, or
//    swiping the app away, ends the session and removes every notification.
//

import Foundation
import AVFoundation
import UserNotifications
import UIKit

// MARK: - Back ends (real ones below; tests use fakes)

/// Plays bell sounds and keeps the audio session alive.
@MainActor
protocol BellAudioOutput: AnyObject {
    /// Activates the playback audio session. False if iOS refused (for
    /// example during a phone call).
    func activate() -> Bool
    var outputVolume: Float { get }
    /// True when audio goes to headphones (wired, Bluetooth, AirPlay is not).
    var isUsingHeadphones: Bool { get }
    func startKeepAlive()
    func resumeKeepAlive()
    func stopKeepAlive()
    /// Starts one strike of the sound at `instant` (now, if it has passed).
    func play(soundId: Int, at instant: Date, now: Date)
    /// Cancels strikes that haven't started yet. Sounding ones play out.
    func cancelNotStarted(now: Date)
    /// After a media-services reset every player is gone.
    func reset()
}

struct BellNotificationStatus: Equatable {
    var notDetermined = false
    var allowed = true
    var soundsOn = true
    var timeSensitiveOn = true
    var criticalOn = false
}

struct BellNotificationRequest: Equatable {
    let id: String
    let title: String
    let body: String
    let soundFile: String
    let critical: Bool
    let fireIn: TimeInterval
}

/// Schedules and clears the backup notifications.
@MainActor
protocol BellNotificationScheduler: AnyObject {
    func add(_ request: BellNotificationRequest)
    func removePending(ids: [String])
    /// Synchronous, so it is ordered before any request added afterwards.
    func removeAllPending()
    func removeAllDelivered()
    func deliveredIDs() async -> [String]
    func status() async -> BellNotificationStatus
    func requestAuthorization() async -> Bool
    func pendingCount() async -> Int
}

// MARK: - Engine

@MainActor
final class BellEngine: NSObject, ObservableObject {
    static let shared = BellEngine(audio: SystemBellAudio(), notifications: SystemBellNotifications(),
                                   state: .standard, defaults: .standard, clock: { .now }, observeSystem: true)

    /// The running session, if any.
    @Published private(set) var session: TimingSession?
    /// A session resumed at launch, for the UI to navigate to. Cleared by the UI.
    @Published var resumedSession: TimingSession?
    /// False when the user has turned notifications off: bells still ring
    /// while the app runs, but the backup is gone.
    @Published private(set) var notificationsAllowed = true
    /// False when notifications are allowed but their sound or Time
    /// Sensitive delivery is off, so a backup could be silent or held back.
    @Published private(set) var notificationSoundsOn = true
    @Published private(set) var timeSensitiveOn = true
    /// System output volume, 0...1.
    @Published private(set) var outputVolume: Float = 1
    /// Bells currently play in headphones, not the speaker.
    @Published private(set) var usingHeadphones = false
    /// Another audio source (a call, Siri, an alarm) holds the audio session.
    @Published private(set) var audioInterrupted = false

    /// Only maintained in UI tests (launch argument --uitesting): a summary of
    /// the engine's state that UI tests can read.
    @Published private(set) var debugStatus = "idle pending=0"
    /// Test hooks exist only in Debug builds, so a release build ignores
    /// these launch arguments.
    static let isUITesting: Bool = {
        #if DEBUG
        return ProcessInfo.processInfo.arguments.contains("--uitesting")
        #else
        return false
        #endif
    }()

    /// Looks up a day plan by id. Set by the app once days are loaded.
    var dayProvider: (UUID) -> Day? = { _ in nil }

    private let audio: BellAudioOutput
    private let notifications: BellNotificationScheduler
    private let state: BellStateFile
    private let defaults: UserDefaults
    private let clock: () -> Date
    private let calendar = Calendar.autoupdatingCurrent

    private var handled: Set<String> = []
    private var tickTimer: Timer?
    /// Backup notifications this engine registered: group id -> fire instant.
    private(set) var registeredNotifications: [String: Date] = [:]
    private var criticalAlertsAllowed = false
    private var observers: [NSObjectProtocol] = []
    private var volumeObservation: NSKeyValueObservation?

    /// Where versions before 1.5 kept the session; read once, then removed.
    private static let legacySessionKey = "BellEngine.session"
    private static let legacyHandledKey = "BellEngine.handledChimes"
    static let notificationPrefix = "impermanence.chime."
    private static let tickInterval: TimeInterval = 0.25

    init(audio: BellAudioOutput, notifications: BellNotificationScheduler,
         state: BellStateFile, defaults: UserDefaults, clock: @escaping () -> Date, observeSystem: Bool) {
        self.state = state
        self.audio = audio
        self.notifications = notifications
        self.defaults = defaults
        self.clock = clock
        super.init()
        if observeSystem { observeSystemEvents() }
    }

    // MARK: - Sessions

    /// Starts timing `day`, or resumes the running session if it is already
    /// timing this day. Returns the occurrence date being timed.
    @discardableResult
    func startOrResumeDay(_ day: Day, loopDays: Bool, now: Date? = nil) -> CalendarDay {
        let now = now ?? clock()
        if case .day(var s)? = session, s.dayID == day.id {
            if s.loopDays != loopDays {
                s.loopDays = loopDays
                session = .day(s)
                persistSession()
                tick()
            }
            return s.anchor
        }
        let anchor = DaySchedule.initialOccurrence(of: day, at: now, calendar: calendar).date
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

    func startQuickSit(minutes: Int, soundId: Int, startChimes: Int, endChimes: Int, now: Date? = nil) -> TimingSession.QuickSitSession {
        let s = TimingSession.QuickSitSession(startedAt: now ?? clock(), durationSeconds: max(1, minutes) * 60, soundId: soundId,
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
        removeAllBellNotifications()
        updateDebugStatus(force: true)
    }

    /// Plays a bell right away, outside any schedule ("Ring now").
    func ringNow(_ bell: Bell) {
        _ = audio.activate()
        let now = clock()
        let b = bell.sanitized()
        for ring in 0..<b.numRings {
            audio.play(soundId: b.soundId, at: now.addingTimeInterval(TimeInterval(ring) * DaySchedule.chimeSpacing), now: now)
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
        let now = clock()
        let stored = loadStoredState()
        guard let saved = stored.session,
              !(day(for: saved) == nil && saved.isDay),
              !saved.isFinished(at: now, day: day(for: saved), calendar: calendar) else {
            persistSession(nil)
            removeAllBellNotifications(includingLegacy: true)
            return
        }
        // Bells the backup already rang while the process was gone must not
        // ring again as a catch-up.
        var alreadyRung = Set(stored.handled)
        alreadyRung.formUnion(await deliveredChimeIDs())
        notifications.removeAllDelivered()
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
        removeAllBellNotifications()
        session = newSession
        handled = initial
        persistSession()
        persistHandled()
        audioInterrupted = !audio.activate()
        refreshRoute()
        audio.startKeepAlive()
        startTicking()
        Task { await self.refreshNotificationSettings(requestIfNeeded: true) }
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
        audio.stopKeepAlive()
        removeAllBellNotifications()
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

    /// One step of the engine: plays what is due and keeps the backup
    /// notifications in line. Internal so tests can drive it with their clock.
    func tick() {
        guard let session else { return }
        let now = clock()
        let day = day(for: session)
        if case .day = session, day == nil {
            endSession()
            return
        }
        let window = now.addingTimeInterval(-(ChimePlanner.catchUpLimit + 120))...now.addingTimeInterval(ChimePlanner.notificationHorizon + 60)
        let events = session.events(day: day, window: window, calendar: calendar)
        let chimes = DaySchedule.chimes(for: events)

        // While interrupted the app can't sound: it leaves the due bells to
        // their notifications and decides nothing, so none is marked played.
        // iOS doesn't always say when an interruption ends, so try again now
        // and then.
        if audioInterrupted, now.timeIntervalSince(lastRecoveryAttempt) > 5 {
            lastRecoveryAttempt = now
            if audio.activate() {
                Task { await self.handleInterruption(began: false) }
            }
        }
        if !audioInterrupted {
            let decision = ChimePlanner.decide(chimes: chimes, handled: handled, sessionStart: session.openedAt, now: now)
            if !decision.play.isEmpty {
                if audio.activate() {
                    for playback in decision.play {
                        removeNotifications(ids: [ChimePlanner.groupID(of: playback.chime)])
                        audio.play(soundId: playback.chime.soundId, at: playback.playAt, now: now)
                    }
                    handled.formUnion(decision.handled)
                } else {
                    // iOS refused the audio session: treat it as an interruption.
                    audioInterrupted = true
                }
            } else {
                handled.formUnion(decision.handled)
            }
        }
        let windowIDs = Set(chimes.map(\.id))
        let pruned = handled.intersection(windowIDs)
        if pruned != handled || !defaultsHandledMatches() {
            handled = pruned
            persistHandled()
        }

        reconcileNotifications(events: events, chimes: chimes, session: session, day: day, now: now)

        if session.isFinished(at: now, day: day, calendar: calendar) {
            finishNaturally()
        }
        updateDebugStatus()
    }

    private var lastPersistedHandled: Set<String> = []
    private var lastRecoveryAttempt = Date.distantPast
    private func defaultsHandledMatches() -> Bool { lastPersistedHandled == handled }

    // MARK: - Interruptions and route

    /// A phone call, Siri or an alarm took or gave back the audio session.
    /// Internal so tests can drive it.
    func handleInterruption(began: Bool) async {
        guard session != nil else { return }
        if began {
            audioInterrupted = true
            lastRecoveryAttempt = clock()
            return
        }
        audioInterrupted = !audio.activate()
        audio.resumeKeepAlive()
        // Bells iOS rang as notifications during the interruption must not
        // ring again; then clear them from Notification Center.
        handled.formUnion(await deliveredChimeIDs())
        notifications.removeAllDelivered()
        persistHandled()
        tick()
    }

    private func refreshRoute() {
        usingHeadphones = audio.isUsingHeadphones
        outputVolume = audio.outputVolume
    }

    private func updateDebugStatus(force: Bool = false) {
        guard Self.isUITesting, force || clock().timeIntervalSince(lastDebugUpdate) > 1 else { return }
        lastDebugUpdate = clock()
        let kind: String
        switch session {
        case .day?: kind = "day"
        case .quickSit?: kind = "quicksit"
        case nil: kind = "idle"
        }
        Task {
            let count = await notifications.pendingCount()
            self.debugStatus = "\(kind) pending=\(count)"
        }
    }
    private var lastDebugUpdate = Date.distantPast

    /// Stops ticking and the keep-alive loop and cancels chimes that have not
    /// started yet. Chimes already sounding play out.
    private func stopMachinery() {
        tickTimer?.invalidate()
        tickTimer = nil
        audio.stopKeepAlive()
        audio.cancelNotStarted(now: clock())
    }

    // MARK: - Backup notifications

    private func reconcileNotifications(events: [BellEvent], chimes: [Chime], session: TimingSession, day: Day?, now: Date) {
        guard notificationsAllowed else { return }
        let desired = ChimePlanner.notificationGroups(chimes: chimes, handled: handled, sessionStart: session.openedAt, now: now)
        let desiredInstants = Dictionary(desired.map { ($0.id, $0.instant) }, uniquingKeysWith: { a, _ in a })
        let stale = registeredNotifications.filter { desiredInstants[$0.key] != $0.value }.map(\.key)
        if !stale.isEmpty { removeNotifications(ids: stale) }

        let eventsByID = Dictionary(events.map { ($0.id, $0) }, uniquingKeysWith: { a, _ in a })
        let title: String
        switch session {
        case .day: title = day?.name.isEmpty == false ? day!.name : "Impermanence"
        case .quickSit: title = "Quick Sit"
        }
        for group in desired where registeredNotifications[group.id] != group.instant {
            let sound = BellCatalog.sound(for: group.soundId).notificationFileName(chimes: group.chimeIDs.count)
            notifications.add(BellNotificationRequest(id: Self.notificationPrefix + group.id,
                                                      title: title,
                                                      body: eventsByID[group.eventID]?.label ?? "Bell",
                                                      soundFile: sound,
                                                      critical: criticalAlertsAllowed,
                                                      fireIn: max(1, group.instant.timeIntervalSince(now))))
            registeredNotifications[group.id] = group.instant
        }
    }

    private func removeNotifications(ids: [String]) {
        let present = ids.filter { registeredNotifications[$0] != nil }
        guard !present.isEmpty else { return }
        notifications.removePending(ids: present.map { Self.notificationPrefix + $0 })
        for id in present { registeredNotifications[id] = nil }
    }

    /// Removes every pending and delivered notification. The app posts no
    /// notifications other than bells, so this also removes the repeating
    /// ones that earlier versions scheduled.
    private func removeAllBellNotifications(includingLegacy: Bool = false) {
        registeredNotifications = [:]
        notifications.removeAllPending()
        notifications.removeAllDelivered()
        if includingLegacy {
            defaults.removeObject(forKey: "BellNotificationScheduler.quickSitIDs")
            defaults.removeObject(forKey: "BellNotificationScheduler.dayIDs")
        }
    }

    /// Chime ids that delivered backup notifications already rang.
    private func deliveredChimeIDs() async -> Set<String> {
        var ids = Set<String>()
        for id in await notifications.deliveredIDs() where id.hasPrefix(Self.notificationPrefix) {
            let rest = String(id.dropFirst(Self.notificationPrefix.count))
            let fromGroup = ChimePlanner.chimeIDs(ofGroup: rest)
            if fromGroup.isEmpty {
                ids.insert(rest) // an older version's per-chime id
            } else {
                ids.formUnion(fromGroup)
            }
        }
        return ids
    }

    func refreshNotificationSettings(requestIfNeeded: Bool) async {
        var status = await notifications.status()
        var force = false
        if status.notDetermined && requestIfNeeded {
            _ = await notifications.requestAuthorization()
            status = await notifications.status()
            // Anything registered before the answer was refused: redo it.
            force = true
        }
        let changed = force || status.allowed != notificationsAllowed || status.criticalOn != criticalAlertsAllowed
        notificationsAllowed = status.allowed
        notificationSoundsOn = status.soundsOn
        timeSensitiveOn = status.timeSensitiveOn
        criticalAlertsAllowed = status.criticalOn
        if changed {
            registeredNotifications = [:]
            notifications.removeAllPending()
            tick()
        }
    }

    // MARK: - Persistence

    private func persistSession() { persistSession(session) }

    private func persistSession(_ value: TimingSession?) {
        state.save(.init(session: value, handled: value == nil ? [] : Array(handled)))
    }

    private func persistHandled() {
        state.save(.init(session: session, handled: Array(handled)))
        lastPersistedHandled = handled
    }

    private func loadStoredState() -> BellStateFile.State {
        if let stored = state.load() { return stored }
        // Migrate a session saved by an earlier version.
        defer {
            defaults.removeObject(forKey: Self.legacySessionKey)
            defaults.removeObject(forKey: Self.legacyHandledKey)
        }
        let session = defaults.data(forKey: Self.legacySessionKey).flatMap { try? JSONDecoder().decode(TimingSession.self, from: $0) }
        return .init(session: session, handled: defaults.stringArray(forKey: Self.legacyHandledKey) ?? [])
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
            guard let self,
                  let raw = note.userInfo?[AVAudioSessionInterruptionTypeKey] as? UInt,
                  let type = AVAudioSession.InterruptionType(rawValue: raw) else { return }
            // Resume regardless of the shouldResume hint: bells must ring.
            Task { await self.handleInterruption(began: type == .began) }
        }
        on(AVAudioSession.mediaServicesWereResetNotification) { [weak self] _ in
            guard let self, self.session != nil else { return }
            self.audio.reset()
            self.audioInterrupted = !self.audio.activate()
            self.audio.startKeepAlive()
            self.tick()
        }
        on(AVAudioSession.routeChangeNotification) { [weak self] _ in
            self?.refreshRoute()
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
            guard let self else { return }
            // A fired backup has done its job: keep Notification Center clear.
            if self.session != nil {
                Task {
                    self.handled.formUnion(await self.deliveredChimeIDs())
                    self.notifications.removeAllDelivered()
                    await self.refreshNotificationSettings(requestIfNeeded: false)
                    self.tick()
                }
            } else {
                self.notifications.removeAllDelivered()
            }
            self.refreshRoute()
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

private extension TimingSession {
    var isDay: Bool {
        if case .day = self { return true }
        return false
    }
}

// MARK: - Session state

/// The running session and the chimes already handled, in a file that is
/// excluded from device backups, so a session is never restored onto
/// another device (docs/BEHAVIOR.md, Backup).
final class BellStateFile {
    struct State: Codable, Equatable {
        var session: TimingSession?
        var handled: [String]
    }

    let url: URL

    init(url: URL) {
        self.url = url
    }

    static var standard: BellStateFile {
        let support = (try? FileManager.default.url(for: .applicationSupportDirectory, in: .userDomainMask,
                                                    appropriateFor: nil, create: true))
            ?? FileManager.default.temporaryDirectory
        return BellStateFile(url: support.appendingPathComponent("bell-session.json"))
    }

    func load() -> State? {
        guard let data = try? Data(contentsOf: url) else { return nil }
        return try? JSONDecoder().decode(State.self, from: data)
    }

    func save(_ state: State) {
        guard let data = try? JSONEncoder().encode(state) else { return }
        do {
            try data.write(to: url, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
            var values = URLResourceValues()
            values.isExcludedFromBackup = true
            var target = url
            try target.setResourceValues(values)
        } catch {
            NSLog("BellEngine: couldn't save session state: \(error)")
        }
    }
}

// MARK: - Real back ends

@MainActor
final class SystemBellAudio: NSObject, BellAudioOutput, AVAudioPlayerDelegate {
    private struct Scheduled {
        let player: AVAudioPlayer
        let playAt: Date
    }
    private var keepAlivePlayer: AVAudioPlayer?
    private var scheduled: [Scheduled] = []

    func activate() -> Bool {
        let session = AVAudioSession.sharedInstance()
        do {
            // .playback ignores the Ring/Silent switch; mixing keeps the
            // user's own audio playing and keeps other apps from
            // interrupting this session.
            if session.category != .playback || !session.categoryOptions.contains(.mixWithOthers) {
                try session.setCategory(.playback, mode: .default, options: [.mixWithOthers])
            }
            try session.setActive(true)
            return true
        } catch {
            NSLog("BellEngine: audio session activation failed: \(error)")
            return false
        }
    }

    var outputVolume: Float { AVAudioSession.sharedInstance().outputVolume }

    var isUsingHeadphones: Bool {
        let headphones: Set<AVAudioSession.Port> = [.headphones, .bluetoothA2DP, .bluetoothHFP, .bluetoothLE, .usbAudio]
        return AVAudioSession.sharedInstance().currentRoute.outputs.contains { headphones.contains($0.portType) }
    }

    func startKeepAlive() {
        guard keepAlivePlayer == nil,
              let url = Bundle.main.url(forResource: "silence", withExtension: "caf"),
              let player = try? AVAudioPlayer(contentsOf: url) else { return }
        player.numberOfLoops = -1
        player.volume = 1 // the file itself is silent
        player.prepareToPlay()
        player.play()
        keepAlivePlayer = player
    }

    func resumeKeepAlive() {
        keepAlivePlayer?.play()
    }

    func stopKeepAlive() {
        keepAlivePlayer?.stop()
        keepAlivePlayer = nil
    }

    func play(soundId: Int, at instant: Date, now: Date) {
        guard let url = BellCatalog.sound(for: soundId).url,
              let player = try? AVAudioPlayer(contentsOf: url) else {
            NSLog("BellEngine: missing sound \(soundId)")
            return
        }
        player.delegate = self
        player.prepareToPlay()
        let delay = instant.timeIntervalSince(now)
        if delay > 0.005 {
            player.play(atTime: player.deviceCurrentTime + delay)
        } else {
            player.play()
        }
        scheduled.append(Scheduled(player: player, playAt: instant))
    }

    func cancelNotStarted(now: Date) {
        for s in scheduled where s.playAt > now {
            s.player.stop()
        }
        scheduled.removeAll { $0.playAt > now || !$0.player.isPlaying }
    }

    func reset() {
        keepAlivePlayer = nil
        scheduled.removeAll()
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            self.scheduled.removeAll { $0.player === player }
        }
    }
}

@MainActor
final class SystemBellNotifications: BellNotificationScheduler {
    private var center: UNUserNotificationCenter { .current() }

    func add(_ request: BellNotificationRequest) {
        let content = UNMutableNotificationContent()
        content.title = request.title
        content.body = request.body
        content.threadIdentifier = "impermanence.bells"
        content.relevanceScore = 1
        let soundName = UNNotificationSoundName(request.soundFile)
        if request.critical {
            // Requires Apple's Critical Alerts entitlement; until it is
            // granted this branch is never taken.
            content.sound = .criticalSoundNamed(soundName, withAudioVolume: 1)
            content.interruptionLevel = .critical
        } else {
            content.sound = UNNotificationSound(named: soundName)
            content.interruptionLevel = .timeSensitive
        }
        let trigger = UNTimeIntervalNotificationTrigger(timeInterval: request.fireIn, repeats: false)
        center.add(UNNotificationRequest(identifier: request.id, content: content, trigger: trigger))
    }

    func removePending(ids: [String]) {
        center.removePendingNotificationRequests(withIdentifiers: ids)
    }

    func removeAllPending() {
        center.removeAllPendingNotificationRequests()
    }

    func removeAllDelivered() {
        center.removeAllDeliveredNotifications()
    }

    func deliveredIDs() async -> [String] {
        await center.deliveredNotifications().map(\.request.identifier)
    }

    func status() async -> BellNotificationStatus {
        let s = await center.notificationSettings()
        return BellNotificationStatus(
            notDetermined: s.authorizationStatus == .notDetermined,
            allowed: [.authorized, .provisional, .ephemeral].contains(s.authorizationStatus),
            soundsOn: s.soundSetting != .disabled,
            timeSensitiveOn: s.timeSensitiveSetting != .disabled,
            criticalOn: s.criticalAlertSetting == .enabled)
    }

    func requestAuthorization() async -> Bool {
        (try? await center.requestAuthorization(options: [.alert, .sound, .criticalAlert])) ?? false
    }

    func pendingCount() async -> Int {
        await center.pendingNotificationRequests().count
    }
}

/// Bells are sounds: a backup notification that fires while the app is in
/// front only plays its sound, with no banner and nothing in Notification
/// Center.
final class BellNotificationPresenter: NSObject, UNUserNotificationCenterDelegate {
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.sound])
    }
}
