//
//  DayTimer.swift
//  Impermanence
//
//  Display state for the active-day screen. It never rings bells: BellEngine
//  does. Both read the same schedule from BellSchedule.swift.
//

import Foundation
import SwiftUI

@MainActor
final class DayTimer: ObservableObject {
    @Published private(set) var progress: DayProgress

    let day: Day
    private(set) var anchor: CalendarDay
    var loopDays: Bool {
        didSet { refresh() }
    }
    private var timer: Timer?

    init(day: Day, loopDays: Bool, anchor: CalendarDay? = nil, now: Date = .now) {
        let calendar = Calendar.autoupdatingCurrent
        self.day = day
        self.loopDays = loopDays
        let resolvedAnchor = anchor ?? DaySchedule.initialOccurrence(of: day, at: now, calendar: calendar).date
        self.anchor = resolvedAnchor
        let occurrence = DaySchedule.currentOccurrence(of: day, anchor: resolvedAnchor, at: now, loopDays: loopDays, calendar: calendar)
        self.progress = DayProgress.at(now, occurrence: day.segments.isEmpty ? nil : occurrence)
    }

    deinit {
        timer?.invalidate()
    }

    /// The occurrence date the bell engine is timing; keeps display and bells
    /// on the same run.
    func attach(anchor: CalendarDay) {
        self.anchor = anchor
        refresh()
    }

    func start() {
        guard timer == nil else { return }
        refresh()
        let t = Timer(timeInterval: 0.5, repeats: true) { [weak self] _ in
            MainActor.assumeIsolated { self?.refresh() }
        }
        t.tolerance = 0.1
        RunLoop.main.add(t, forMode: .common)
        timer = t
    }

    func stop() {
        timer?.invalidate()
        timer = nil
    }

    func refresh(now: Date = .now) {
        let calendar = Calendar.autoupdatingCurrent
        let occurrence = DaySchedule.currentOccurrence(of: day, anchor: anchor, at: now, loopDays: loopDays, calendar: calendar)
        let next = DayProgress.at(now, occurrence: day.segments.isEmpty ? nil : occurrence)
        // Publish only when what is shown (whole seconds) changes.
        if next.status != progress.status || next.occurrence != progress.occurrence
            || wholeSeconds(next.remaining) != wholeSeconds(progress.remaining)
            || wholeSeconds(next.elapsed) != wholeSeconds(progress.elapsed) {
            progress = next
        }
    }

    var segmentIndex: Int { progress.segmentIndex }

    var segmentText: String {
        switch progress.status {
        case .empty: return "(Day is empty)"
        case .queued: return "(Day has not started yet)"
        case .running(let i): return i < day.segments.count ? "Now: \(day.segments[i].name)" : "Now"
        case .complete: return "(Day is over)"
        }
    }

    private func wholeSeconds(_ value: TimeInterval?) -> Int? {
        value.map { Int($0.rounded(.down)) }
    }
}
