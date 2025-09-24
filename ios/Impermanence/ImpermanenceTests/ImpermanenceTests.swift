//
//  ImpermanenceTests.swift
//  ImpermanenceTests
//
//  Created by Alex Ellis on 8/5/23.
//

import XCTest
@testable import Impermanence

final class ImpermanenceTests: XCTestCase {

    @MainActor
    func testDayTimerRingsStartAndEndBells() {
        let startBell = Bell(soundId: 0, numRings: 2)
        let endBell = Bell(soundId: 0, numRings: 3)
        let segment = Day.Segment(name: "Meditate", duration: 60, endBell: endBell)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 1_000)

        let timer = DayTimer(
            startTime: 0,
            segments: [segment],
            startBell: startBell,
            loopDays: false,
            currentDate: referenceDate
        )

        var capturedBells: [Bell] = []
        timer.segmentChangedAction = { bell in
            if let bell {
                capturedBells.append(bell)
            }
        }

        timer.refresh(now: referenceDate.addingTimeInterval(-30))
        timer.refresh(now: referenceDate.addingTimeInterval(1))
        timer.refresh(now: referenceDate.addingTimeInterval(61))

        XCTAssertEqual(capturedBells, [startBell, endBell])
    }

    @MainActor
    func testDayTimerRespectsLoopSetting() throws {
        let segment = Day.Segment(name: "Meditate", duration: 60, endBell: .singleBell)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 2_000)

        let loopingTimer = DayTimer(
            startTime: 0,
            segments: [segment],
            startBell: .singleBell,
            loopDays: true,
            currentDate: referenceDate
        )
        let nonLoopingTimer = DayTimer(
            startTime: 0,
            segments: [segment],
            startBell: .singleBell,
            loopDays: false,
            currentDate: referenceDate
        )

        let originalLoopingStart = loopingTimer.startTime
        let originalNonLoopingStart = nonLoopingTimer.startTime

        let oneDayLater = referenceDate.addingTimeInterval(86_400 + 120)

        loopingTimer.refresh(now: oneDayLater)
        nonLoopingTimer.refresh(now: oneDayLater)

        let expectedLoopingStart = try XCTUnwrap(Calendar.current.date(byAdding: .day, value: 1, to: originalLoopingStart))
        XCTAssertEqual(loopingTimer.startTime, expectedLoopingStart)
        XCTAssertEqual(nonLoopingTimer.startTime, originalNonLoopingStart)
    }

    func testSegmentSchedulesMatchesSegments() {
        let day = Day.openingDay
        let schedules = DayDetailEditView.segmentSchedules(for: day)

        XCTAssertEqual(schedules.count, day.segments.count)
        for segment in day.segments {
            XCTAssertNotNil(schedules[segment.id])
        }
    }

    func testSegmentSchedulesEmptyDay() {
        var day = Day.emptyDay
        day.segments = []
        let schedules = DayDetailEditView.segmentSchedules(for: day)
        XCTAssertTrue(schedules.isEmpty)
    }

    func testDurationConversionRoundTrip() {
        XCTAssertEqual(DayDetailEditView.minutes(from: 5400), 90)
        XCTAssertEqual(DayDetailEditView.timeInterval(fromMinutes: 90), 5400)
    }

    func testDurationConversionMinimum() {
        XCTAssertEqual(DayDetailEditView.minutes(from: 0), 1)
        XCTAssertEqual(DayDetailEditView.timeInterval(fromMinutes: 0), 60)
    }

    func testClampedMinutesRespectsBounds() {
        XCTAssertEqual(DayDetailEditView.clampedMinutes(0), 1)
        XCTAssertEqual(DayDetailEditView.clampedMinutes(DayDetailEditView.maxDurationMinutes + 10), DayDetailEditView.maxDurationMinutes)
        XCTAssertEqual(DayDetailEditView.clampedMinutes(90), 90)
    }
}
