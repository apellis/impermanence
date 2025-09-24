//
//  DayDetailEditView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI

struct DayDetailEditView: View {
    @Binding var day: Day
    @State private var newSegmentName = ""
    @State private var newSegmentHours: Int = 0
    @State private var newSegmentMinutes: Int = 0
    @State private var newSegmentChimes: Int = 1
    @AppStorage("use24HourClock") private var use24HourClock = false

    private var segmentScheduleMap: [UUID: (Date, Date)] {
        Self.segmentSchedules(for: day)
    }

    var body: some View {
        Form {
            Section(header: Text("Day Info")) {
                TextField("Name", text: $day.name)
                DatePicker("Start of Day", selection: $day.startTimeAsDate, displayedComponents: .hourAndMinute)
                    .accessibilityValue("\(day.startTimeAsDate)")
                ThemePicker(selection: $day.theme)
            }
            .listSectionSeparator(.hidden)
            Section(header:
                VStack(alignment: .leading) {
                    Text("Segments").fontWeight(.bold)
                    // TODO add a help button using SF Symbol questionmark.circle with instructions for editing
                }
            ) {
                ForEach($day.segments) { $segment in
                    SegmentEditorRow(
                        segment: $segment,
                        startTime: segmentScheduleMap[segment.id]?.0 ?? day.startTimeAsDate,
                        endTime: segmentScheduleMap[segment.id]?.1 ?? day.startTimeAsDate,
                        theme: day.theme,
                        use24HourClock: use24HourClock
                    )
                }
                .onDelete { indices in
                    day.segments.remove(atOffsets: indices)
                }
                .onMove { from, to in
                    day.segments.move(fromOffsets: from, toOffset: to)
                }
            }
            .listSectionSeparator(.hidden)
            Section(header: Text("New Segment")) {
                VStack {
                    TextField("New Segment Name", text: $newSegmentName)
                    HStack {
                        Picker("", selection: $newSegmentHours) {
                            ForEach(0..<24, id: \.self) { i in
                                Text("\(i)h").tag(i)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                        Picker("", selection: $newSegmentMinutes) {
                            ForEach(0..<60, id: \.self) { i in
                                Text("\(i)min").tag(i)
                            }
                        }
                        .pickerStyle(WheelPickerStyle())
                    }
                    .padding(.horizontal)
                    .accessibilityElement(children: .combine)
                    .accessibilityValue("\(newSegmentHours) hours, \(newSegmentMinutes) minutes")
                    Stepper(value: $newSegmentChimes, in: 1...12) {
                        Label("End bell chimes: \(newSegmentChimes)", systemImage: "bell.badge")
                            .accessibilityLabel("Number of chimes")
                            .accessibilityValue("\(newSegmentChimes)")
                    }
                    Button(action: {
                        withAnimation {
                            var bell = Bell.singleBell
                            bell.numRings = newSegmentChimes
                            let segment = Day.Segment(name: newSegmentName, duration: TimeInterval(60 * 60 * newSegmentHours + 60 * newSegmentMinutes), endBell: bell)
                            day.segments.append(segment)
                            newSegmentName = ""
                            newSegmentHours = 0
                            newSegmentMinutes = 0
                            newSegmentChimes = 1
                        }
                    }) {
                        Text("Add")
                            .accessibilityLabel("Add segment")
                    }
                    .disabled(newSegmentName.isEmpty || newSegmentHours < 0 || newSegmentMinutes < 0 || newSegmentHours + newSegmentMinutes == 0)
                }
            }
            .listSectionSeparator(.hidden)
            .listRowSeparator(.hidden)
        }
        .scrollContentBackground(.hidden)
    }
}

extension DayDetailEditView {
    static let maxDurationMinutes = 24 * 60

    static func segmentSchedules(for day: Day) -> [UUID: (Date, Date)] {
        let schedules = day.segmentStartEndTimes
        var lookup: [UUID: (Date, Date)] = [:]
        for (segment, schedule) in zip(day.segments, schedules) {
            lookup[segment.id] = schedule
        }
        return lookup
    }

    static func minutes(from duration: TimeInterval) -> Int {
        let minutes = Int((duration / 60).rounded())
        return clampedMinutes(minutes)
    }

    static func timeInterval(fromMinutes minutes: Int) -> TimeInterval {
        TimeInterval(clampedMinutes(minutes) * 60)
    }

    static func clampedMinutes(_ minutes: Int) -> Int {
        min(max(minutes, 1), maxDurationMinutes)
    }
}

private struct SegmentEditorRow: View {
    @Binding var segment: Day.Segment
    let startTime: Date
    let endTime: Date
    let theme: Theme
    let use24HourClock: Bool

    private var chimeBinding: Binding<Int> {
        Binding(
            get: { segment.endBell.numRings },
            set: { newValue in
                segment.endBell.numRings = newValue
            }
        )
    }

    private var durationMinutesBinding: Binding<Int> {
        Binding(
            get: { DayDetailEditView.minutes(from: segment.duration) },
            set: { segment.duration = DayDetailEditView.timeInterval(fromMinutes: $0) }
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .firstTextBaseline, spacing: 12) {
                TextField("Segment name", text: $segment.name)
                    .font(.headline)
                    .textInputAutocapitalization(.words)
                    .disableAutocorrection(false)
                Spacer(minLength: 8)
                Label(timeRangeText, systemImage: "clock")
                    .font(.caption)
                    .monospacedDigit()
            }
            Stepper(value: durationMinutesBinding, in: 1...DayDetailEditView.maxDurationMinutes) {
                Label("Duration: \(formattedDuration(durationMinutesBinding.wrappedValue))", systemImage: "hourglass")
                    .accessibilityLabel("Segment duration")
                    .accessibilityValue("\(durationMinutesBinding.wrappedValue) minutes")
            }
            quickAdjustRow
            Stepper(value: chimeBinding, in: 1...12) {
                Label("End bell chimes: \(segment.endBell.numRings)", systemImage: "bell.badge")
                    .accessibilityLabel("Number of chimes")
                    .accessibilityValue("\(segment.endBell.numRings)")
            }
        }
        .padding(.vertical, 4)
    }

    private var timeRangeText: String {
        let start = TimeFormatting.formattedTime(from: startTime, use24HourClock: use24HourClock)
        let end = TimeFormatting.formattedTime(from: endTime, use24HourClock: use24HourClock)
        return "\(start) – \(end)"
    }

    private func formattedDuration(_ minutes: Int) -> String {
        let hours = minutes / 60
        let remaining = minutes % 60
        switch (hours, remaining) {
        case (0, let m):
            return "\(m)m"
        case (let h, 0):
            return "\(h)h"
        default:
            return "\(hours)h \(remaining)m"
        }
    }

    private var quickAdjustRow: some View {
        HStack(spacing: 12) {
            Button {
                adjustDuration(by: -5)
            } label: {
                Label("-5 min", systemImage: "minus.circle")
                    .labelStyle(.titleAndIcon)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Subtract five minutes")

            Button {
                adjustDuration(by: 5)
            } label: {
                Label("+5 min", systemImage: "plus.circle")
                    .labelStyle(.titleAndIcon)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Add five minutes")
        }
    }

    private func adjustDuration(by delta: Int) {
        let current = durationMinutesBinding.wrappedValue
        let updated = DayDetailEditView.clampedMinutes(current + delta)
        durationMinutesBinding.wrappedValue = updated
    }
}

struct DayDetailEditView_Previews: PreviewProvider {
    static var previews: some View {
        DayDetailEditView(day: .constant(Day.fullDay))
    }
}
