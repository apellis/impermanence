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

// MARK: - BellEngine with a fake clock, audio and notifications

@MainActor
final class FakeBellAudio: BellAudioOutput {
    var plays: [(soundId: Int, at: Date)] = []
    var activateSucceeds = true
    var keepAliveRunning = false
    var outputVolume: Float = 1
    var isUsingHeadphones = false
    func activate() -> Bool { activateSucceeds }
    func startKeepAlive() { keepAliveRunning = true }
    func resumeKeepAlive() {}
    func stopKeepAlive() { keepAliveRunning = false }
    func play(soundId: Int, at instant: Date, now: Date) { plays.append((soundId, instant)) }
    func cancelNotStarted(now: Date) { plays.removeAll { $0.at > now } }
    func reset() {}
}

@MainActor
final class FakeBellNotifications: BellNotificationScheduler {
    var pending: [String: BellNotificationRequest] = [:]
    var delivered: [String] = []
    var status = BellNotificationStatus()
    func add(_ request: BellNotificationRequest) { pending[request.id] = request }
    func removePending(ids: [String]) { ids.forEach { pending[$0] = nil } }
    func removeAllPending() { pending = [:] }
    func removeAllDelivered() { delivered = [] }
    func deliveredIDs() async -> [String] { delivered }
    func status() async -> BellNotificationStatus { status }
    func requestAuthorization() async -> Bool { true }
    func pendingCount() async -> Int { pending.count }
}

@MainActor
final class BellEngineTests: XCTestCase {
    private final class Clock { var now = Date(timeIntervalSince1970: 1_781_100_000) }
    private var clock = Clock()
    private var audio = FakeBellAudio()
    private var notifications = FakeBellNotifications()
    private var engine: BellEngine!

    override func setUp() async throws {
        clock = Clock()
        audio = FakeBellAudio()
        notifications = FakeBellNotifications()
        let defaults = UserDefaults(suiteName: "BellEngineTests-\(UUID())")!
        let clock = self.clock
        engine = BellEngine(audio: audio, notifications: notifications, defaults: defaults,
                            clock: { clock.now }, observeSystem: false)
    }

    private var endID: String { BellEngine.notificationPrefix + "quicksit.end.g0" }

    func testQuickSitRingsStartNowAndHoldsOneBackupForTheEndBell() {
        _ = engine.startQuickSit(minutes: 1, soundId: 0, startChimes: 2, endChimes: 3)
        XCTAssertEqual(audio.plays.count, 1, "the first start chime plays at once")
        clock.now += 2.5
        engine.tick()
        XCTAssertEqual(audio.plays.count, 2, "the second follows 3 s later")
        XCTAssertEqual(Array(notifications.pending.keys), [endID])
        XCTAssertEqual(notifications.pending[endID]?.soundFile, "bell_0_x3.caf")
        XCTAssertEqual(notifications.pending[endID]?.fireIn ?? 0, 60, accuracy: 3)
    }

    func testTwelveChimesNeedTwoBackups() {
        _ = engine.startQuickSit(minutes: 5, soundId: 0, startChimes: 1, endChimes: 12)
        XCTAssertEqual(Set(notifications.pending.keys),
                       [endID, BellEngine.notificationPrefix + "quicksit.end.g1"])
        XCTAssertEqual(notifications.pending[endID]?.soundFile, "bell_0_x6.caf")
    }

    func testPlayingABellRemovesItsBackup() {
        _ = engine.startQuickSit(minutes: 1, soundId: 0, startChimes: 1, endChimes: 1)
        clock.now += 59.5
        engine.tick()
        XCTAssertEqual(audio.plays.count, 2)
        XCTAssertNil(notifications.pending[endID])
    }

    func testDuringAnInterruptionTheBackupRingsInstead() async {
        _ = engine.startQuickSit(minutes: 1, soundId: 0, startChimes: 1, endChimes: 1)
        await engine.handleInterruption(began: true)
        audio.activateSucceeds = false // iOS refuses the audio session during a call
        clock.now += 60.2
        engine.tick()
        XCTAssertEqual(audio.plays.count, 1, "the end bell is left to its notification")
        XCTAssertNotNil(notifications.pending[endID])

        // iOS rang it as a notification; when the call ends it must not ring again.
        notifications.delivered = [endID]
        clock.now += 5
        audio.activateSucceeds = true
        await engine.handleInterruption(began: false)
        XCTAssertEqual(audio.plays.count, 1)
        XCTAssertTrue(notifications.delivered.isEmpty, "fired backups are cleared from Notification Center")
    }

    func testAMissedBellWithoutBackupCatchesUpWithinAMinute() async {
        notifications.status.allowed = false
        await engine.refreshNotificationSettings(requestIfNeeded: false)
        _ = engine.startQuickSit(minutes: 1, soundId: 0, startChimes: 1, endChimes: 1)
        await engine.handleInterruption(began: true)
        audio.activateSucceeds = false
        clock.now += 70
        engine.tick()
        audio.activateSucceeds = true
        await engine.handleInterruption(began: false)
        XCTAssertEqual(audio.plays.count, 2, "10 s late, so it rings now")
    }

    func testRefusedAudioSessionCountsAsAnInterruption() {
        _ = engine.startQuickSit(minutes: 1, soundId: 0, startChimes: 1, endChimes: 1)
        audio.activateSucceeds = false
        clock.now += 59.5
        engine.tick()
        XCTAssertEqual(audio.plays.count, 1)
        XCTAssertNotNil(notifications.pending[endID])
        XCTAssertTrue(engine.audioInterrupted)
    }

    func testEndingTheSessionClearsEverything() {
        _ = engine.startQuickSit(minutes: 10, soundId: 0, startChimes: 1, endChimes: 1)
        notifications.delivered = ["x"]
        engine.endSession()
        XCTAssertTrue(notifications.pending.isEmpty)
        XCTAssertTrue(notifications.delivered.isEmpty)
        XCTAssertFalse(audio.keepAliveRunning)
        XCTAssertNil(engine.session)
    }

    func testBackupsReachOnlyTwoHoursAhead() {
        var day = Day.emptyDay
        day.name = "Hours"
        day.segments = (0..<6).map { _ in Day.Segment(name: "h", duration: 3600) }
        let start = Calendar.current.dateComponents([.hour, .minute, .second], from: clock.now)
        day.startTime = TimeInterval((start.hour ?? 0) * 3600 + (start.minute ?? 0) * 60 + (start.second ?? 0) + 10)
        engine.dayProvider = { _ in day }
        engine.startOrResumeDay(day, loopDays: false)
        // Start bell in 10 s and the first end bell at +1 h are held; the
        // next, at +2 h 10 s, is just past the horizon.
        XCTAssertEqual(notifications.pending.count, 2)
        XCTAssertTrue(notifications.pending.values.allSatisfy { $0.fireIn <= ChimePlanner.notificationHorizon })
    }
}
