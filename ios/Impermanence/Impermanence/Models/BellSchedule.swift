//
//  BellSchedule.swift
//  Impermanence
//
//  Pure, Foundation-only bell timing logic. This file is the reference
//  implementation of docs/BEHAVIOR.md and is unit-tested on Linux
//  (ios/Package.swift) as well as in the Xcode test target.
//

import Foundation

/// A calendar date (year, month, day) with no time or zone attached.
struct CalendarDay: Codable, Hashable, Comparable {
    var year: Int
    var month: Int
    var day: Int

    init(year: Int, month: Int, day: Int) {
        self.year = year
        self.month = month
        self.day = day
    }

    /// The calendar date that `date` falls on in `calendar`'s time zone.
    init(containing date: Date, calendar: Calendar) {
        let c = calendar.dateComponents([.year, .month, .day], from: date)
        self.init(year: c.year ?? 1970, month: c.month ?? 1, day: c.day ?? 1)
    }

    /// Stable text form, used in chime identifiers: "2026-03-08".
    var key: String {
        String(format: "%04d-%02d-%02d", year, month, day)
    }

    /// Noon on this date. Noon always exists, even on days whose midnight is
    /// skipped by a DST change, so it is a safe base for date arithmetic.
    func noon(in calendar: Calendar) -> Date {
        var c = DateComponents()
        c.year = year
        c.month = month
        c.day = day
        c.hour = 12
        return calendar.date(from: c) ?? Date(timeIntervalSince1970: 0)
    }

    func adding(days: Int, calendar: Calendar) -> CalendarDay {
        let base = noon(in: calendar)
        let shifted = calendar.date(byAdding: .day, value: days, to: base) ?? base
        return CalendarDay(containing: shifted, calendar: calendar)
    }

    static func < (lhs: CalendarDay, rhs: CalendarDay) -> Bool {
        (lhs.year, lhs.month, lhs.day) < (rhs.year, rhs.month, rhs.day)
    }
}

/// One run of a Day on a particular calendar date.
struct DayOccurrence: Equatable {
    struct Interval: Equatable {
        let start: Date
        let end: Date
    }

    let date: CalendarDay
    let start: Date
    let segments: [Interval]

    var end: Date { segments.last?.end ?? start }
}

/// A scheduled bell: the day's start bell, or the end bell of one segment.
struct BellEvent: Equatable {
    enum Kind: String, Codable {
        case start
        case end
    }

    /// Stable across recomputation (time zone changes, relaunches):
    /// "<occurrence date>.start" or "<occurrence date>.end.<segment index>",
    /// or "quicksit.start" / "quicksit.end".
    let id: String
    let kind: Kind
    /// 0 for the start bell; the index of the segment that ends otherwise.
    let segmentIndex: Int
    let instant: Date
    let bell: Bell
    /// Short human text, used as the notification body.
    let label: String
}

/// One strike of a bell. A bell with N rings is N chimes, 3 seconds apart.
struct Chime: Equatable, Hashable {
    let id: String
    let eventID: String
    let ringIndex: Int
    let instant: Date
    let soundId: Int
}

enum DaySchedule {
    static let chimeSpacing: TimeInterval = 3

    /// Seconds after local midnight, split into wall-clock components.
    static func wallClock(fromSecondsAfterMidnight seconds: TimeInterval) -> (hour: Int, minute: Int, second: Int) {
        let total = Int(seconds.isFinite ? seconds.rounded() : 0)
        let wrapped = ((total % 86_400) + 86_400) % 86_400
        return (wrapped / 3600, (wrapped % 3600) / 60, wrapped % 60)
    }

