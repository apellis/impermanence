//
//  DayActiveView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI
import UIKit

struct DayActiveView: View {
    @Binding var day: Day
    @Environment(\.scenePhase) private var scenePhase
    @StateObject var dayTimer: DayTimer
    @State private var scheduleRows: [ScheduleRowData]
    @State private var startTask: Task<Void, Never>?

    @AppStorage("loopDays") private var loopDaysSetting = true
    @AppStorage("use24HourClock") private var use24HourClock = false
    @AppStorage("keepScreenAwakeDuringActiveDay") private var keepScreenAwakeDuringActiveDay = true

    private let bellPlayer = BellPlayer.shared

    private struct ScheduleRowData: Identifiable, Equatable {
        let id: UUID
        let segment: Day.Segment
        let startTime: Date
        let endTime: Date
        let bell: Bell

        static func == (lhs: ScheduleRowData, rhs: ScheduleRowData) -> Bool {
            lhs.id == rhs.id &&
            lhs.segment.name == rhs.segment.name &&
            lhs.segment.duration == rhs.segment.duration &&
            lhs.segment.customEndBell == rhs.segment.customEndBell &&
            lhs.startTime == rhs.startTime &&
            lhs.endTime == rhs.endTime &&
            lhs.bell == rhs.bell
        }
    }

    private struct ScheduleListView: View {
        let rows: [ScheduleRowData]
        let theme: Theme
        let use24HourClock: Bool
        let highlightedIndex: Int

        var body: some View {
            LazyVStack(spacing: 10) {
                Text("Schedule")
                    .font(.headline)
                    .frame(maxWidth: .infinity, alignment: .leading)
                ForEach(rows.indices, id: \.self) { index in
                    let row = rows[index]
                    SegmentCardView(
                        segment: row.segment,
                        startTime: row.startTime,
                        endTime: row.endTime,
                        theme: theme,
                        bell: row.bell,
                        use24HourClock: use24HourClock,
                        useTheme: true,
                        highlighted: index == highlightedIndex
                    )
                }
                .animation(.easeInOut(duration: 0.25), value: highlightedIndex)
            }
        }
    }

