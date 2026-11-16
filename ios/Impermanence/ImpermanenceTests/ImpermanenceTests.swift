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
    func testDayTimerTracksSegmentsFromWallClock() {
        let segment = Day.Segment(name: "Meditate", duration: 60)
        let day = Day(name: "T", startTime: 0, segments: [segment])
        let calendar = Calendar.autoupdatingCurrent
        let reference = Date(timeIntervalSince1970: 1_781_000_000)
        let anchor = CalendarDay(containing: reference, calendar: calendar)
        let start = DaySchedule.occurrence(of: day, on: anchor, calendar: calendar).start

        let timer = DayTimer(day: day, loopDays: false, anchor: anchor, now: start.addingTimeInterval(-5))
        XCTAssertEqual(timer.segmentIndex, -1)
        timer.refresh(now: start.addingTimeInterval(10))
        XCTAssertEqual(timer.segmentIndex, 0)
        XCTAssertEqual(timer.segmentText, "Now: Meditate")
        XCTAssertEqual(timer.progress.remaining, 50)
        timer.refresh(now: start.addingTimeInterval(61))
        XCTAssertEqual(timer.segmentIndex, 1)
        XCTAssertEqual(timer.segmentText, "(Day is over)")
    }

    @MainActor
    func testDayTimerLoopsToTheNextDayOnlyWhenLooping() throws {
        let day = Day(name: "T", startTime: 3600, segments: [Day.Segment(name: "Sit", duration: 60)])
        let calendar = Calendar.autoupdatingCurrent
        let reference = Date(timeIntervalSince1970: 1_781_000_000)
        let anchor = CalendarDay(containing: reference, calendar: calendar)
        let nextDay = try XCTUnwrap(calendar.date(byAdding: .day, value: 1, to: anchor.noon(in: calendar)))

        let looping = DayTimer(day: day, loopDays: true, anchor: anchor, now: reference)
        looping.refresh(now: nextDay)
        XCTAssertEqual(looping.progress.occurrence?.date, CalendarDay(containing: nextDay, calendar: calendar))

        let single = DayTimer(day: day, loopDays: false, anchor: anchor, now: reference)
        single.refresh(now: nextDay)
        XCTAssertEqual(single.progress.occurrence?.date, anchor)
        XCTAssertEqual(single.segmentText, "(Day is over)")
    }

    @MainActor
    func testDayTimerEmptyDay() {
        let timer = DayTimer(day: Day(name: "E", startTime: 0, segments: []), loopDays: true)
        XCTAssertEqual(timer.segmentIndex, -2)
        XCTAssertEqual(timer.segmentText, "(Day is empty)")
    }

    func testBundledSoundsExist() {
        for sound in BellCatalog.sounds {
            XCTAssertNotNil(sound.url, "missing \(sound.fileName)")
        }
        XCTAssertNotNil(Bundle.main.url(forResource: "silence", withExtension: "caf"))
    }

    func testBackgroundAudioModeIsDeclared() {
        let modes = Bundle.main.object(forInfoDictionaryKey: "UIBackgroundModes") as? [String]
        XCTAssertEqual(modes, ["audio"])
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

    func testSegmentDurationsAreWholeSecondsFromOneSecondTo24Hours() {
        XCTAssertEqual(SegmentDuration.interval(minutes: 90, seconds: 0), 5400)
        XCTAssertEqual(SegmentDuration.interval(minutes: 2, seconds: 30), 150)
        XCTAssertEqual(SegmentDuration.interval(minutes: 0, seconds: 0), 1)
        XCTAssertEqual(SegmentDuration.interval(minutes: 2000, seconds: 0), 86400)
        XCTAssertEqual(SegmentDuration.parts(of: 150).minutes, 2)
        XCTAssertEqual(SegmentDuration.parts(of: 150).seconds, 30)
        XCTAssertEqual(SegmentDuration.shortText(45 * 60), "45m")
        XCTAssertEqual(SegmentDuration.shortText(150), "2m 30s")
        XCTAssertEqual(SegmentDuration.shortText(3605), "1h 0m 5s")
        XCTAssertEqual(SegmentDuration.shortText(5400), "1h 30m")
    }

    func testTimesShowSecondsOnlyWhenNeeded() {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = .current
        let whole = calendar.date(from: DateComponents(year: 2026, month: 3, day: 1, hour: 7, minute: 15))!
        XCTAssertEqual(TimeFormatting.formattedTime(from: whole, use24HourClock: false), "7:15 AM")
        XCTAssertEqual(TimeFormatting.formattedTime(from: whole.addingTimeInterval(30), use24HourClock: false), "7:15:30 AM")
        XCTAssertEqual(TimeFormatting.formattedTime(from: whole.addingTimeInterval(30), use24HourClock: true), "07:15:30")
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

    func testNativeDecodeSanitizesBellValuesAndStartTime() throws {
        let json = """
        [
          {
            "id": "6A9805A9-C40B-440C-8A62-C843C8A8D603",
            "name": "Corrupt Day",
            "startTime": 987654321,
            "segments": [
              {
                "id": "B845B10B-5BA4-4A5A-B4C8-8FEC4E2512A6",
                "name": "Sit",
                "duration": 0,
                "endBell": { "soundId": 99, "numRings": 9999 }
              }
            ],
            "startBell": { "soundId": 99, "numRings": 9999 },
            "manualBell": { "soundId": 99, "numRings": -4 },
            "defaultBell": { "soundId": 99, "numRings": 99 },
            "theme": "orange"
          }
        ]
        """.data(using: .utf8)!

        let decoded = try JSONDecoder().decode([Day].self, from: json)
        let day = try XCTUnwrap(decoded.first)

        XCTAssertGreaterThanOrEqual(day.startTime, 0)
        XCTAssertLessThan(day.startTime, 86_400)
        XCTAssertEqual(day.startBell.soundId, Bell.singleBell.soundId)
        XCTAssertEqual(day.defaultBell.soundId, Bell.singleBell.soundId)
        XCTAssertEqual(day.manualBell.soundId, Bell.singleBell.soundId)
        XCTAssertEqual(day.startBell.numRings, Bell.maxRings)
        XCTAssertEqual(day.defaultBell.numRings, Bell.maxRings)
        XCTAssertEqual(day.manualBell.numRings, Bell.minRings)
        XCTAssertEqual(day.segments.first?.duration, 1)
        XCTAssertEqual(day.segments.first?.customEndBell?.numRings, Bell.maxRings)
    }

    func testBellSanitizationClampsUnsafeValues() {
        let unsafeBell = Bell(soundId: 77, numRings: 50_000).sanitized()
        XCTAssertEqual(unsafeBell.soundId, Bell.singleBell.soundId)
        XCTAssertEqual(unsafeBell.numRings, Bell.maxRings)
    }
}