    /// The instant a wall-clock time occurs on `date`.
    /// A time inside a DST gap moves forward by the length of the gap
    /// (02:30 becomes 03:30); a repeated time uses its first occurrence.
    static func instant(ofSecondsAfterMidnight seconds: TimeInterval, on date: CalendarDay, calendar: Calendar) -> Date {
        let (h, m, s) = wallClock(fromSecondsAfterMidnight: seconds)
        let noon = date.noon(in: calendar)
        if let exact = calendar.date(bySettingHour: h, minute: m, second: s, of: noon,
                                     matchingPolicy: .strict, repeatedTimePolicy: .first, direction: .forward),
           CalendarDay(containing: exact, calendar: calendar) == date {
            return exact
        }
        // Not a valid wall-clock time (DST gap): interpret it with the offset
        // in effect just before the gap, which shifts it forward by the gap.
        let startOfDay = calendar.startOfDay(for: noon)
        var utcCalendar = Calendar(identifier: .gregorian)
        utcCalendar.timeZone = TimeZone(identifier: "UTC")!
        var c = DateComponents()
        c.year = date.year
        c.month = date.month
        c.day = date.day
        c.hour = h
        c.minute = m
        c.second = s
        guard let naiveUTC = utcCalendar.date(from: c) else { return startOfDay }
        // Offset in effect before the transition: the offset at noon of the
        // previous day (a gap can sit at midnight, so not this day's start).
        let previousNoon = noon.addingTimeInterval(-86_400)
        let offsetBefore = TimeInterval(calendar.timeZone.secondsFromGMT(for: previousNoon))
        return naiveUTC.addingTimeInterval(-offsetBefore)
    }

    static func occurrence(of day: Day, on date: CalendarDay, calendar: Calendar) -> DayOccurrence {
        let start = instant(ofSecondsAfterMidnight: day.startTime, on: date, calendar: calendar)
        var cursor = start
        var intervals: [DayOccurrence.Interval] = []
        intervals.reserveCapacity(day.segments.count)
        for segment in day.segments {
            let end = cursor.addingTimeInterval(segment.duration)
            intervals.append(.init(start: cursor, end: end))
            cursor = end
        }
        return DayOccurrence(date: date, start: start, segments: intervals)
    }

    /// The occurrence a session opened at `now` times: yesterday's if it is
    /// still running (a day that crosses midnight), otherwise today's.
    static func initialOccurrence(of day: Day, at now: Date, calendar: Calendar) -> DayOccurrence {
        let today = CalendarDay(containing: now, calendar: calendar)
        let yesterday = occurrence(of: day, on: today.adding(days: -1, calendar: calendar), calendar: calendar)
        if !day.segments.isEmpty && now < yesterday.end {
            return yesterday
        }
        return occurrence(of: day, on: today, calendar: calendar)
    }

    /// The occurrence being timed at `now` for a session anchored on `anchor`.
    /// Without looping it is always the anchor. With looping, the session
    /// moves to a later date only once the earlier occurrence has ended and
    /// the calendar date has moved past it.
    static func currentOccurrence(of day: Day, anchor: CalendarDay, at now: Date, loopDays: Bool, calendar: Calendar) -> DayOccurrence {
        let anchored = occurrence(of: day, on: anchor, calendar: calendar)
        guard loopDays else { return anchored }
        let candidate = initialOccurrence(of: day, at: now, calendar: calendar)
        return candidate.date > anchor ? candidate : anchored
    }

    static func events(for occurrence: DayOccurrence, of day: Day) -> [BellEvent] {
        guard !day.segments.isEmpty, occurrence.segments.count == day.segments.count else { return [] }
        let key = occurrence.date.key
        var events: [BellEvent] = [
            BellEvent(id: "\(key).start", kind: .start, segmentIndex: 0,
                      instant: occurrence.start, bell: day.startBell.sanitized(),
                      label: day.segments[0].name.isEmpty ? "Day started" : "\(day.segments[0].name) begins")
        ]
        for (index, segment) in day.segments.enumerated() {
            let nextName = index + 1 < day.segments.count ? day.segments[index + 1].name : nil
            let label: String
            if let nextName, !nextName.isEmpty {
                label = "\(nextName) begins"
            } else if nextName == nil {
                label = "Day complete"
            } else {
                label = segment.name.isEmpty ? "Segment ended" : "\(segment.name) ended"
            }
            events.append(BellEvent(id: "\(key).end.\(index)", kind: .end, segmentIndex: index,
                                    instant: occurrence.segments[index].end,
                                    bell: segment.resolvedEndBell(defaultBell: day.defaultBell).sanitized(),
                                    label: label))
        }
        return events
    }

