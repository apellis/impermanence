//
//  Day.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import Foundation

struct Day: Identifiable, Codable {
    private static let secondsPerDay = 24 * 60 * 60

    let id: UUID
    var name: String
    var startTime: TimeInterval
    var segments: [Segment]
    var startBell: Bell
    var manualBell: Bell
    var defaultBell: Bell
    var theme: Theme

    /// Today's start, as a wall-clock time in the current time zone.
    /// Setting it keeps only the hour, minute and second.
    var startTimeAsDate: Date {
        get {
            let calendar = Calendar.current
            return DaySchedule.instant(ofSecondsAfterMidnight: startTime,
                                       on: CalendarDay(containing: Date.now, calendar: calendar),
                                       calendar: calendar)
        }
        set {
            let c = Calendar.current.dateComponents([.hour, .minute, .second], from: newValue)
            let hours: Int = c.hour ?? 0
            let minutes: Int = c.minute ?? 0
            let seconds: Int = c.second ?? 0
            let total: Int = hours * 3600 + minutes * 60 + seconds
            self.startTime = Self.normalizedStartTime(TimeInterval(total))
        }
    }

    /// Total length of all segments, in seconds.
    var totalDuration: TimeInterval {
        segments.reduce(0) { $0 + $1.duration }
    }

    /// Start and end of each segment for today's occurrence (DST-correct).
    var segmentStartEndTimes: [(Date, Date)] {
        let calendar = Calendar.current
        let occurrence = DaySchedule.occurrence(of: self, on: CalendarDay(containing: Date.now, calendar: calendar), calendar: calendar)
        return occurrence.segments.map { ($0.start, $0.end) }
    }

    init(id: UUID = UUID(), name: String, startTime: TimeInterval, segments: [Segment], startBell: Bell = Bell.singleBell, manualBell: Bell? = nil, defaultBell: Bell = Bell.singleBell, theme: Theme = Theme.bubblegum) {
        self.id = id
        self.name = name
        self.startTime = Self.normalizedStartTime(startTime)
        self.segments = segments
        self.startBell = startBell.sanitized()
        self.defaultBell = defaultBell.sanitized()
        self.manualBell = (manualBell ?? defaultBell).sanitized()
        self.theme = theme
    }

    enum CodingKeys: String, CodingKey {
        case id
        case name
        case startTime
        case segments
        case startBell
        case manualBell
        case defaultBell
        case theme
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        id = try container.decode(UUID.self, forKey: .id)
        name = try container.decode(String.self, forKey: .name)
        startTime = Self.normalizedStartTime(try container.decode(TimeInterval.self, forKey: .startTime))
        segments = try container.decode([Segment].self, forKey: .segments)
        theme = try container.decode(Theme.self, forKey: .theme)

        let decodedDefault = try container.decodeIfPresent(Bell.self, forKey: .defaultBell)?.sanitized()
        let decodedManual = try container.decodeIfPresent(Bell.self, forKey: .manualBell)?.sanitized()
        let decodedStart = try container.decodeIfPresent(Bell.self, forKey: .startBell)?.sanitized()

        defaultBell = decodedDefault ?? decodedManual ?? decodedStart ?? .singleBell
        startBell = decodedStart ?? defaultBell
        manualBell = decodedManual ?? defaultBell
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(Self.normalizedStartTime(startTime), forKey: .startTime)
        try container.encode(segments, forKey: .segments)
        try container.encode(startBell.sanitized(), forKey: .startBell)
        try container.encode(manualBell.sanitized(), forKey: .manualBell)
        try container.encode(defaultBell.sanitized(), forKey: .defaultBell)
        try container.encode(theme, forKey: .theme)
    }

    private static func normalizedStartTime(_ value: TimeInterval) -> TimeInterval {
        guard value.isFinite else { return 0 }
        let seconds = Int(value.rounded())
        let mod = seconds % secondsPerDay
        let wrapped = mod < 0 ? mod + secondsPerDay : mod
        return TimeInterval(wrapped)
    }

    static var emptyDay: Day {
        Day(name: "", startTime: TimeInterval(), segments: [], startBell: Bell.singleBell, manualBell: nil, defaultBell: Bell.singleBell, theme: Theme.teal)
    }
}

