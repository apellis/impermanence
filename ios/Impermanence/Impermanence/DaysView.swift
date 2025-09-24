//
//  DaysView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import SwiftUI

struct DaysView: View {
    @Binding var days: [Day]
    @Environment(\.scenePhase) private var scenePhase
    @State private var isPresentingSettingsView = false
    @State private var isPresentingAboutView = false
    @State private var isPresentingNewDayView = false
    let saveAction: ()->Void

    var body: some View {
        NavigationStack {
            List {
                ForEach($days) { $day in
                    NavigationLink(destination: DayDetailView(day: $day)) {
                        DayCardView(day: day)
                    }
                    .listRowBackground(day.theme.mainColor)
                }
                .onDelete { indices in
                    days.remove(atOffsets: indices)
                }
                .onMove { from, to in
                    days.move(fromOffsets: from, toOffset: to)
                }
            }
            .navigationTitle("Days")
            .toolbar {
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button(action: {
                        isPresentingAboutView = true
                    }) {
                        Image(systemName: "info.circle")
                    }
                    .accessibilityLabel("About")
                    Button(action: {
                        isPresentingSettingsView = true
                    }) {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("Settings")
                    Button(action: {
                        isPresentingNewDayView = true
                    }) {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("New Day")
                }
            }
            .sheet(isPresented: $isPresentingNewDayView) {
                NewDaySheet(days: $days, isPresentingNewDayView: $isPresentingNewDayView)
            }
            .sheet(isPresented: $isPresentingSettingsView) {
                SettingsView(isPresentingSettingsView: $isPresentingSettingsView)
            }
            .sheet(isPresented: $isPresentingAboutView) {
                AboutView(isPresentingAboutView: $isPresentingAboutView)
            }
            .onChange(of: scenePhase) { phase in
                if phase == .inactive { saveAction() }
            }
        }
    }
}

struct DaysView_Previews: PreviewProvider {
    static var previews: some View {
        DaysView(days: .constant([Day.openingDay, Day.fullDay]), saveAction: {})
    }
}
