//
//  DayDetailEditView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//
//  The one day editor, used by both New Day and Edit: day info, then a
//  compact list of segments. Tapping a segment opens its own editor.
//

import SwiftUI
import UIKit

struct DayDetailEditView: View {
    @Binding var day: Day
    @AppStorage("use24HourClock") private var use24HourClock = false

    var body: some View {
        // One schedule per render, shared by the rows and the footer.
        let schedules = Self.segmentSchedules(for: day)
        Form {
            Section(header: Text("Day Info")) {
                TextField("Name", text: $day.name)
                DatePicker("Start of Day", selection: $day.startTimeAsDate, displayedComponents: .hourAndMinute)
                ThemePicker(selection: $day.theme)
                BellSelectionControl(title: "Default bell", bell: $day.defaultBell)
            }

            Section {
                if day.segments.isEmpty {
                    Text("No segments yet. Add one to begin.")
                        .font(.footnote)
                        .foregroundColor(.secondary)
                }
                // editActions: .move lets a row be dragged straight away
                // (touch and hold, then drag), with no Edit mode needed.
                ForEach($day.segments, editActions: .move) { $segment in
                    NavigationLink {
                        SegmentEditorView(day: $day, segmentID: segment.id)
                    } label: {
                        SegmentRow(segment: segment,
                                   schedule: schedules[segment.id],
                                   bell: segment.resolvedEndBell(defaultBell: day.defaultBell),
                                   use24HourClock: use24HourClock)
                    }
                    .accessibilityIdentifier("segmentRow")
                    .swipeActions(edge: .trailing) {
                        if day.segments.count > 1 {
                            Button(role: .destructive) {
                                deleteSegment(id: segment.id)
                            } label: {
                                Label("Delete", systemImage: "trash")
                            }
                        }
                    }
                    .swipeActions(edge: .leading) {
                        Button {
                            duplicateSegment(id: segment.id)
                        } label: {
                            Label("Duplicate", systemImage: "plus.square.on.square")
                        }
                        .tint(.indigo)
                    }
                    .accessibilityAction(named: "Move up") { moveSegment(id: segment.id, by: -1) }
                    .accessibilityAction(named: "Move down") { moveSegment(id: segment.id, by: 1) }
                    .accessibilityAction(named: "Duplicate") { duplicateSegment(id: segment.id) }
                }
                .onDelete { indices in
                    // In Edit mode. A day keeps at least one segment.
                    guard day.segments.count - indices.count >= 1 else { return }
                    day.segments.remove(atOffsets: indices)
                }
                .deleteDisabled(day.segments.count <= 1)

                Button(action: addSegment) {
                    Label("Add Segment", systemImage: "plus")
                }
            } header: {
                HStack {
                    Text("Segments")
                    Spacer()
                    if day.segments.count > 1 {
                        EditButton()
                            .font(.footnote)
                            .textCase(nil)
                    }
                }
            } footer: {
                if let first = day.segments.first.flatMap({ schedules[$0.id] }),
                   let last = day.segments.last.flatMap({ schedules[$0.id] }) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(TimeFormatting.formattedTime(from: first.0, use24HourClock: use24HourClock)) – \(TimeFormatting.formattedTime(from: last.1, use24HourClock: use24HourClock)), \(SegmentDuration.shortText(last.1.timeIntervalSince(first.0)))")
                        if day.segments.count > 1 {
                            Text("Touch and hold a segment to drag it. Swipe for Duplicate and Delete.")
                        }
                    }
                }
            }
        }
        .onChange(of: day.defaultBell) { newBell in
            day.startBell = newBell
            day.manualBell = newBell
        }
    }

    private func moveSegment(id: UUID, by offset: Int) {
        guard let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        let target = index + offset
        guard day.segments.indices.contains(target) else { return }
        day.segments.swapAt(index, target)
        let name = day.segments[target].name.isEmpty ? "Segment" : day.segments[target].name
        UIAccessibility.post(notification: .announcement,
                             argument: "\(name), position \(target + 1) of \(day.segments.count)")
    }

    private func deleteSegment(id: UUID) {
        guard day.segments.count > 1, let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        withAnimation {
            _ = day.segments.remove(at: index)
        }
    }

    private func addSegment() {
        withAnimation {
            day.segments.append(Day.Segment(name: "", duration: 15 * 60, customEndBell: nil))
        }
    }

    private func duplicateSegment(id: UUID) {
        guard let index = day.segments.firstIndex(where: { $0.id == id }) else { return }
        var duplicate = day.segments[index]
        duplicate.id = UUID()
        withAnimation {
            day.segments.insert(duplicate, at: index + 1)
        }
    }
}