    /// Every bell event a day session rings while it runs from `sessionStart`,
    /// restricted to instants in `window`. Nothing before `sessionStart` ever
    /// rings. With looping, the occurrence on date D+1 only rings events strictly
    /// after the end of D's occurrence.
    static func sessionEvents(for day: Day, anchor: CalendarDay, sessionStart: Date, loopDays: Bool,
                              window: ClosedRange<Date>, calendar: Calendar) -> [BellEvent] {
        guard !day.segments.isEmpty else { return [] }
        let lower = max(window.lowerBound, sessionStart)
        guard lower <= window.upperBound else { return [] }

        if !loopDays {
            return events(for: occurrence(of: day, on: anchor, calendar: calendar), of: day)
                .filter { $0.instant >= lower && $0.instant <= window.upperBound }
        }

        var result: [BellEvent] = []
        // Start a few days before the window rather than at the anchor, so a
        // session that has looped for weeks stays cheap. Three days of lead
        // is enough for the overlap rule unless a day lasts over 72 hours.
        let lead = CalendarDay(containing: lower, calendar: calendar).adding(days: -3, calendar: calendar)
        var date = max(anchor, lead)
        var previousEnd: Date? = nil
        // Walk occurrences from the anchor until they start after the window.
        // The bound keeps a pathological plan from looping forever.
        for _ in 0..<4000 {
            let occ = occurrence(of: day, on: date, calendar: calendar)
            if occ.start > window.upperBound && (previousEnd ?? occ.start) > window.upperBound {
                break
            }
            for event in events(for: occ, of: day) {
                if let previousEnd, event.instant <= previousEnd { continue }
                if event.instant >= lower && event.instant <= window.upperBound {
                    result.append(event)
                }
            }
            previousEnd = max(previousEnd ?? occ.end, occ.end)
            date = date.adding(days: 1, calendar: calendar)
        }
        return result.sorted { $0.instant < $1.instant }
    }

    static func chimes(for events: [BellEvent]) -> [Chime] {
        events.flatMap { event in
            (0..<event.bell.sanitized().numRings).map { ring in
                Chime(id: "\(event.id).\(ring)", eventID: event.id, ringIndex: ring,
                      instant: event.instant.addingTimeInterval(TimeInterval(ring) * chimeSpacing),
                      soundId: event.bell.sanitized().soundId)
            }
        }
    }
}

/// Where the timed day stands at an instant, for display.
struct DayProgress: Equatable {
    enum Status: Equatable {
        case empty
        case queued
        case running(segmentIndex: Int)
        case complete
    }

    let status: Status
    let occurrence: DayOccurrence?
    /// Elapsed / remaining time in the running segment; nil otherwise.
    let elapsed: TimeInterval?
    let remaining: TimeInterval?

    /// -2 empty, -1 queued, 0..<count running, count complete.
    var segmentIndex: Int {
        switch status {
        case .empty: return -2
        case .queued: return -1
        case .running(let i): return i
        case .complete: return occurrence?.segments.count ?? 0
        }
    }

    /// When the next bell rings, if any remain in this occurrence.
    var nextBell: Date? {
        guard let occurrence else { return nil }
        switch status {
        case .queued: return occurrence.start
        case .running(let i): return occurrence.segments[i].end
        default: return nil
        }
    }

    static func at(_ now: Date, occurrence: DayOccurrence?) -> DayProgress {
        guard let occurrence, !occurrence.segments.isEmpty else {
            return DayProgress(status: .empty, occurrence: occurrence, elapsed: nil, remaining: nil)
        }
        if now < occurrence.start {
            return DayProgress(status: .queued, occurrence: occurrence, elapsed: nil, remaining: nil)
        }
        if now >= occurrence.end {
            return DayProgress(status: .complete, occurrence: occurrence, elapsed: nil, remaining: nil)
        }
        let index = occurrence.segments.firstIndex { now >= $0.start && now < $0.end } ?? 0
        let interval = occurrence.segments[index]
        return DayProgress(status: .running(segmentIndex: index), occurrence: occurrence,
                           elapsed: now.timeIntervalSince(interval.start),
                           remaining: interval.end.timeIntervalSince(now))
    }
}

