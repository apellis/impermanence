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
