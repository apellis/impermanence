//
//  DayStore.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/17/23.
//

import SwiftUI

@MainActor
class DayStore: ObservableObject {
    @Published var days: [Day] = []
    /// A saved days file that couldn't be read. It is kept, untouched, so the
    /// user can save it somewhere; the app starts with an empty list.
    @Published var unreadableFile: URL?
    private static let defaultDays: [Day] = [Day.openingDay, Day.fullDay]

    struct UnreadableDaysError: LocalizedError {
        var errorDescription: String? {
            "Your saved days couldn't be read. A copy of the file was kept, so nothing is lost. You can save it and send it for help."
        }
    }

    private static func documents() throws -> URL {
        try FileManager.default.url(for: .documentDirectory, in: .userDomainMask, appropriateFor: nil, create: true)
    }

    private static func fileURL() throws -> URL {
        try documents().appendingPathComponent("days.data")
    }

    func load() async throws {
        let arguments = ProcessInfo.processInfo.arguments
        if arguments.contains("--uitesting-reset-days") {
            days = Self.uiTestDays(startSoon: arguments.contains("--uitesting-days-start-soon"))
            return
        }
        let fileURL = try Self.fileURL()
        guard FileManager.default.fileExists(atPath: fileURL.path) else {
            days = Self.defaultDays
            return
        }
        do {
            let data = try Data(contentsOf: fileURL)
            days = try JSONDecoder().decode([Day].self, from: data)
        } catch {
            // Keep the unreadable file under another name before anything can
            // be saved over days.data.
            let stamp = ISO8601DateFormatter().string(from: Date()).replacingOccurrences(of: ":", with: "-")
            let copy = try Self.documents().appendingPathComponent("days-unreadable-\(stamp).data")
            try? FileManager.default.copyItem(at: fileURL, to: copy)
            unreadableFile = FileManager.default.fileExists(atPath: copy.path) ? copy : fileURL
            // `days` stays empty; it isn't assigned, so nothing is saved
            // until the user changes something.
            throw UnreadableDaysError()
        }
    }

    func save(days: [Day]) async throws {
        let data = try JSONEncoder().encode(days)
        let outfile = try Self.fileURL()
        // Atomic: a kill mid-write leaves the previous file, never half a file.
        try data.write(to: outfile, options: [.atomic, .completeFileProtectionUntilFirstUserAuthentication])
    }

    /// UI tests: the seed days, optionally starting 5 minutes from now so
    /// their first bells fall inside the backup notification horizon.
    private static func uiTestDays(startSoon: Bool) -> [Day] {
        guard startSoon else { return defaultDays }
        let soon = Calendar.current.dateComponents([.hour, .minute], from: Date().addingTimeInterval(300))
        return defaultDays.map { day in
            var d = day
            d.startTime = TimeInterval((soon.hour ?? 0) * 3600 + (soon.minute ?? 0) * 60)
            return d
        }
    }
}

enum PortableDayPlanCodec {
    private static let currentVersion = 1
    private static let secondsPerDay = 24 * 60 * 60

    private struct FileEnvelope: Codable {
        var version: Int
        var days: [PortableDay]
    }

    private struct PortableDay: Codable {
        var id: String
        var name: String
        var startTimeSeconds: Int
        var segments: [PortableSegment]
        var startBell: Bell
        var manualBell: Bell
        var defaultBell: Bell
        var theme: String
    }

    private struct PortableSegment: Codable {
        var id: String
        var name: String
        var durationSeconds: Int
        var customEndBell: Bell?
    }

    static func exportData(days: [Day]) throws -> Data {
        let payload = FileEnvelope(
            version: currentVersion,
            days: days.map { day in
                PortableDay(
                    id: day.id.uuidString,
                    name: day.name,
                    startTimeSeconds: normalizedSeconds(from: day.startTime),
                    segments: day.segments.map { segment in
                        PortableSegment(
                            id: segment.id.uuidString,
                            name: segment.name,
                            durationSeconds: max(1, Int(segment.duration.rounded())),
                            customEndBell: segment.customEndBell?.sanitized()
                        )
                    },
                    startBell: day.startBell.sanitized(),
                    manualBell: day.manualBell.sanitized(),
                    defaultBell: day.defaultBell.sanitized(),
                    theme: day.theme.rawValue
                )
            }
        )
        let encoder = JSONEncoder()
        encoder.outputFormatting = [.prettyPrinted, .sortedKeys]
        return try encoder.encode(payload)
    }