/// A timing session the app is running: one active day, or one Quick Sit.
/// Persisted while it runs so that a relaunch after the system ends the
/// process resumes it. Leaving the screen ends it and clears it.
enum TimingSession: Codable, Equatable {
    case day(DaySession)
    case quickSit(QuickSitSession)

    struct DaySession: Codable, Equatable {
        var dayID: UUID
        var openedAt: Date
        var anchor: CalendarDay
        var loopDays: Bool
    }

    struct QuickSitSession: Codable, Equatable {
        var startedAt: Date
        var durationSeconds: Int
        var soundId: Int
        var startChimes: Int
        var endChimes: Int

        var endsAt: Date { startedAt.addingTimeInterval(TimeInterval(durationSeconds)) }
    }

    var openedAt: Date {
        switch self {
        case .day(let s): return s.openedAt
        case .quickSit(let s): return s.startedAt
        }
    }

    /// Bell events of this session with instants in `window`.
    /// `day` is required for a day session; nil yields no events.
    func events(day: Day?, window: ClosedRange<Date>, calendar: Calendar) -> [BellEvent] {
        switch self {
        case .day(let s):
            guard let day else { return [] }
            return DaySchedule.sessionEvents(for: day, anchor: s.anchor, sessionStart: s.openedAt,
                                             loopDays: s.loopDays, window: window, calendar: calendar)
        case .quickSit(let s):
            let all = [
                BellEvent(id: "quicksit.start", kind: .start, segmentIndex: 0, instant: s.startedAt,
                          bell: Bell(soundId: s.soundId, numRings: s.startChimes).sanitized(),
                          label: "Quick Sit started"),
                BellEvent(id: "quicksit.end", kind: .end, segmentIndex: 0, instant: s.endsAt,
                          bell: Bell(soundId: s.soundId, numRings: s.endChimes).sanitized(),
                          label: "Quick Sit complete"),
            ]
            return all.filter { window.contains($0.instant) && $0.instant >= s.startedAt }
        }
    }

    /// True when no bell can ring after `now`: the session's machinery can
    /// stop. A looping day never finishes.
    func isFinished(at now: Date, day: Day?, calendar: Calendar) -> Bool {
        switch self {
        case .day(let s):
            guard let day, !day.segments.isEmpty else { return true }
            if s.loopDays { return false }
            let occ = DaySchedule.occurrence(of: day, on: s.anchor, calendar: calendar)
            let lastChime = occ.end.addingTimeInterval(
                TimeInterval(max(0, (day.segments.last?.resolvedEndBell(defaultBell: day.defaultBell).sanitized().numRings ?? 1) - 1)) * DaySchedule.chimeSpacing)
            return now > lastChime
        case .quickSit(let s):
            let lastChime = s.endsAt.addingTimeInterval(TimeInterval(max(0, Bell(soundId: s.soundId, numRings: s.endChimes).sanitized().numRings - 1)) * DaySchedule.chimeSpacing)
            return now > lastChime
        }
    }
}

/// Decides, on each tick, which chimes to play now and which to leave to the
/// notification safety net. Pure: the engine executes its decisions.
enum ChimePlanner {
    /// How far ahead a chime is handed to the audio player, which then starts
    /// it on the exact instant.
    static let audioLookahead: TimeInterval = 1.0
    /// A chime this late is treated as on time (timer jitter).
    static let onTimeTolerance: TimeInterval = 0.5
    /// The most recent missed event may still ring if it is at most this late
    /// (the process was suspended or restarted). Older misses never ring.
    static let catchUpLimit: TimeInterval = 60
    /// Backup notifications cover this far ahead, capped by count, and are
    /// extended on every tick. Kept short so that if the user swipes away an
    /// app iOS had already suspended (no termination callback), stray
    /// backups stop within this time.
    static let notificationHorizon: TimeInterval = 2 * 60 * 60
    static let maxPendingNotifications = 60
    /// One backup notification plays up to this many chimes of one event
    /// (a sound file of at most 30 s).
    static let chimesPerNotification = 6

    struct Playback: Equatable {
        let chime: Chime
        /// The instant to start it: its own instant, or later for a catch-up.
        let playAt: Date
    }

    struct Decision: Equatable {
        var play: [Playback] = []
        /// Chimes that are now handled: played, or skipped as too late.
        var handled: Set<String> = []
    }

