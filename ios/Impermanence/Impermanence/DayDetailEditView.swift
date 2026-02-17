//
//  DayDetailEditView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI

struct DayDetailEditView: View {
    @Binding var day: Day
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
                BellSelectionControl(title: "Default bell", bell: $day.defaultBell)
            }
            .listSectionSeparator(.hidden)
            Section(header:
                VStack(alignment: .leading) {
                    Text("Segments").fontWeight(.bold)
                    Text("Timeline style editor with inline controls.")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            ) {
                timelineSummaryRow
                ForEach(day.segments, id: \.id) { segment in
                    let binding = safeBinding(for: segment.id)
                    let schedule = segmentScheduleMap[segment.id]
                    HStack(alignment: .top, spacing: 10) {
                        timelineRail(
                            start: schedule?.0 ?? day.startTimeAsDate,
                            end: schedule?.1 ?? day.startTimeAsDate
                        )
                        DaySegmentEditorRow(
                            segment: binding,
                            totalSegments: day.segments.count,
                            startTime: schedule?.0 ?? day.startTimeAsDate,
                            endTime: schedule?.1 ?? day.startTimeAsDate,
                            use24HourClock: use24HourClock,
                            defaultBell: day.defaultBell,
                            canMoveUp: segment.id != day.segments.first?.id,
                            canMoveDown: segment.id != day.segments.last?.id,
                            onMoveUp: { moveSegment(id: segment.id, offset: -1) },
                            onMoveDown: { moveSegment(id: segment.id, offset: 1) },
                            onDuplicate: { duplicateSegment(id: segment.id) },
                            onDelete: { deleteSegment(id: segment.id) }
                        )
                        .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    .padding(12)
                    .background(
                        RoundedRectangle(cornerRadius: UIStyle.cardCorner)
                            .fill(Color(uiColor: .secondarySystemBackground))
                    )
                    .listRowInsets(EdgeInsets(top: 4, leading: UIStyle.screenPadding, bottom: 4, trailing: UIStyle.screenPadding))
                }
                .onDelete { indices in
                    day.segments.remove(atOffsets: indices)
                }
                .onMove { from, to in
                    day.segments.move(fromOffsets: from, toOffset: to)
                }

                Button(action: addSegment) {
                    Label("Add Segment", systemImage: "plus")
                        .frame(maxWidth: .infinity)
                }
                .buttonStyle(.borderedProminent)
                .tint(day.theme.mainColor)
            }
            .listSectionSeparator(.hidden)
            .listRowSeparator(.hidden)
        }
        .scrollContentBackground(.hidden)
        .onChange(of: day.defaultBell) { newBell in
            day.startBell = newBell
            day.manualBell = newBell
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

    @ViewBuilder
    private var timelineSummaryRow: some View {
        let start = day.segmentStartEndTimes.first?.0 ?? day.startTimeAsDate
        let end = day.segmentStartEndTimes.last?.1 ?? day.startTimeAsDate
        let totalMinutes = max(0, Int((end.timeIntervalSince(start) / 60).rounded()))
        VStack(alignment: .leading, spacing: 4) {
            Text("Schedule Timeline")
                .font(.headline)
            Text("\(TimeFormatting.formattedTime(from: start, use24HourClock: use24HourClock)) – \(TimeFormatting.formattedTime(from: end, use24HourClock: use24HourClock))")
                .font(.subheadline)
            Text("Total duration: \(formattedDuration(totalMinutes))")
                .font(.caption)
                .foregroundColor(.secondary)
        }
        .padding(.vertical, 4)
    }

    @ViewBuilder
    private func timelineRail(start: Date, end: Date) -> some View {
        VStack(spacing: 6) {
            Text(TimeFormatting.formattedTime(from: start, use24HourClock: use24HourClock))
                .font(.caption2)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
            Spacer()
                .frame(height: 10)
            RoundedRectangle(cornerRadius: 1)
                .fill(Color.secondary.opacity(0.3))
                .frame(width: UIStyle.timelineRailWidth, height: 62)
            Text(TimeFormatting.formattedTime(from: end, use24HourClock: use24HourClock))
                .font(.caption2)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.8)
        }
        .frame(width: 78)
    }

    private func duplicateSegment(id: UUID) {
        guard let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        var duplicate = day.segments[index]
        duplicate.id = UUID()
        day.segments.insert(duplicate, at: index + 1)
    }

    private func deleteSegment(id: UUID) {
        guard let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        day.segments.remove(at: index)
    }

    private func moveSegment(id: UUID, offset: Int) {
        guard let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        let target = index + offset
        guard target >= 0 && target < day.segments.count else { return }
        withAnimation(.easeInOut(duration: 0.18)) {
            let segment = day.segments.remove(at: index)
            day.segments.insert(segment, at: target)
        }
    }

    private func addSegment() {
        withAnimation {
            let segment = Day.Segment(name: "", duration: DayDetailEditView.timeInterval(fromMinutes: 15), customEndBell: nil)
            day.segments.append(segment)
        }
    }

    private func safeBinding(for id: UUID) -> Binding<Day.Segment> {
        Binding(
            get: {
                day.segments.first(where: { $0.id == id }) ?? Day.Segment(name: "", duration: 0, customEndBell: nil)
            },
            set: { updated in
                if let index = day.segments.firstIndex(where: { $0.id == id }) {
                    day.segments[index] = updated
                }
            }
        )
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
}

struct DaySegmentEditorRow: View {
    @Binding var segment: Day.Segment
    let totalSegments: Int
    let startTime: Date
    let endTime: Date
    let use24HourClock: Bool
    let defaultBell: Bell
    let canMoveUp: Bool
    let canMoveDown: Bool
    let onMoveUp: () -> Void
    let onMoveDown: () -> Void
    let onDuplicate: () -> Void
    let onDelete: () -> Void

    private var durationMinutesBinding: Binding<Int> {
        Binding(
            get: { DayDetailEditView.minutes(from: segment.duration) },
            set: { segment.duration = DayDetailEditView.timeInterval(fromMinutes: $0) }
        )
    }

    private var durationTextBinding: Binding<String> {
        Binding(
            get: { String(durationMinutesBinding.wrappedValue) },
            set: { newValue in
                let digits = newValue.filter(\.isNumber)
                guard let parsed = Int(digits), !digits.isEmpty else { return }
                durationMinutesBinding.wrappedValue = DayDetailEditView.clampedMinutes(parsed)
            }
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
            HStack(spacing: 6) {
                Button(action: onMoveUp) {
                    Image(systemName: "arrow.up")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .disabled(!canMoveUp)
                .accessibilityLabel("Move segment up")

                Button(action: onMoveDown) {
                    Image(systemName: "arrow.down")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .disabled(!canMoveDown)
                .accessibilityLabel("Move segment down")

                Button(action: onDuplicate) {
                    Image(systemName: "square.on.square")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .accessibilityLabel("Duplicate segment")

                Button(role: .destructive, action: onDelete) {
                    Image(systemName: "trash")
                }
                .buttonStyle(.bordered)
                .controlSize(.small)
                .accessibilityLabel("Delete segment")
                .disabled(totalSegments <= 1)
            }

            TextField("Name", text: $segment.name)
                .textFieldStyle(.roundedBorder)
                .textInputAutocapitalization(.words)
                .disableAutocorrection(false)

            Text("Time: \(timeRangeText)")
                .font(.subheadline)
                .foregroundColor(.secondary)
                .monospacedDigit()

            VStack(alignment: .leading, spacing: 8) {
                Text("Duration")
                    .font(.subheadline)
                HStack(spacing: 8) {
                    Text("Minutes")
                        .font(.caption)
                        .foregroundColor(.secondary)
                    TextField("Minutes", text: durationTextBinding)
                        .textFieldStyle(.roundedBorder)
                        .keyboardType(.numberPad)
                        .frame(width: 110)
                    Text(formattedDuration(durationMinutesBinding.wrappedValue))
                        .font(.caption)
                        .foregroundColor(.secondary)
                    Spacer()
                }
                durationAdjustRow
            }

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

    private var durationAdjustRow: some View {
        HStack(spacing: 12) {
            Button {
                adjustDuration(by: -1)
            } label: {
                Image(systemName: "minus")
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .accessibilityLabel("Subtract one minute")

            Button {
                adjustDuration(by: 1)
            } label: {
                Image(systemName: "plus")
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .accessibilityLabel("Add one minute")

            Button {
                adjustDuration(by: -5)
            } label: {
                Text("-5 min")
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .lineLimit(1)
            .accessibilityLabel("Subtract five minutes")

            Button {
                adjustDuration(by: 5)
            } label: {
                Text("+5 min")
            }
            .buttonStyle(.bordered)
            .controlSize(.small)
            .lineLimit(1)
            .accessibilityLabel("Add five minutes")
            Spacer()
        }
    }

    private func adjustDuration(by delta: Int) {
        let current = durationMinutesBinding.wrappedValue
        let updated = DayDetailEditView.clampedMinutes(current + delta)
        durationMinutesBinding.wrappedValue = updated
    }
}

struct BellSelectionControl: View {
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
