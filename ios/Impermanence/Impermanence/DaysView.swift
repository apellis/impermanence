//
//  DaysView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import SwiftUI
import Combine

struct DaysView: View {
    @Binding var days: [Day]
    @Environment(\.scenePhase) private var scenePhase
    @State private var isPresentingSettingsView = false
    @State private var isPresentingAboutView = false
    @State private var isPresentingNewDayView = false
    @State private var isPresentingQuickSit = false
    @State private var path: [UUID] = []
    @ObservedObject private var engine = BellEngine.shared
    let saveAction: ()->Void

    var body: some View {
        NavigationStack(path: $path) {
            List {
                ForEach(days) { day in
                    NavigationLink(value: day.id) {
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
            .navigationDestination(for: UUID.self) { id in
                if let index = days.firstIndex(where: { $0.id == id }) {
                    DayDetailView(day: $days[index])
                }
            }
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
                        isPresentingQuickSit = true
                    }) {
                        Image(systemName: "stopwatch")
                    }
                    .accessibilityLabel("Quick Sit")
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
                SettingsView(days: $days, isPresentingSettingsView: $isPresentingSettingsView)
            }
            .sheet(isPresented: $isPresentingAboutView) {
                AboutView(isPresentingAboutView: $isPresentingAboutView)
            }
            .sheet(isPresented: $isPresentingQuickSit) {
                QuickSitSheet(isPresented: $isPresentingQuickSit)
            }
            .onChange(of: scenePhase) { phase in
                if phase != .active { saveAction() }
            }
            .onReceive(engine.$resumedSession.compactMap { $0 }) { resumed in
                // iOS ended the app while a day or Quick Sit was being timed:
                // go back to that screen, where the bells already resumed.
                engine.resumedSession = nil
                switch resumed {
                case .day(let s):
                    path = [s.dayID]
                case .quickSit:
                    path = []
                    isPresentingQuickSit = true
                }
            }
        }
        .overlay(alignment: .bottomLeading) { BellEngineDebugStatus() }
    }
}

/// Visible only in UI tests: lets them check the bell engine's state.
struct BellEngineDebugStatus: View {
    @ObservedObject private var engine = BellEngine.shared

    var body: some View {
        if BellEngine.isUITesting {
            Text(engine.debugStatus)
                .font(.caption2)
                .padding(4)
                .accessibilityIdentifier("bellEngineStatus")
        }
    }
}


struct DaysView_Previews: PreviewProvider {
    static var previews: some View {
        DaysView(days: .constant([Day.openingDay, Day.fullDay]), saveAction: {})
    }
}
