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
    @State private var pendingDelete: Day?
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
                    // Ask first, as deleting a day can't be undone.
                    pendingDelete = indices.first.map { days[$0] }
                }
                .onMove { from, to in
                    days.move(fromOffsets: from, toOffset: to)
                }
            }
            .overlay {
                if days.isEmpty {
                    Text("No days yet. Tap New Day to begin.")
                        .foregroundColor(.secondary)
                        .multilineTextAlignment(.center)
                        .padding()
                }
            }
            .confirmationDialog(pendingDelete.map { "Delete \u{201C}\($0.name)\u{201D}?" } ?? "",
                                isPresented: Binding(get: { pendingDelete != nil },
                                                     set: { if !$0 { pendingDelete = nil } }),
                                titleVisibility: .visible,
                                presenting: pendingDelete) { day in
                Button("Delete", role: .destructive) {
                    days.removeAll { $0.id == day.id }
                    pendingDelete = nil
                }
                Button("Cancel", role: .cancel) { pendingDelete = nil }
            } message: { _ in
                Text("This day and its segments will be removed.")
            }
            .navigationTitle("Days")
            .navigationDestination(for: UUID.self) { id in
                if let index = days.firstIndex(where: { $0.id == id }) {
                    DayDetailView(day: $days[index])
                }
            }
            .toolbar {
                ToolbarItem(placement: .navigationBarLeading) {
                    Button(action: {
                        isPresentingSettingsView = true
                    }) {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("Settings")
                }
                ToolbarItemGroup(placement: .navigationBarTrailing) {
                    Button("Quick Sit") {
                        isPresentingQuickSit = true
                    }
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