extension Day {
    struct Segment: Identifiable, Codable {
        var id: UUID
        var name: String
        var duration: TimeInterval

        private var endBellStorage: Bell?

        init(id: UUID = UUID(), name: String, duration: TimeInterval, customEndBell: Bell? = nil) {
            self.id = id
            self.name = name
            self.duration = duration
            self.endBellStorage = customEndBell
        }

        var customEndBell: Bell? {
            get { endBellStorage }
            set { endBellStorage = newValue }
        }

        func resolvedEndBell(defaultBell: Bell) -> Bell {
            endBellStorage ?? defaultBell
        }

        mutating func useDefaultBell() {
            endBellStorage = nil
        }

        mutating func setCustomEndBell(_ bell: Bell) {
            endBellStorage = bell
        }

        enum CodingKeys: String, CodingKey {
            case id
            case name
            case duration
            case endBell
        }

        init(from decoder: Decoder) throws {
            let container = try decoder.container(keyedBy: CodingKeys.self)
            id = try container.decode(UUID.self, forKey: .id)
            name = try container.decode(String.self, forKey: .name)
            let decodedDuration = try container.decode(TimeInterval.self, forKey: .duration)
            duration = max(1, decodedDuration)
            endBellStorage = try container.decodeIfPresent(Bell.self, forKey: .endBell)?.sanitized()
        }

        func encode(to encoder: Encoder) throws {
            var container = encoder.container(keyedBy: CodingKeys.self)
            try container.encode(id, forKey: .id)
            try container.encode(name, forKey: .name)
            try container.encode(duration, forKey: .duration)
            try container.encode(endBellStorage, forKey: .endBell)
        }
    }
}

extension Day {
    static let fullDay: Day = Day(
        name: "Full Day",
        startTime: TimeInterval(7 * 60 * 60),
        segments: [
            Segment(name: "Wake", duration: TimeInterval(15 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Eat", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(60 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Eat", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Exercise", duration: TimeInterval(45 * 60)),
            Segment(name: "Sit", duration: TimeInterval(60 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Eat", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Dharma Talk", duration: TimeInterval(90 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
        ],
        startBell: .singleBell,
        manualBell: nil,
        defaultBell: .singleBell,
        theme: .poppy
    )
    static let openingDay: Day = Day(
        name: "Opening Day",
        startTime: TimeInterval(18 * 60 * 60),
        segments: [
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Dharma Talk", duration: TimeInterval(90 * 60)),
            Segment(name: "Walk", duration: TimeInterval(30 * 60)),
            Segment(name: "Sit", duration: TimeInterval(45 * 60)),
        ],
        startBell: .singleBell,
        manualBell: nil,
        defaultBell: .singleBell,
        theme: .orange
    )
}

/// Segment durations are whole seconds, from 1 s to 24 h (docs/BEHAVIOR.md 1.1).
/// The editor shows them as minutes and seconds.
enum SegmentDuration {
    static let minSeconds = 1
    static let maxSeconds = 24 * 60 * 60

    static func clamped(_ seconds: Int) -> Int {
        min(max(seconds, minSeconds), maxSeconds)
    }

    /// Whole seconds of `duration`, clamped.
    static func seconds(of duration: TimeInterval) -> Int {
        guard duration.isFinite else { return minSeconds }
        return clamped(Int(duration.rounded()))
    }

    /// Minutes and seconds (0...59) of a duration.
    static func parts(of duration: TimeInterval) -> (minutes: Int, seconds: Int) {
        let total = seconds(of: duration)
        return (total / 60, total % 60)
    }

    static func interval(minutes: Int, seconds: Int) -> TimeInterval {
        TimeInterval(clamped(minutes * 60 + seconds))
    }

    /// "45m", "1h 30m", "2m 30s", "1h 0m 5s".
    static func shortText(_ duration: TimeInterval) -> String {
        let total = seconds(of: duration)
        let h = total / 3600, m = (total % 3600) / 60, s = total % 60
        var parts: [String] = []
        if h > 0 { parts.append("\(h)h") }
        if m > 0 || (h > 0 && s > 0) { parts.append("\(m)m") }
        if s > 0 { parts.append("\(s)s") }
        return parts.isEmpty ? "0m" : parts.joined(separator: " ")
    }
}
