//
//  BellPlayer.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/16/23.
//

import Foundation
import AVFoundation
import UserNotifications

final class BellPlayer: NSObject, AVAudioPlayerDelegate {
    static let shared = BellPlayer()

    private var activePlayers: [AVAudioPlayer] = []
    private let schedulingQueue = DispatchQueue(label: "com.neversink.impermanence.bellplayer")

    private override init() {
        super.init()
    }

    func play(bell: Bell) {
        let sanitizedBell = bell.sanitized()
        let rings = sanitizedBell.numRings
        for index in 0..<rings {
            let delay = TimeInterval(index) * 3.0
            schedulingQueue.asyncAfter(deadline: .now() + delay) { [weak self] in
                self?.playSingleBell(sanitizedBell)
            }
        }
    }

    private func playSingleBell(_ bell: Bell) {
        guard let url = resolveURL(for: bell) else {
            assertionFailure("Failed to find sound file for bell id \(bell.soundId)")
            return
        }

        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.delegate = self
            player.prepareToPlay()

            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.activePlayers.append(player)
                player.play()
            }
        } catch {
            assertionFailure("Failed to play bell: \(error.localizedDescription)")
        }
    }

    private func resolveURL(for bell: Bell) -> URL? {
        for name in bell.sound.resourceCandidates {
            if let url = Bundle.main.url(forResource: name, withExtension: "mp3") {
                return url
            }
        }

        return nil
    }
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.activePlayers.removeAll { $0 === player }
        }
    }
}

enum BellNotificationScheduler {
    private static let quickSitIDsKey = "BellNotificationScheduler.quickSitIDs"
    private static let dayIDsKey = "BellNotificationScheduler.dayIDs"
    private static let maxPendingRequests = 60

    static func scheduleQuickSitCompletion(after duration: TimeInterval, bell: Bell) {
        cancelQuickSit()

        let sanitizedBell = bell.sanitized()
        let baseDelay = max(1, Int(duration.rounded()))
        let requests: [UNNotificationRequest] = (0..<sanitizedBell.numRings).compactMap { ringIndex in
            let trigger = UNTimeIntervalNotificationTrigger(
                timeInterval: TimeInterval(baseDelay + (ringIndex * 3)),
                repeats: false
            )
            let content = notificationContent(
                title: "Quick Sit Complete",
                body: "Session complete",
                bell: sanitizedBell
            )
            return UNNotificationRequest(
                identifier: "quicksit.end.\(ringIndex)",
                content: content,
                trigger: trigger
            )
        }

        register(requests: requests, storageKey: quickSitIDsKey)
    }

    static func prepareAuthorization() {
        ensureAuthorization { _ in }
    }

    static func cancelQuickSit() {
        cancelStoredNotifications(storageKey: quickSitIDsKey)
    }

    static func scheduleDayBells(day: Day, loopDays: Bool, now: Date = Date.now) {
        cancelDayBells()

        struct BellEvent {
            let key: String
            let title: String
            let body: String
            let fireDate: Date
            let bell: Bell
        }

        var events: [BellEvent] = []
        let schedule = day.segmentStartEndTimes
        let startDate = day.startTimeAsDate

        events.append(
            BellEvent(
                key: "start",
                title: day.name,
                body: "Session started",
                fireDate: startDate,
                bell: day.startBell
            )
        )

        for index in day.segments.indices {
            guard index < schedule.count else { continue }
            let segment = day.segments[index]
            events.append(
                BellEvent(
                    key: "segment.\(index)",
                    title: day.name,
                    body: segment.name,
                    fireDate: schedule[index].1,
                    bell: segment.resolvedEndBell(defaultBell: day.defaultBell)
                )
            )
        }

        let calendar = Calendar.current
        var primaryRequests: [UNNotificationRequest] = []
        var extraRingRequests: [UNNotificationRequest] = []

        for event in events.sorted(by: { $0.fireDate < $1.fireDate }) {
            let sanitizedBell = event.bell.sanitized()
            for ringIndex in 0..<sanitizedBell.numRings {
                let ringDate = event.fireDate.addingTimeInterval(TimeInterval(ringIndex * 3))
                if !loopDays && ringDate <= now {
                    continue
                }

                let trigger: UNNotificationTrigger
                if loopDays {
                    let timeOfDay = calendar.dateComponents([.hour, .minute, .second], from: ringDate)
                    trigger = UNCalendarNotificationTrigger(dateMatching: timeOfDay, repeats: true)
                } else {
                    let components = calendar.dateComponents([.year, .month, .day, .hour, .minute, .second], from: ringDate)
                    trigger = UNCalendarNotificationTrigger(dateMatching: components, repeats: false)
                }

                let content = notificationContent(title: event.title, body: event.body, bell: sanitizedBell)
                let request = UNNotificationRequest(
                    identifier: "day.\(day.id.uuidString).\(event.key).\(ringIndex)",
                    content: content,
                    trigger: trigger
                )
                if ringIndex == 0 {
                    primaryRequests.append(request)
                } else {
                    extraRingRequests.append(request)
                }
            }
        }

        let limitedRequests = Array((primaryRequests + extraRingRequests).prefix(maxPendingRequests))
        register(requests: limitedRequests, storageKey: dayIDsKey)
    }

    static func cancelDayBells() {
        cancelStoredNotifications(storageKey: dayIDsKey)
    }

    private static func register(requests: [UNNotificationRequest], storageKey: String) {
        guard !requests.isEmpty else {
            save(ids: [], storageKey: storageKey)
            return
        }

        ensureAuthorization { granted in
            guard granted else {
                save(ids: [], storageKey: storageKey)
                return
            }

            let center = UNUserNotificationCenter.current()
            let ids = requests.map(\.identifier)
            center.removePendingNotificationRequests(withIdentifiers: ids)
            center.removeDeliveredNotifications(withIdentifiers: ids)
            requests.forEach { request in
                center.add(request)
            }
            save(ids: ids, storageKey: storageKey)
        }
    }

    private static func cancelStoredNotifications(storageKey: String) {
        let ids = storedIDs(storageKey: storageKey)
        guard !ids.isEmpty else { return }

        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ids)
        center.removeDeliveredNotifications(withIdentifiers: ids)
        save(ids: [], storageKey: storageKey)
    }

    private static func ensureAuthorization(completion: @escaping (Bool) -> Void) {
        let center = UNUserNotificationCenter.current()
        center.getNotificationSettings { settings in
            switch settings.authorizationStatus {
            case .authorized, .provisional, .ephemeral:
                completion(true)
            case .notDetermined:
                center.requestAuthorization(options: [.alert, .sound]) { granted, _ in
                    completion(granted)
                }
            case .denied:
                completion(false)
            @unknown default:
                completion(false)
            }
        }
    }

    private static func notificationContent(title: String, body: String, bell: Bell) -> UNMutableNotificationContent {
        let content = UNMutableNotificationContent()
        content.title = title
        content.body = body
        content.sound = notificationSound(for: bell)
        return content
    }

    private static func notificationSound(for bell: Bell) -> UNNotificationSound {
        for name in bell.sound.resourceCandidates {
            guard Bundle.main.url(forResource: name, withExtension: "mp3") != nil else { continue }
            return UNNotificationSound(named: UNNotificationSoundName(rawValue: "\(name).mp3"))
        }
        return .default
    }

    private static func save(ids: [String], storageKey: String) {
        UserDefaults.standard.set(ids, forKey: storageKey)
    }

    private static func storedIDs(storageKey: String) -> [String] {
        UserDefaults.standard.stringArray(forKey: storageKey) ?? []
    }
}
