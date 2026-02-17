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
    private static let defaultDays: [Day] = [Day.openingDay, Day.fullDay]

    private static func fileURL() throws -> URL {
        try FileManager.default.url(for: .documentDirectory,
                                    in: .userDomainMask,
                                    appropriateFor: nil,
                                    create: false)
        .appendingPathComponent("days.data")
    }

    func load() async throws {
        let task = Task<[Day], Error> {
            if ProcessInfo.processInfo.arguments.contains("--uitesting-reset-days") {
                return Self.defaultDays
            }
            let fileURL = try Self.fileURL()
            guard let data = try? Data(contentsOf: fileURL) else {
                return Self.defaultDays
            }
            let days = try JSONDecoder().decode([Day].self, from: data)
            return days
        }
        let days = try await task.value
        self.days = days
    }

    func save(days: [Day]) async throws {
        let task = Task {
            let data = try JSONEncoder().encode(days)
            let outfile = try Self.fileURL()
            try data.write(to: outfile)
        }
        _ = try await task.value
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

    static func importDays(from data: Data) throws -> [Day] {
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
                            duration: TimeInterval(max(1, segment.durationSeconds)),
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

        throw CocoaError(.fileReadCorruptFile)
    }

    private static func normalizedSeconds(from startTime: TimeInterval) -> Int {
        normalizedSeconds(Int(startTime.rounded()))
    }

    private static func normalizedSeconds(_ seconds: Int) -> Int {
        let mod = seconds % secondsPerDay
        return mod < 0 ? mod + secondsPerDay : mod
    }
}
