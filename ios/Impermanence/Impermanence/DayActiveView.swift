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
    @StateObject var dayTimer: DayTimer

    @AppStorage("loopDays") private var loopDaysSetting = true
    @AppStorage("use24HourClock") private var use24HourClock = false
    @AppStorage("keepScreenAwakeDuringActiveDay") private var keepScreenAwakeDuringActiveDay = true

    private let bellPlayer = BellPlayer.shared

    init(day: Binding<Day>) {
        self._day = day
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
                    VStack(spacing: 10) {
                        Text("Schedule")
                            .font(.headline)
                            .frame(maxWidth: .infinity, alignment: .leading)
                        ForEach(day.segments.indices, id: \.self) { i in
                            let segment = day.segments[i]
                            SegmentCardView(segment: segment,
                                            startTime: day.segmentStartEndTimes[i].0,
                                            endTime: day.segmentStartEndTimes[i].1,
                                            theme: day.theme,
                                            bell: segment.resolvedEndBell(defaultBell: day.defaultBell),
                                            useTheme: true,
                                            highlighted: i == dayTimer.segmentIndex)
                        }
                        .animation(.easeInOut(duration: 0.25), value: dayTimer.segmentIndex)
                    }
                }
                .padding(.horizontal, UIStyle.screenPadding)
                .padding(.bottom, UIStyle.screenPadding)
            }
        }
        .padding(UIStyle.screenPadding)
        .foregroundColor(day.theme.accentColor)
        .onAppear {
            dayTimer.segmentChangedAction = { bell in
                guard let bell else { return }
                bellPlayer.play(bell: bell)
            }
            dayTimer.loopDays = loopDaysSetting
            dayTimer.startDay()
            updateIdleTimerBehavior()
        }
        .onDisappear {
            dayTimer.stopDay()
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .onChange(of: loopDaysSetting) { newValue in
            dayTimer.loopDays = newValue
        }
        .onChange(of: keepScreenAwakeDuringActiveDay) { _ in
            updateIdleTimerBehavior()
        }
        .onChange(of: dayTimer.segmentIndex) { _ in
            updateIdleTimerBehavior()
        }
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .navigationBarTrailing) {
                Menu {
                    Button {
                        bellPlayer.play(bell: day.manualBell)
                    } label: {
                        Label("Ring now", systemImage: "play.circle")
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
                            Label("Chimes: \(day.manualBell.numRings)", systemImage: "bell")
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
        let schedules = day.segmentStartEndTimes
        guard !schedules.isEmpty else { return nil }
        if dayTimer.segmentIndex < 0 {
            return TimeFormatting.formattedTime(from: schedules[0].0, use24HourClock: use24HourClock)
        }
        guard dayTimer.segmentIndex < schedules.count else { return nil }
        return TimeFormatting.formattedTime(from: schedules[dayTimer.segmentIndex].1, use24HourClock: use24HourClock)
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

    private func formatDuration(_ duration: TimeInterval) -> String {
        let formatter = DateComponentsFormatter()
        formatter.allowedUnits = [.hour, .minute, .second]
        formatter.zeroFormattingBehavior = .pad
        return formatter.string(from: max(0, duration)) ?? "--:--"
    }
}

struct DayActiveView_Previews: PreviewProvider {
    static var previews: some View {
        DayActiveView(day: .constant(Day.fullDay))
    }
}