    init(day: Binding<Day>) {
        self._day = day
        self._scheduleRows = State(initialValue: Self.buildScheduleRows(for: day.wrappedValue))
        let storedLoopSetting: Bool = {
            if UserDefaults.standard.object(forKey: "loopDays") == nil {
                return true
            }
            return UserDefaults.standard.bool(forKey: "loopDays")
        }()

        _dayTimer = StateObject(
            wrappedValue: DayTimer(
                startTime: day.wrappedValue.startTime,
                segments: day.wrappedValue.segments,
                startBell: day.wrappedValue.startBell,
                defaultBell: day.wrappedValue.defaultBell,
                loopDays: storedLoopSetting
            )
        )
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16.0)
                .fill(day.theme.mainColor)
            VStack(spacing: UIStyle.sectionSpacing) {
                activeSummary
                if dayTimer.activeSegmentTimeElapsed >= 0 && dayTimer.activeSegmentTimeRemaining >= 0 {
                    SegmentProgressView(timeElapsed: dayTimer.activeSegmentTimeElapsed, timeRemaining: dayTimer.activeSegmentTimeRemaining, theme: day.theme)
                }
                Divider()
                    .frame(height: 2)
                    .overlay(day.theme.accentColor)
                ScrollView {
                    ScheduleListView(rows: scheduleRows,
                                     theme: day.theme,
                                     use24HourClock: use24HourClock,
                                     highlightedIndex: dayTimer.segmentIndex)
                }
                .padding(.horizontal, UIStyle.screenPadding)
                .padding(.bottom, UIStyle.screenPadding)
            }
        }
        .padding(UIStyle.screenPadding)
        .foregroundColor(day.theme.accentColor)
        .onAppear {
            BellNotificationScheduler.prepareAuthorization()
            dayTimer.segmentChangedAction = { bell in
                guard let bell else { return }
                bellPlayer.play(bell: bell)
            }
            scheduleRows = Self.buildScheduleRows(for: day)
            dayTimer.loopDays = loopDaysSetting
            startTask?.cancel()
            startTask = Task { @MainActor in
                try? await Task.sleep(nanoseconds: 250_000_000)
                guard !Task.isCancelled else { return }
                dayTimer.startDay()
                updateIdleTimerBehavior()
                handleScenePhaseChange(scenePhase)
            }
            updateIdleTimerBehavior()
        }
        .onDisappear {
            startTask?.cancel()
            startTask = nil
            dayTimer.stopDay()
            BellNotificationScheduler.cancelDayBells()
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .onChange(of: loopDaysSetting) { newValue in
            dayTimer.loopDays = newValue
            if scenePhase != .active {
                scheduleBackgroundBellNotificationsIfNeeded()
            }
        }
        .onChange(of: keepScreenAwakeDuringActiveDay) { _ in
            updateIdleTimerBehavior()
        }
        .onChange(of: dayTimer.segmentIndex) { _ in
            updateIdleTimerBehavior()
        }
        .onChange(of: scenePhase) { phase in
            handleScenePhaseChange(phase)
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button {
                        bellPlayer.play(bell: day.manualBell)
                    } label: {
                        HStack(spacing: 6) {
                            Image(systemName: "play.circle")
                            Text("Ring now")
                        }
                    }

                    Section("Manual bell sound") {
                        Picker("Sound", selection: $day.manualBell.soundId) {
                            ForEach(BellCatalog.sounds) { sound in
                                Text(sound.displayName).tag(sound.id)
                            }
                        }
                        .pickerStyle(.automatic)
                    }

                    Section("Chimes") {
                        Stepper(value: $day.manualBell.numRings, in: 1...12) {
                            HStack(spacing: 6) {
                                Image(systemName: "bell")
                                Text("Chimes: \(day.manualBell.numRings)")
                            }
                        }
                    }
                } label: {
                    Image(systemName: "bell")
                }
                .accessibilityLabel("Manual bell controls")
            }
        }
    }

    private var activeSummary: some View {
        VStack(alignment: .leading, spacing: 6) {
            Text(statusTitle)
                .font(.caption)
                .textCase(.uppercase)
                .fontWeight(.semibold)
            Text(dayTimer.segmentText)
                .font(.title3)
                .fontWeight(.semibold)
            if dayTimer.activeSegmentTimeRemaining >= 0 {
                Text("Remaining \(formatDuration(dayTimer.activeSegmentTimeRemaining))")
                    .font(.headline)
            }
            if let nextBellTime {
                Text("Next bell at \(nextBellTime)")
                    .font(.subheadline)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(UIStyle.cardPadding)
        .background(day.theme.accentColor.opacity(0.14))
        .clipShape(RoundedRectangle(cornerRadius: UIStyle.cardCorner))
        .accessibilityElement(children: .combine)
    }

    private var statusTitle: String {
        switch dayTimer.segmentIndex {
        case -2:
            return "Empty"
        case -1:
            return "Day queued"
        case 0..<day.segments.count:
            return "Now"
        default:
            return "Complete"
        }
    }

    private var nextBellTime: String? {
        guard !scheduleRows.isEmpty else { return nil }
        if dayTimer.segmentIndex < 0 {
            return TimeFormatting.formattedTime(from: scheduleRows[0].startTime, use24HourClock: use24HourClock)
        }
        guard dayTimer.segmentIndex < scheduleRows.count else { return nil }
        return TimeFormatting.formattedTime(from: scheduleRows[dayTimer.segmentIndex].endTime, use24HourClock: use24HourClock)
    }

    private func updateIdleTimerBehavior() {
        UIApplication.shared.isIdleTimerDisabled = keepScreenAwakeDuringActiveDay && isSessionActive
    }

    private var isSessionActive: Bool {
        let index = dayTimer.segmentIndex
        if day.segments.isEmpty {
            return false
        }
        if index == -1 {
            return true
        }
        return index >= 0 && index < day.segments.count
    }

    private func handleScenePhaseChange(_ phase: ScenePhase) {
        switch phase {
        case .active:
            BellNotificationScheduler.cancelDayBells()
        case .inactive, .background:
            scheduleBackgroundBellNotificationsIfNeeded()
        @unknown default:
            break
        }
    }

    private func scheduleBackgroundBellNotificationsIfNeeded() {
        guard isSessionActive else {
            BellNotificationScheduler.cancelDayBells()
            return
        }
        BellNotificationScheduler.scheduleDayBells(day: day, loopDays: loopDaysSetting)
    }

    private func formatDuration(_ duration: TimeInterval) -> String {
        TimeFormatting.formattedDuration(from: duration)
    }

    private static func buildScheduleRows(for day: Day) -> [ScheduleRowData] {
        let schedules = day.segmentStartEndTimes
        guard schedules.count == day.segments.count else { return [] }
        return day.segments.enumerated().map { index, segment in
            ScheduleRowData(id: segment.id,
                            segment: segment,
                            startTime: schedules[index].0,
                            endTime: schedules[index].1,
                            bell: segment.resolvedEndBell(defaultBell: day.defaultBell))
        }
    }
}

struct DayActiveView_Previews: PreviewProvider {
    static var previews: some View {
        DayActiveView(day: .constant(Day.fullDay))
    }
}
