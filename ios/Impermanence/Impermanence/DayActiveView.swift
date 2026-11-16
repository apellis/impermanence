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
    @StateObject private var dayTimer: DayTimer
    @ObservedObject private var engine = BellEngine.shared

    @AppStorage("loopDays") private var loopDaysSetting = true
    @AppStorage("use24HourClock") private var use24HourClock = false
    @AppStorage("keepScreenAwakeDuringActiveDay") private var keepScreenAwakeDuringActiveDay = true

    private struct ScheduleRowData: Identifiable {
        let id: UUID
        let segment: Day.Segment
        let startTime: Date
        let endTime: Date
        let bell: Bell
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
        let storedLoopSetting = UserDefaults.standard.object(forKey: "loopDays") == nil
            ? true
            : UserDefaults.standard.bool(forKey: "loopDays")
        _dayTimer = StateObject(wrappedValue: DayTimer(day: day.wrappedValue, loopDays: storedLoopSetting))
    }

    var body: some View {
        ZStack {
            RoundedRectangle(cornerRadius: 16.0)
                .fill(day.theme.mainColor)
            VStack(spacing: UIStyle.sectionSpacing) {
                BellWarningsView(engine: engine)
                activeSummary
                if let elapsed = dayTimer.progress.elapsed, let remaining = dayTimer.progress.remaining {
                    SegmentProgressView(timeElapsed: elapsed.rounded(.down), timeRemaining: remaining.rounded(.down), theme: day.theme)
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
            // Being on this screen is "timing this day": start the bell
            // session, or rejoin it if it is already running (resumed at launch).
            let anchor = engine.startOrResumeDay(dayTimer.day, loopDays: loopDaysSetting)
            dayTimer.attach(anchor: anchor)
            dayTimer.loopDays = loopDaysSetting
            dayTimer.start()
            updateIdleTimerBehavior()
        }
        .onDisappear {
            // Leaving this screen ends the session: no more bells.
            dayTimer.stop()
            engine.endSession()
            UIApplication.shared.isIdleTimerDisabled = false
        }
        .onChange(of: loopDaysSetting) { newValue in
            dayTimer.loopDays = newValue
            engine.setLoopDays(newValue)
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
                        engine.ringNow(day.manualBell)
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
                    Label("Manual bell controls", systemImage: "bell")
                        .labelStyle(.iconOnly)
                }
                .accessibilityLabel("Manual bell controls")
            }
        }
    }

    private var scheduleRows: [ScheduleRowData] {
        guard let occurrence = dayTimer.progress.occurrence,
              occurrence.segments.count == dayTimer.day.segments.count else { return [] }
        return dayTimer.day.segments.enumerated().map { index, segment in
            ScheduleRowData(id: segment.id,
                            segment: segment,
                            startTime: occurrence.segments[index].start,
                            endTime: occurrence.segments[index].end,
                            bell: segment.resolvedEndBell(defaultBell: dayTimer.day.defaultBell))
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
            if let remaining = dayTimer.progress.remaining {
                Text("Remaining \(TimeFormatting.formattedDuration(from: remaining))")
                    .font(.headline)
            }
            if let next = dayTimer.progress.nextBell {
                Text("Next bell at \(TimeFormatting.formattedTime(from: next, use24HourClock: use24HourClock))")
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
        switch dayTimer.progress.status {
        case .empty: return "Empty"
        case .queued: return "Day queued"
        case .running: return "Now"
        case .complete: return "Complete"
        }
    }

    private func updateIdleTimerBehavior() {
        let active: Bool
        switch dayTimer.progress.status {
        case .queued, .running: active = true
        case .empty, .complete: active = false
        }
        UIApplication.shared.isIdleTimerDisabled = keepScreenAwakeDuringActiveDay && active
    }
}

/// Tells the user when something outside the app could keep bells from
/// being heard.
struct BellWarningsView: View {
    @ObservedObject var engine: BellEngine

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if !engine.notificationsAllowed {
                warning("Notifications are off. Bells ring while Impermanence is running, but not if iOS closes it.",
                        systemImage: "bell.slash")
            }
            if engine.outputVolume < 0.1 {
                warning("Volume is very low. Turn it up to hear the bells.", systemImage: "speaker.slash")
            }
        }
    }

    @ViewBuilder
    private func warning(_ text: String, systemImage: String) -> some View {
        HStack(alignment: .top, spacing: 8) {
            Image(systemName: systemImage)
            VStack(alignment: .leading, spacing: 4) {
                Text(text).font(.footnote)
                if systemImage == "bell.slash" {
                    Button("Open Settings") {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    .font(.footnote.weight(.semibold))
                }
            }
        }
        .padding(10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Color.yellow.opacity(0.9))
        .foregroundColor(.black)
        .clipShape(RoundedRectangle(cornerRadius: 10))
        .accessibilityElement(children: .combine)
    }
}

struct DayActiveView_Previews: PreviewProvider {
    static var previews: some View {
        DayActiveView(day: .constant(Day.fullDay))
    }
}
