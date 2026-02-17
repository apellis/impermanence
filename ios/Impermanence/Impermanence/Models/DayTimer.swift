//
//  DayTimer.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/14/23.
//

import Foundation
import SwiftUI

@MainActor
final class DayTimer: ObservableObject {
    struct Segment: Identifiable {
        let name: String
        var startTime: Date
        var endTime: Date
        let endBell: Bell
        var rang: Bool = false
        let id = UUID()
    }

    @Published var activeSegmentName = ""
    @Published var activeSegmentTimeElapsed: TimeInterval = 0
    @Published var activeSegmentTimeRemaining: TimeInterval = 0
    @Published var segmentIndex: Int = -1

    private(set) var segments: [DayTimer.Segment] = []
    var startTime: Date
    var loopDays: Bool
    private let startBell: Bell
    private var freshTimer = true

    var segmentChangedAction: ((Bell?) -> Void)?

    private var timer: Timer?
    private var frequency: TimeInterval { 1.0 }
    private static weak var activeOwner: DayTimer?

    var segmentText: String {
        switch segmentIndex {
        case -2:
            return "(Day is empty)"
        case -1:
            return "(Day has not started yet)"
        case 0...segments.count - 1:
            return "Now: \(segments[segmentIndex].name)"
        default:
            return "(Day is over)"
        }
    }

    init(startTime: TimeInterval = 0,
         segments: [Day.Segment] = [],
         startBell: Bell = .singleBell,
         defaultBell: Bell = .singleBell,
         loopDays: Bool = true,
         currentDate: Date = Date.now) {
        self.startBell = startBell
        self.loopDays = loopDays

        let startOfDay = Calendar.current.startOfDay(for: currentDate)
        self.startTime = startOfDay.addingTimeInterval(startTime)

        guard !segments.isEmpty else {
            segmentIndex = -2
            activeSegmentName = segmentText
            updateActiveTimes(for: segmentIndex, now: currentDate)
            return
        }

        var newSegments: [DayTimer.Segment] = []
        var timeCursor = self.startTime
        segments.forEach { segment in
            newSegments.append(
                DayTimer.Segment(
                    name: segment.name,
                    startTime: timeCursor,
                    endTime: timeCursor.addingTimeInterval(segment.duration),
                    endBell: segment.resolvedEndBell(defaultBell: defaultBell)
                )
            )
            timeCursor.addTimeInterval(segment.duration)
        }
        self.segments = newSegments
        segmentIndex = -1
        activeSegmentName = segmentText
        updateActiveTimes(for: segmentIndex, now: currentDate)
    }

    deinit {
        timer?.invalidate()
    }

    func startDay() {
        if let owner = Self.activeOwner, owner !== self {
            owner.stopDay()
        }
        guard timer == nil else { return }
        Self.activeOwner = self
        refresh()
        timer = Timer.scheduledTimer(withTimeInterval: frequency, repeats: true) { [weak self] _ in
            guard let self else { return }
            MainActor.assumeIsolated {
                self.refresh()
            }
        }
        timer?.tolerance = 0.2
    }

    func stopDay() {
        timer?.invalidate()
        timer = nil
        if Self.activeOwner === self {
            Self.activeOwner = nil
        }
    }

    func refresh(now: Date = Date.now) {
        if loopDays && !Calendar.current.isDate(now, equalTo: startTime, toGranularity: .day) {
            alignToCurrentDay(now: now)
        }
        updateSegments(now: now)
        freshTimer = false
    }

    private func alignToCurrentDay(now: Date) {
        let calendar = Calendar.current
        let startDay = calendar.startOfDay(for: startTime)
        let nowDay = calendar.startOfDay(for: now)
        guard let dayShift = calendar.dateComponents([.day], from: startDay, to: nowDay).day,
              dayShift != 0 else { return }

        guard let newStart = calendar.date(byAdding: .day, value: dayShift, to: startTime) else { return }
        startTime = newStart
        for index in segments.indices {
            if let newSegmentStart = calendar.date(byAdding: .day, value: dayShift, to: segments[index].startTime),
               let newSegmentEnd = calendar.date(byAdding: .day, value: dayShift, to: segments[index].endTime) {
                segments[index].startTime = newSegmentStart
                segments[index].endTime = newSegmentEnd
                segments[index].rang = false
            }
        }
        changeToSegment(at: -1, now: now)
        freshTimer = true
    }

    private func updateSegments(now: Date) {
        let previousIndex = segmentIndex
        let newIndex = nextSegmentIndex(for: now)

        if newIndex != segmentIndex {
            changeToSegment(at: newIndex, now: now)
            if shouldPlayBell(previousIndex: previousIndex, newIndex: newIndex),
               let bell = bellForTransition(previousIndex: previousIndex, newIndex: newIndex) {
                segmentChangedAction?(bell)
            }
        } else {
            updateActiveTimes(for: newIndex, now: now)
        }
    }

    private func nextSegmentIndex(for now: Date) -> Int {
        if segments.isEmpty {
            return -2
        }
        if now < startTime {
            return -1
        }
        if let last = segments.last, now >= last.endTime {
            return segments.count
        }
        for (index, segment) in segments.enumerated() where now >= segment.startTime && now < segment.endTime {
            return index
        }
        return segmentIndex
    }

    private func changeToSegment(at index: Int, now: Date) {
        if index > 0 && index - 1 < segments.count {
            segments[index - 1].rang = true
        }
        segmentIndex = index
        activeSegmentName = segmentText
        updateActiveTimes(for: index, now: now)
    }

    private func updateActiveTimes(for index: Int, now: Date) {
        if index >= 0 && index < segments.count {
            let elapsed = now.timeIntervalSinceReferenceDate - segments[index].startTime.timeIntervalSinceReferenceDate
            let remaining = segments[index].endTime.timeIntervalSince(now)

            // Avoid publishing every timer tick when displayed whole-second values have not changed.
            let elapsedSeconds = max(0, Int(elapsed.rounded(.down)))
            let remainingSeconds = max(0, Int(remaining.rounded(.down)))

            if Int(activeSegmentTimeElapsed.rounded(.down)) != elapsedSeconds {
                activeSegmentTimeElapsed = TimeInterval(elapsedSeconds)
            }
            if Int(activeSegmentTimeRemaining.rounded(.down)) != remainingSeconds {
                activeSegmentTimeRemaining = TimeInterval(remainingSeconds)
            }
        } else {
            if activeSegmentTimeElapsed != -1 {
                activeSegmentTimeElapsed = -1
            }
            if activeSegmentTimeRemaining != -1 {
                activeSegmentTimeRemaining = -1
            }
        }
    }

    private func shouldPlayBell(previousIndex: Int, newIndex: Int) -> Bool {
        if newIndex == 0 && previousIndex < 0 {
            return true
        }
        return !freshTimer
    }

    private func bellForTransition(previousIndex: Int, newIndex: Int) -> Bell? {
        if newIndex == 0 && previousIndex < 0 {
            return startBell
        }
        if previousIndex >= 0 && previousIndex < segments.count {
            return segments[previousIndex].endBell
        }
        return nil
    }
}