    static let maxFileBytes = 1_000_000
    static let maxDays = 200
    static let maxSegmentsPerDay = 500
    static let maxNameLength = 200

    enum ImportError: LocalizedError, Equatable {
        case tooLarge, noDays, tooManyDays, dayHasNoSegments(String), tooManySegments(String), unreadable

        var errorDescription: String? {
            switch self {
            case .tooLarge: return "The file is larger than 1 MB, so it isn't a day plan file."
            case .noDays: return "The file has no days. Your days were not changed."
            case .tooManyDays: return "The file has more than \(PortableDayPlanCodec.maxDays) days."
            case .dayHasNoSegments(let name): return "\"\(name)\" has no segments. Every day needs at least one."
            case .tooManySegments(let name): return "\"\(name)\" has more than \(PortableDayPlanCodec.maxSegmentsPerDay) segments."
            case .unreadable: return "The file isn't an Impermanence day plan."
            }
        }
    }

    /// Reads an exported file (or the iOS native array) and checks it
    /// against docs/BEHAVIOR.md before it may replace the user's days.
    static func importDays(from data: Data) throws -> [Day] {
        guard data.count <= maxFileBytes else { throw ImportError.tooLarge }
        return try validated(decode(data))
    }

    private static func decode(_ data: Data) throws -> [Day] {
        let decoder = JSONDecoder()

        if let envelope = try? decoder.decode(FileEnvelope.self, from: data) {
            return envelope.days.map { portable in
                Day(
                    id: UUID(uuidString: portable.id) ?? UUID(),
                    name: portable.name,
                    startTime: TimeInterval(normalizedSeconds(portable.startTimeSeconds)),
                    segments: portable.segments.map { segment in
                        Day.Segment(
                            id: UUID(uuidString: segment.id) ?? UUID(),
                            name: segment.name,
                            duration: TimeInterval(segment.durationSeconds),
                            customEndBell: segment.customEndBell?.sanitized()
                        )
                    },
                    startBell: portable.startBell.sanitized(),
                    manualBell: portable.manualBell.sanitized(),
                    defaultBell: portable.defaultBell.sanitized(),
                    theme: Theme(rawValue: portable.theme) ?? .teal
                )
            }
        }

        if let nativeDays = try? decoder.decode([Day].self, from: data) {
            return nativeDays
        }

        throw ImportError.unreadable
    }

    /// Refuses what can't be fixed (no days, a day with no segments, too
    /// many of either) and fixes the rest: names are trimmed and given if
    /// missing, durations are kept to 1 s ... 24 h, and repeated ids get
    /// new ones (navigation finds days by id).
    static func validated(_ days: [Day]) throws -> [Day] {
        guard !days.isEmpty else { throw ImportError.noDays }
        guard days.count <= maxDays else { throw ImportError.tooManyDays }
        var dayIDs = Set<UUID>()
        var segmentIDs = Set<UUID>()
        return try days.enumerated().map { index, original in
            var day = original
            day.name = String(day.name.trimmingCharacters(in: .whitespacesAndNewlines).prefix(maxNameLength))
            if day.name.isEmpty { day.name = "Day \(index + 1)" }
            guard !day.segments.isEmpty else { throw ImportError.dayHasNoSegments(day.name) }
            guard day.segments.count <= maxSegmentsPerDay else { throw ImportError.tooManySegments(day.name) }
            if !dayIDs.insert(day.id).inserted {
                day = Day(id: UUID(), name: day.name, startTime: day.startTime, segments: day.segments,
                          startBell: day.startBell, manualBell: day.manualBell, defaultBell: day.defaultBell, theme: day.theme)
            }
            day.segments = day.segments.map { original in
                var segment = original
                segment.name = String(segment.name.prefix(maxNameLength))
                if !segment.duration.isFinite || segment.duration < TimeInterval(SegmentDuration.minSeconds) {
                    segment.duration = TimeInterval(SegmentDuration.minSeconds)
                } else if segment.duration > TimeInterval(SegmentDuration.maxSeconds) {
                    segment.duration = TimeInterval(SegmentDuration.maxSeconds)
                }
                if !segmentIDs.insert(segment.id).inserted { segment.id = UUID() }
                return segment
            }
            return day
        }
    }

    private static func normalizedSeconds(from startTime: TimeInterval) -> Int {
        normalizedSeconds(Int(startTime.rounded()))
    }

    private static func normalizedSeconds(_ seconds: Int) -> Int {
        let mod = seconds % secondsPerDay
        return mod < 0 ? mod + secondsPerDay : mod
    }
}
