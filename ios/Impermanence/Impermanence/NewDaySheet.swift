//
//  NewDaySheet.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/16/23.
//

import SwiftUI

struct NewDaySheet: View {
    @Binding var days: [Day]
    @Binding var isPresentingNewDayView: Bool

    @State private var newDay = Day.emptyDay
    @State private var expandedSegmentIDs: Set<UUID> = []
    @AppStorage("use24HourClock") private var use24HourClock = false

    var body: some View {
        NavigationStack {
            Form {
                dayInfoSection
                segmentsSection
            }
            .navigationTitle("New Day")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Dismiss") {
                        isPresentingNewDayView = false
                    }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Add") {
                        days.append(newDay)
                        isPresentingNewDayView = false
                    }
                    .disabled(newDay.name.trimmingCharacters(in: .whitespaces).isEmpty || newDay.segments.isEmpty)
                }
            }
            .onAppear {
                expandedSegmentIDs = Set(newDay.segments.map(\.id))
            }
            .onChange(of: newDay.segments.map(\.id)) { ids in
                let valid = Set(ids)
                expandedSegmentIDs = expandedSegmentIDs.intersection(valid)
            }
            .onChange(of: newDay.defaultBell) { newBell in
                newDay.startBell = newBell
                newDay.manualBell = newBell
            }
        }
    }

    private var dayInfoSection: some View {
        Section(header: Text("Day Info")) {
            TextField("Name", text: $newDay.name)
            DatePicker("Start of Day", selection: $newDay.startTimeAsDate, displayedComponents: .hourAndMinute)
            ThemePicker(selection: $newDay.theme)
            BellSelectionControl(title: "Default bell", bell: $newDay.defaultBell)
        }
    }

    private var segmentsSection: some View {
        Section(header: Text("Segments")) {
            let scheduleMap = DayDetailEditView.segmentSchedules(for: newDay)

            if newDay.segments.isEmpty {
                Text("No segments yet. Tap Add Segment to begin.")
                    .font(.footnote)
                    .foregroundColor(.secondary)
                    .padding(.vertical, 4)
            }

            ForEach(Array(newDay.segments.enumerated()), id: \.element.id) { index, segment in
                let binding = $newDay.segments[index]
                let schedule = scheduleMap[segment.id]
                let isExpanded = expandedSegmentIDs.contains(segment.id)

                VStack(spacing: 0) {
                    Button(action: { toggleExpanded(id: segment.id) }) {
                        SegmentCardView(
                            segment: segment,
                            startTime: schedule?.0 ?? newDay.startTimeAsDate,
                            endTime: schedule?.1 ?? newDay.startTimeAsDate,
                            theme: newDay.theme,
                            bell: segment.resolvedEndBell(defaultBell: newDay.defaultBell),
                            useTheme: isExpanded,
                            highlighted: isExpanded
                        )
                        .contentShape(Rectangle())
                    }
                    .buttonStyle(.plain)

                    if isExpanded {
                        Divider().padding(.horizontal)
                        DaySegmentEditorRow(
                            segment: binding,
                            startTime: schedule?.0 ?? newDay.startTimeAsDate,
                            endTime: schedule?.1 ?? newDay.startTimeAsDate,
                            use24HourClock: use24HourClock,
                            defaultBell: newDay.defaultBell,
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
                        .stroke(isExpanded ? newDay.theme.mainColor : Color.clear, lineWidth: isExpanded ? 2 : 0)
                )
                .padding(.vertical, 4)
            }
            .onDelete { indices in
                let removed = indices.compactMap { newDay.segments[$0].id }
                newDay.segments.remove(atOffsets: indices)
                removed.forEach { expandedSegmentIDs.remove($0) }
            }
            .onMove { from, to in
                newDay.segments.move(fromOffsets: from, toOffset: to)
            }

            Button(action: addSegment) {
                Label("Add Segment", systemImage: "plus")
                    .frame(maxWidth: .infinity)
            }
            .buttonStyle(.borderedProminent)
            .tint(newDay.theme.mainColor)
        }
    }

    private func addSegment() {
        withAnimation {
            let segment = Day.Segment(name: "", duration: DayDetailEditView.timeInterval(fromMinutes: 15), customEndBell: nil)
            newDay.segments.append(segment)
            expandedSegmentIDs.insert(segment.id)
        }
    }

    private func toggleExpanded(id: UUID) {
        withAnimation {
            if expandedSegmentIDs.contains(id) {
                expandedSegmentIDs.remove(id)
            } else {
                expandedSegmentIDs.insert(id)
            }
        }
    }

    private func duplicateSegment(at index: Int) {
        guard index < newDay.segments.count else { return }
        withAnimation {
            var duplicate = newDay.segments[index]
            duplicate.id = UUID()
            newDay.segments.insert(duplicate, at: index + 1)
            expandedSegmentIDs.insert(duplicate.id)
        }
    }
}

struct NewDaySheet_Previews: PreviewProvider {
    static var previews: some View {
        NewDaySheet(days: .constant([Day.openingDay, Day.fullDay]), isPresentingNewDayView: .constant(true))
    }
}
