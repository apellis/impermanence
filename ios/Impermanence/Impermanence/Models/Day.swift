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
    var theme: Theme
    private let nowDate: Date

    var startTimeAsDate: Date {
        get {
            Calendar.current.startOfDay(for: nowDate).addingTimeInterval(self.startTime)
        }
        set {
            self.startTime = newValue.timeIntervalSince(Calendar.current.startOfDay(for: nowDate))
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
        var timeCursor = Calendar.current.startOfDay(for: nowDate)
            .addingTimeInterval(startTime)
        self.segments.forEach { segment in
            ret.append((timeCursor, timeCursor.addingTimeInterval(segment.duration)))
            timeCursor.addTimeInterval(segment.duration)
        }
        return ret
    }

    init(id: UUID = UUID(), name: String, startTime: TimeInterval, segments: [Segment], startBell: Bell = Bell.singleBell, manualBell: Bell = Bell.singleBell, theme: Theme = Theme.bubblegum, nowDate: Date = Date.now) {
        self.id = id
        self.name = name
        self.startTime = startTime
        self.segments = segments
        self.startBell = startBell
        self.manualBell = manualBell
        self.theme = theme
        self.nowDate = nowDate
    }

    var currentSegmentAndTimeRemaining: (Segment, TimeInterval)? {
        var ret: (Segment, TimeInterval)? = nil
        zip(segments, segmentStartEndTimes).forEach({ (segment, times) in
            if nowDate >= times.0 && nowDate < times.1 {
                ret = (segment, times.1.timeIntervalSince(nowDate))
            }
        })
        return ret
    }

    static var emptyDay: Day {
        Day(name: "", startTime: TimeInterval(), segments: [], startBell: Bell.singleBell, manualBell: Bell.singleBell, theme: Theme.teal)
    }
}

extension Day {
    struct Segment: Identifiable, Codable {
        var id: UUID
        var name: String
        var duration: TimeInterval
        var endBell: Bell

        init(id: UUID = UUID(), name: String, duration: TimeInterval, endBell: Bell = Bell.singleBell) {
            self.id = id
            self.name = name
            self.duration = duration
            self.endBell = endBell
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
        theme: .orange
    )
}
