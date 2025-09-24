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
    @State private var newSegmentUsesDefaultBell = true
    @State private var newSegmentSoundId = BellCatalog.defaultSound.id
    @AppStorage("use24HourClock") private var use24HourClock = false
    @State private var expandedSegmentIDs: Set<UUID> = []

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
                BellSelectionControl(title: "Default bell", bell: $day.defaultBell)
            }
            .listSectionSeparator(.hidden)
            Section(header:
                VStack(alignment: .leading) {
                    Text("Segments").fontWeight(.bold)
                    // TODO add a help button using SF Symbol questionmark.circle with instructions for editing
                }
            ) {
                ForEach(Array(day.segments.enumerated()), id: \.element.id) { index, segment in
                    let binding = $day.segments[index]
                    let schedule = segmentScheduleMap[segment.id]
                    let resolvedBell = segment.resolvedEndBell(defaultBell: day.defaultBell)

                    let expanded = expandedSegmentIDs.contains(segment.id)

                    VStack(spacing: 0) {
                        Button(action: {
                            toggleExpanded(id: segment.id)
                        }) {
                            SegmentCardView(segment: segment,
                                            startTime: schedule?.0 ?? day.startTimeAsDate,
                                            endTime: schedule?.1 ?? day.startTimeAsDate,
                                            theme: day.theme,
                                            bell: resolvedBell,
                                            useTheme: expanded,
                                            highlighted: expanded)
                                .contentShape(Rectangle())
                        }
                        .buttonStyle(.plain)

                        if expanded {
                            Divider()
                                .padding(.horizontal)
                            SegmentEditorRow(
                                segment: binding,
                                startTime: schedule?.0 ?? day.startTimeAsDate,
                                endTime: schedule?.1 ?? day.startTimeAsDate,
                                use24HourClock: use24HourClock,
                                defaultBell: day.defaultBell,
                                onDuplicate: { duplicateSegment(at: index) }
                            )
                            .padding(.horizontal)
                            .padding(.bottom, 12)
                        }
                    }
                    .background(
                        RoundedRectangle(cornerRadius: 12)
                            .fill(Color(uiColor: .secondarySystemBackground))
                    )
                    .overlay(
                        RoundedRectangle(cornerRadius: 12)
                            .stroke(expanded ? day.theme.mainColor : Color.clear, lineWidth: expanded ? 2 : 0)
                    )
                    .padding(.vertical, 4)
                }
                .onDelete { indices in
                    let ids = indices.compactMap { day.segments[$0].id }
                    day.segments.remove(atOffsets: indices)
                    ids.forEach { expandedSegmentIDs.remove($0) }
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
                    Toggle("Use day default bell", isOn: $newSegmentUsesDefaultBell)
                        .toggleStyle(.switch)
                    if !newSegmentUsesDefaultBell {
                        Picker("Bell sound", selection: $newSegmentSoundId) {
                            ForEach(BellCatalog.sounds) { sound in
                                Text(sound.displayName).tag(sound.id)
                            }
                        }
                        .pickerStyle(.menu)
                        Stepper(value: $newSegmentChimes, in: 1...12) {
                            Label("Chimes: \(newSegmentChimes)", systemImage: "bell")
                                .accessibilityValue("\(newSegmentChimes)")
                        }
                    }
                    Button(action: {
                        withAnimation {
                            let duration = TimeInterval(60 * 60 * newSegmentHours + 60 * newSegmentMinutes)
                            let customBell: Bell? = newSegmentUsesDefaultBell ? nil : Bell(soundId: newSegmentSoundId, numRings: newSegmentChimes)
                            var segment = Day.Segment(name: newSegmentName, duration: duration, customEndBell: customBell)
                            day.segments.append(segment)
                            expandedSegmentIDs.insert(segment.id)
                            newSegmentName = ""
                            newSegmentHours = 0
                            newSegmentMinutes = 0
                            newSegmentUsesDefaultBell = true
                            newSegmentSoundId = day.defaultBell.soundId
                            newSegmentChimes = day.defaultBell.numRings
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
        .onAppear {
            expandedSegmentIDs = Set(day.segments.map(\.id))
            newSegmentSoundId = day.defaultBell.soundId
            newSegmentChimes = day.defaultBell.numRings
        }
        .onChange(of: day.segments.map(\.id)) { ids in
            let idSet = Set(ids)
            expandedSegmentIDs = expandedSegmentIDs.intersection(idSet)
        }
        .onChange(of: day.defaultBell) { newBell in
            day.manualBell = newBell
            if newSegmentUsesDefaultBell {
                newSegmentSoundId = newBell.soundId
                newSegmentChimes = newBell.numRings
            }
        }
        .onChange(of: newSegmentUsesDefaultBell) { useDefault in
            newSegmentSoundId = day.defaultBell.soundId
            newSegmentChimes = day.defaultBell.numRings
        }
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

    private func toggleExpanded(id: UUID) {
        if expandedSegmentIDs.contains(id) {
            expandedSegmentIDs.remove(id)
        } else {
            expandedSegmentIDs.insert(id)
        }
    }

    private func duplicateSegment(at index: Int) {
        guard index < day.segments.count else { return }
        var duplicate = day.segments[index]
        duplicate.id = UUID()
        day.segments.insert(duplicate, at: index + 1)
        expandedSegmentIDs.insert(duplicate.id)
    }
}

private struct SegmentEditorRow: View {
    @Binding var segment: Day.Segment
    let startTime: Date
    let endTime: Date
    let use24HourClock: Bool
    let defaultBell: Bell
    let onDuplicate: () -> Void

    private var durationMinutesBinding: Binding<Int> {
        Binding(
            get: { DayDetailEditView.minutes(from: segment.duration) },
            set: { segment.duration = DayDetailEditView.timeInterval(fromMinutes: $0) }
        )
    }

    private var usesDefaultBellBinding: Binding<Bool> {
        Binding(
            get: { segment.customEndBell == nil },
            set: { newValue in
                if newValue {
                    segment.useDefaultBell()
                } else {
                    if segment.customEndBell == nil {
                        segment.setCustomEndBell(defaultBell)
                    }
                }
            }
        )
    }

    private var customBellBinding: Binding<Bell> {
        Binding(
            get: { segment.customEndBell ?? defaultBell },
            set: { segment.setCustomEndBell($0) }
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
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

            Toggle("Use day default bell", isOn: usesDefaultBellBinding)
                .toggleStyle(.switch)

            if !usesDefaultBellBinding.wrappedValue {
                BellSelectionControl(title: "Bell", bell: customBellBinding)
            } else {
                Text("Using default bell (\(defaultBell.sound.displayName), \(defaultBell.numRings) chimes)")
                    .font(.caption)
                    .foregroundColor(.secondary)
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

            Spacer()

            Button(action: onDuplicate) {
                Label("Duplicate", systemImage: "square.on.square")
                    .labelStyle(.titleAndIcon)
            }
            .buttonStyle(.bordered)
            .accessibilityLabel("Duplicate segment")
        }
    }

    private func adjustDuration(by delta: Int) {
        let current = durationMinutesBinding.wrappedValue
        let updated = DayDetailEditView.clampedMinutes(current + delta)
        durationMinutesBinding.wrappedValue = updated
    }
}

private struct BellSelectionControl: View {
    let title: String
    @Binding var bell: Bell

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Picker(title, selection: $bell.soundId) {
                ForEach(BellCatalog.sounds) { sound in
                    Text(sound.displayName).tag(sound.id)
                }
            }
            .pickerStyle(.menu)

            Stepper(value: $bell.numRings, in: 1...12) {
                Label("Chimes: \(bell.numRings)", systemImage: "bell")
                    .accessibilityLabel("Number of chimes")
                    .accessibilityValue("\(bell.numRings)")
            }
        }
        .padding(.vertical, 4)
    }
}

struct DayDetailEditView_Previews: PreviewProvider {
    static var previews: some View {
        DayDetailEditView(day: .constant(Day.fullDay))
    }
}
