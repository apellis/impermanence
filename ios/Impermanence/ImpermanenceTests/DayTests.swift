//
//  DayTests.swift
//  ImpermanenceTests
//
//  Created by Taylor Barrella on 8/26/23.
//

import XCTest

final class DayTests: XCTestCase {
    func testStartTimeAsDateGet() throws {
        let day = newDay(now: "2023-08-26 13:00:00", startTime: 7 * 60 * 60)

        let startTimeDate = day.startTimeAsDate

        XCTAssertEqual(startTimeDate, dateFromString("2023-08-26 07:00:00"))
    }

    func testStartTimeAsDateSet() throws {
        var day = newDay(now: "2023-08-26 13:00:00")

        day.startTimeAsDate = dateFromString("2023-08-26 07:00:00")

        XCTAssertEqual(day.startTime, 7 * 60 * 60)
    }

    func testStartEndTimeIntervals() throws {
        let day = newDay(now: "2023-08-26 13:00:00", startTime: 7 * 60 * 60, segments: [
            newSegment(30 * 60),
            newSegment(30 * 60)
        ])

        let (startTime, endTime) = day.startEndTimeIntervals

        XCTAssertEqual(startTime, 7 * 60 * 60)
        XCTAssertEqual(endTime, 8 * 60 * 60)
    }

    func testSegmentStartEndTimes() throws {
        let day = newDay(now: "2023-08-26 13:00:00", startTime: 7 * 60 * 60, segments: [
            newSegment(30 * 60),
            newSegment(30 * 60)
        ])

        let times = day.segmentStartEndTimes

        XCTAssertEqual(times.count, 2)
        let (start1, end1) = times[0]
        XCTAssertEqual(start1, dateFromString("2023-08-26 07:00:00"))
        XCTAssertEqual(end1, dateFromString("2023-08-26 07:30:00"))
        let (start2, end2) = times[1]
        XCTAssertEqual(start2, dateFromString("2023-08-26 07:30:00"))
        XCTAssertEqual(end2, dateFromString("2023-08-26 08:00:00"))
    }
}

func newDay(
    now: String,
    startTime: TimeInterval = 0,
    segments: [Day.Segment] = []
) -> Day {
    return Day(
        name: "day",
        startTime: startTime,
        segments: segments,
        nowDate: dateFromString(now)
    )
}

func newSegment(_ duration: TimeInterval) -> Day.Segment {
    Day.Segment(name: "segment", duration: duration)
}

func dateFromString(_ str: String) -> Date {
    let formatter = DateFormatter()
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.locale = Locale(identifier: "en_US")
    formatter.timeZone = TimeZone(identifier: "America/Los_Angeles")
    formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
    return formatter.date(from: str)!
}
