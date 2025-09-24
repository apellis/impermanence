//
//  DayDetailView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI

struct DayDetailView: View {
    @Binding var day: Day

    let calendar: Calendar = Calendar.current
    @AppStorage("use24HourClock") private var use24HourClock = false

    @State private var editingDay = Day.emptyDay
    @State private var isPresentingEditView = false

    init(day: Binding<Day>) {
        self._day = day
    }

    var body: some View {
        List {
            Section(header: Text("Day Info")) {
                NavigationLink(destination: DayActiveView(day: $day)) {
                    Label("Start or Resume Day", systemImage: "play.circle")
                        .font(.headline)
                        .foregroundColor(.accentColor)
                }
                .listRowSeparator(.hidden)
                Label("\(TimeFormatting.formattedTime(from: dayStartTime, use24HourClock: use24HourClock))–\(TimeFormatting.formattedTime(from: dayEndTime, use24HourClock: use24HourClock))", systemImage: "clock")
                    .listRowSeparator(.hidden)
                HStack {
                    Label("Theme", systemImage: "paintpalette")
                    Spacer()
                    Text(day.theme.name)
                        .padding(4)
                        .foregroundColor(day.theme.accentColor)
                        .background(day.theme.mainColor)
                        .cornerRadius(4)
                }
                .accessibilityElement(children: .combine)
                .listRowSeparator(.hidden)
            }
            .listSectionSeparator(.hidden)
            Section(header: Text("Segments")) {
                ForEach(day.segments.indices, id: \.self) { i in
                    let segment = day.segments[i]
                    let start = day.segmentStartEndTimes[i].0
                    let end = day.segmentStartEndTimes[i].1
                    SegmentCardView(segment: segment,
                                    startTime: start,
                                    endTime: end,
                                    theme: day.theme,
                                    bell: segment.resolvedEndBell(defaultBell: day.defaultBell))
                        .listRowSeparator(.hidden)
                }
            }
            .listSectionSeparator(.hidden)
        }
        .navigationTitle(day.name)
        .listStyle(.plain)
        .listRowSeparator(.hidden, edges: .all)
        .listSectionSeparator(.hidden, edges: .all)
        .scrollContentBackground(.hidden)
        .toolbar {
            Button("Edit") {
                isPresentingEditView = true
                editingDay = day
            }
        }
        .sheet(isPresented: $isPresentingEditView) {
            NavigationStack {
                DayDetailEditView(day: $editingDay)
                    .navigationTitle(day.name)
                    .toolbar {
                        ToolbarItem(placement: .cancellationAction) {
                            Button("Cancel") {
                                isPresentingEditView = false
                            }
                        }
                        ToolbarItem(placement: .confirmationAction) {
                            Button("Done") {
                                isPresentingEditView = false
                                day = editingDay
                            }
                        }
                    }
            }
        }
    }

    private var dayStartTime: Date {
        calendar.startOfDay(for: Date.now)
            .addingTimeInterval(day.startTime)
    }

    private var dayEndTime: Date {
        calendar.startOfDay(for: Date.now)
            .addingTimeInterval(day.startEndTimeIntervals.1)
    }
}

struct DayDetailView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack {
            DayDetailView(day: .constant(Day.fullDay))
        }
    }
}