extension DayDetailEditView {
    static func segmentSchedules(for day: Day) -> [UUID: (Date, Date)] {
        var lookup: [UUID: (Date, Date)] = [:]
        for (segment, schedule) in zip(day.segments, day.segmentStartEndTimes) {
            lookup[segment.id] = schedule
        }
        return lookup
    }

    /// Whether a day may be saved: it needs a name and at least one segment
    /// (docs/BEHAVIOR.md, Editing).
    static func canSave(_ day: Day) -> Bool {
        !day.name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && !day.segments.isEmpty
    }
}

/// One line per segment: name, duration, times and bell.
private struct SegmentRow: View {
    let segment: Day.Segment
    let schedule: (Date, Date)?
    let bell: Bell
    let use24HourClock: Bool

    var body: some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack {
                Text(segment.name.isEmpty ? "Untitled segment" : segment.name)
                    .font(.body)
                Spacer()
                Text(SegmentDuration.shortText(segment.duration))
                    .monospacedDigit()
                    .foregroundColor(.secondary)
            }
            HStack(spacing: 8) {
                if let schedule {
                    Text("\(TimeFormatting.formattedTime(from: schedule.0, use24HourClock: use24HourClock)) – \(TimeFormatting.formattedTime(from: schedule.1, use24HourClock: use24HourClock))")
                        .monospacedDigit()
                }
                Label("\(bell.numRings)", systemImage: "bell")
                    .labelStyle(.titleAndIcon)
                    .accessibilityLabel("\(bell.numRings) chimes at the end")
            }
            .font(.caption)
            .foregroundColor(.secondary)
        }
        .accessibilityElement(children: .combine)
    }
}

/// Edits one segment of `day`: name, duration in minutes and seconds, and
/// its end bell.
struct SegmentEditorView: View {
    @Binding var day: Day
    let segmentID: UUID
    @Environment(\.dismiss) private var dismiss
    @AppStorage("use24HourClock") private var use24HourClock = false

    private var index: Int? { day.segments.firstIndex { $0.id == segmentID } }

    private var segment: Binding<Day.Segment> {
        Binding(
            get: { index.map { day.segments[$0] } ?? Day.Segment(name: "", duration: 60) },
            set: { updated in
                if let index { day.segments[index] = updated }
            }
        )
    }

    private var minutes: Binding<Int> {
        Binding(
            get: { SegmentDuration.parts(of: segment.wrappedValue.duration).minutes },
            set: { segment.wrappedValue.duration = SegmentDuration.interval(minutes: $0, seconds: SegmentDuration.parts(of: segment.wrappedValue.duration).seconds) }
        )
    }

    private var seconds: Binding<Int> {
        Binding(
            get: { SegmentDuration.parts(of: segment.wrappedValue.duration).seconds },
            set: { segment.wrappedValue.duration = SegmentDuration.interval(minutes: SegmentDuration.parts(of: segment.wrappedValue.duration).minutes, seconds: min(max($0, 0), 59)) }
        )
    }

    private var usesDefaultBell: Binding<Bool> {
        Binding(
            get: { segment.wrappedValue.customEndBell == nil },
            set: { useDefault in
                if useDefault {
                    segment.wrappedValue.useDefaultBell()
                } else if segment.wrappedValue.customEndBell == nil {
                    segment.wrappedValue.setCustomEndBell(day.defaultBell)
                }
            }
        )
    }

    private var customBell: Binding<Bell> {
        Binding(
            get: { segment.wrappedValue.customEndBell ?? day.defaultBell },
            set: { segment.wrappedValue.setCustomEndBell($0) }
        )
    }

