//
//  Day.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import Foundation

struct Day: Identifiable, Codable {
    let id: UUID
    var name: String
    var startTime: TimeInterval
    var segments: [Segment]
    var startBell: Bell
    var manualBell: Bell
    var defaultBell: Bell
    var theme: Theme

    var startTimeAsDate: Date {
        get {
            Calendar.current.startOfDay(for: Date.now).addingTimeInterval(self.startTime)
        }
        set {
            self.startTime = newValue.timeIntervalSince(Calendar.current.startOfDay(for: Date.now))
        }
    }

    var startEndTimeIntervals: (TimeInterval, TimeInterval) {
        var endTime: TimeInterval = startTime
        self.segments.forEach { segment in
            endTime += segment.duration
        }
        return (self.startTime, endTime)
    }

    var segmentStartEndTimes: [(Date, Date)] {
        var ret: [(Date, Date)] = []
        var timeCursor = Calendar.current.startOfDay(for: Date.now)
            .addingTimeInterval(startTime)
        self.segments.forEach { segment in
            ret.append((timeCursor, timeCursor.addingTimeInterval(segment.duration)))
            timeCursor.addTimeInterval(segment.duration)
        }
        return ret
    }

    init(id: UUID = UUID(), name: String, startTime: TimeInterval, segments: [Segment], startBell: Bell = Bell.singleBell, manualBell: Bell? = nil, defaultBell: Bell = Bell.singleBell, theme: Theme = Theme.bubblegum) {
        self.id = id
        self.name = name
        self.startTime = startTime
        self.segments = segments
        self.startBell = startBell
        self.defaultBell = defaultBell
        self.manualBell = manualBell ?? defaultBell
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
        startTime = try container.decode(TimeInterval.self, forKey: .startTime)
        segments = try container.decode([Segment].self, forKey: .segments)
        theme = try container.decode(Theme.self, forKey: .theme)

        let decodedDefault = try container.decodeIfPresent(Bell.self, forKey: .defaultBell)
        let decodedManual = try container.decodeIfPresent(Bell.self, forKey: .manualBell)
        let decodedStart = try container.decodeIfPresent(Bell.self, forKey: .startBell)

        defaultBell = decodedDefault ?? decodedManual ?? decodedStart ?? .singleBell
        startBell = decodedStart ?? defaultBell
        manualBell = decodedManual ?? defaultBell
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encode(id, forKey: .id)
        try container.encode(name, forKey: .name)
        try container.encode(startTime, forKey: .startTime)
        try container.encode(segments, forKey: .segments)
        try container.encode(startBell, forKey: .startBell)
        try container.encode(manualBell, forKey: .manualBell)
        try container.encode(defaultBell, forKey: .defaultBell)
        try container.encode(theme, forKey: .theme)
    }

    var currentSegmentAndTimeRemaining: (Segment, TimeInterval)? {
        var ret: (Segment, TimeInterval)? = nil
        zip(segments, segmentStartEndTimes).forEach({ (segment, times) in
            if Date.now >= times.0 && Date.now < times.1 {
                ret = (segment, times.1.timeIntervalSince(Date.now))
            }
        })
        return ret
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
            duration = try container.decode(TimeInterval.self, forKey: .duration)
            endBellStorage = try container.decodeIfPresent(Bell.self, forKey: .endBell)
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
