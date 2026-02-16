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
        let segment = Day.Segment(name: "Meditate", duration: 60, customEndBell: endBell)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 1_000)
        let startOfDay = Calendar.current.startOfDay(for: referenceDate)
        let startOffset = referenceDate.timeIntervalSince(startOfDay) + 30

        let timer = DayTimer(
            startTime: startOffset,
            segments: [segment],
            startBell: startBell,
            defaultBell: .singleBell,
            loopDays: false,
            currentDate: referenceDate
        )

        var capturedBells: [Bell] = []
        timer.segmentChangedAction = { bell in
            if let bell {
                capturedBells.append(bell)
            }
        }

        timer.refresh(now: referenceDate)
        timer.refresh(now: referenceDate.addingTimeInterval(31))
        timer.refresh(now: referenceDate.addingTimeInterval(91))

        XCTAssertEqual(capturedBells, [startBell, endBell])
    }

    @MainActor
    func testDayTimerRespectsLoopSetting() throws {
        let segment = Day.Segment(name: "Meditate", duration: 60)
        let referenceDate = Date(timeIntervalSinceReferenceDate: 2_000)

        let loopingTimer = DayTimer(
            startTime: 0,
            segments: [segment],
            startBell: .singleBell,
            defaultBell: .singleBell,
            loopDays: true,
            currentDate: referenceDate
        )
        let nonLoopingTimer = DayTimer(
            startTime: 0,
            segments: [segment],
            startBell: .singleBell,
            defaultBell: .singleBell,
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

    func testPortableCodecRoundTrip() throws {
        let sourceDays = [Day.openingDay, Day.fullDay]
        let data = try PortableDayPlanCodec.exportData(days: sourceDays)
        let imported = try PortableDayPlanCodec.importDays(from: data)

        XCTAssertEqual(imported.count, sourceDays.count)
        XCTAssertEqual(imported[0].name, sourceDays[0].name)
        XCTAssertEqual(imported[0].segments.count, sourceDays[0].segments.count)
        XCTAssertEqual(imported[1].name, sourceDays[1].name)
        XCTAssertEqual(imported[1].segments.count, sourceDays[1].segments.count)
    }

    func testPortableCodecDecodesAndroidStyleEnvelopeAndNormalizesValues() throws {
        let json = """
        {
          "version": 1,
          "days": [
            {
              "id": "B86D6CF6-49A8-4A2D-9192-9E7465A78A31",
              "name": "Imported Day",
              "startTimeSeconds": -120,
              "segments": [
                {
                  "id": "50A08A8D-25B8-4D84-B2E3-9D84F942A2F7",
                  "name": "Sit",
                  "durationSeconds": 0,
                  "customEndBell": { "soundId": 0, "numRings": 0 }
                }
              ],
              "startBell": { "soundId": 0, "numRings": 0 },
              "manualBell": { "soundId": 0, "numRings": 0 },
              "defaultBell": { "soundId": 0, "numRings": 0 },
              "theme": "teal"
            }
          ]
        }
        """.data(using: .utf8)!

        let imported = try PortableDayPlanCodec.importDays(from: json)
        let day = try XCTUnwrap(imported.first)

        XCTAssertEqual(imported.count, 1)
        XCTAssertEqual(day.name, "Imported Day")
        XCTAssertEqual(Int(day.startTime), 24 * 60 * 60 - 120)
        XCTAssertEqual(day.segments.count, 1)
        XCTAssertEqual(Int(day.segments[0].duration), 1)
        XCTAssertEqual(day.startBell.numRings, 1)
        XCTAssertEqual(day.manualBell.numRings, 1)
        XCTAssertEqual(day.defaultBell.numRings, 1)
        XCTAssertEqual(day.segments[0].customEndBell?.numRings, 1)
    }
}