    var body: some View {
        Form {
            Section {
                TextField("Name", text: segment.name)
                    .textInputAutocapitalization(.words)
                if let index, let schedule = DayDetailEditView.segmentSchedules(for: day)[segmentID] {
                    LabeledContent("Time",
                                   value: "\(TimeFormatting.formattedTime(from: schedule.0, use24HourClock: use24HourClock)) – \(TimeFormatting.formattedTime(from: schedule.1, use24HourClock: use24HourClock))")
                    LabeledContent("Segment", value: "\(index + 1) of \(day.segments.count)")
                }
            }

            Section {
                HStack(spacing: 8) {
                    NumberField(title: "Minutes", value: minutes, upper: SegmentDuration.maxSeconds / 60)
                    Text("min").foregroundColor(.secondary)
                    NumberField(title: "Seconds", value: seconds, upper: 59)
                    Text("sec").foregroundColor(.secondary)
                    Spacer()
                }
                HStack(spacing: 10) {
                    adjust("-5 min", by: -5, label: "Subtract five minutes")
                    adjust("-1", by: -1, label: "Subtract one minute")
                    adjust("+1", by: 1, label: "Add one minute")
                    adjust("+5 min", by: 5, label: "Add five minutes")
                }
            } header: {
                Text("Duration")
            } footer: {
                Text("1 second to 24 hours. \(SegmentDuration.shortText(segment.wrappedValue.duration)).")
            }

            Section(header: Text("End bell")) {
                Toggle("Use day default bell", isOn: usesDefaultBell)
                if usesDefaultBell.wrappedValue {
                    Text("\(day.defaultBell.sound.displayName), \(day.defaultBell.numRings) \(day.defaultBell.numRings == 1 ? "chime" : "chimes")")
                        .foregroundColor(.secondary)
                } else {
                    BellSelectionControl(title: "Bell", bell: customBell)
                }
            }

            Section {
                Button("Duplicate Segment") {
                    guard let index else { return }
                    var copy = day.segments[index]
                    copy.id = UUID()
                    day.segments.insert(copy, at: index + 1)
                    dismiss()
                }
                Button("Delete Segment", role: .destructive) {
                    guard let index, day.segments.count > 1 else { return }
                    dismiss()
                    day.segments.remove(at: index)
                }
                .disabled(day.segments.count <= 1)
            } footer: {
                if day.segments.count <= 1 {
                    Text("A day needs at least one segment.")
                }
            }
        }
        .navigationTitle(segment.wrappedValue.name.isEmpty ? "Segment" : segment.wrappedValue.name)
        .navigationBarTitleDisplayMode(.inline)
    }

    private func adjust(_ title: String, by delta: Int, label: String) -> some View {
        Button(title) {
            let current = SegmentDuration.seconds(of: segment.wrappedValue.duration)
            segment.wrappedValue.duration = TimeInterval(SegmentDuration.clamped(current + delta * 60))
        }
        .buttonStyle(.bordered)
        .controlSize(.small)
        .lineLimit(1)
        .accessibilityLabel(label)
    }
}

/// A number-pad field over an Int. Emptying it means 0, so a new number can
/// be typed without selecting the old one first.
private struct NumberField: View {
    let title: String
    @Binding var value: Int
    let upper: Int

    var body: some View {
        TextField(title, text: Binding(
            get: { String(value) },
            set: { newValue in
                let digits = String(newValue.filter(\.isNumber).prefix(5))
                value = min(Int(digits) ?? 0, upper)
            }
        ))
        .keyboardType(.numberPad)
        .textFieldStyle(.roundedBorder)
        .multilineTextAlignment(.trailing)
        .frame(minWidth: 56, maxWidth: 88)
        .accessibilityLabel(title)
    }
}

struct BellSelectionControl: View {
    let title: String
    @Binding var bell: Bell

    var body: some View {
        // With a single sound there is nothing to choose.
        if BellCatalog.sounds.count > 1 {
            Picker(title, selection: $bell.soundId) {
                ForEach(BellCatalog.sounds) { sound in
                    Text(sound.displayName).tag(sound.id)
                }
            }
            .pickerStyle(.menu)
        } else {
            LabeledContent(title, value: bell.sound.displayName)
        }
        Stepper(value: $bell.numRings, in: 1...12) {
            Label("Chimes: \(bell.numRings)", systemImage: "bell")
        }
        .accessibilityLabel("Number of chimes")
        .accessibilityValue("\(bell.numRings)")
    }
}

struct DayDetailEditView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DayDetailEditView(day: .constant(Day.fullDay))
        }
    }
}