    /// - Parameters:
    ///   - chimes: candidate chimes of the session (any order).
    ///   - handled: chime ids already played or skipped.
    ///   - sessionStart: nothing before this instant ever rings.
    static func decide(chimes: [Chime], handled: Set<String>, sessionStart: Date, now: Date) -> Decision {
        var decision = Decision()
        let pending = chimes.filter { !handled.contains($0.id) && $0.instant >= sessionStart }
        let due = pending.filter { $0.instant <= now.addingTimeInterval(audioLookahead) }
        guard !due.isEmpty else { return decision }

        // Group by event; an event is late when its first pending chime is
        // more than the tolerance in the past.
        let byEvent = Dictionary(grouping: due, by: \.eventID)
        var lateEvents: [(id: String, first: Date)] = []
        for (eventID, eventChimes) in byEvent {
            let sorted = eventChimes.sorted { $0.instant < $1.instant }
            guard let first = sorted.first else { continue }
            if first.instant >= now.addingTimeInterval(-onTimeTolerance) {
                for chime in sorted {
                    decision.play.append(Playback(chime: chime, playAt: max(chime.instant, now)))
                    decision.handled.insert(chime.id)
                }
            } else {
                lateEvents.append((eventID, first.instant))
            }
        }

        // Only the most recent late event may ring, and only if recent enough.
        // A late event's remaining chimes keep their 3-second spacing from now.
        // All of a late event's pending chimes are decided together, including
        // ones not yet due, so none of them also rings on its own schedule.
        let mostRecent = lateEvents.max { $0.first < $1.first }
        for late in lateEvents {
            let eventChimes = pending.filter { $0.eventID == late.id }.sorted { $0.instant < $1.instant }
            let ringIt = late.id == mostRecent?.id && now.timeIntervalSince(late.first) <= catchUpLimit
            for (offset, chime) in eventChimes.enumerated() {
                if ringIt {
                    decision.play.append(Playback(chime: chime,
                                                  playAt: now.addingTimeInterval(TimeInterval(offset) * DaySchedule.chimeSpacing)))
                }
                decision.handled.insert(chime.id)
            }
        }
        decision.play.sort { $0.playAt < $1.playAt }
        return decision
    }

    /// One backup notification: up to `chimesPerNotification` consecutive
    /// chimes of one event, played by one sound file.
    struct NotificationGroup: Equatable {
        /// "<event id>.g<k>": chimes k*6 ..< k*6+6 of the event.
        let id: String
        let eventID: String
        let instant: Date
        let chimeIDs: [String]
        let soundId: Int
    }

    static func groupID(of chime: Chime) -> String {
        "\(chime.eventID).g\(chime.ringIndex / chimesPerNotification)"
    }

    /// The chime ids a delivered group notification already rang.
    static func chimeIDs(ofGroup groupID: String) -> [String] {
        guard let range = groupID.range(of: ".g", options: .backwards),
              let k = Int(groupID[range.upperBound...]) else { return [] }
        let eventID = String(groupID[..<range.lowerBound])
        return (k * chimesPerNotification ..< (k + 1) * chimesPerNotification).map { "\(eventID).\($0)" }
    }

    /// The backup notifications to hold right now: one per group whose first
    /// chime is unhandled, still in the future and within the horizon.
    static func notificationGroups(chimes: [Chime], handled: Set<String>, sessionStart: Date, now: Date) -> [NotificationGroup] {
        let horizon = now.addingTimeInterval(notificationHorizon)
        let byGroup = Dictionary(grouping: chimes, by: groupID(of:))
        var groups: [NotificationGroup] = []
        for (id, members) in byGroup {
            let sorted = members.sorted { $0.ringIndex < $1.ringIndex }
            guard let first = sorted.first,
                  !handled.contains(first.id), first.instant >= sessionStart,
                  first.instant > now, first.instant <= horizon else { continue }
            let open = sorted.filter { !handled.contains($0.id) }
            groups.append(NotificationGroup(id: id, eventID: first.eventID, instant: first.instant,
                                            chimeIDs: open.map(\.id), soundId: first.soundId))
        }
        return Array(groups.sorted { $0.instant < $1.instant }.prefix(maxPendingNotifications))
    }
}
