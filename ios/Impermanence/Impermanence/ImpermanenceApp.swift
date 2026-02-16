//
//  ImpermanenceApp.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import SwiftUI

@main
struct ImpermanenceApp : App {
    @StateObject private var store = DayStore()
    @State private var persistenceErrorMessage: String?

    var body: some Scene {
        WindowGroup {
            DaysView(days: $store.days, saveAction: saveDays)
                .task {
                    await loadDays()
                }
                .alert("Unable to Access Saved Data",
                       isPresented: isShowingPersistenceError,
                       actions: {
                           Button("OK", role: .cancel) {
                               persistenceErrorMessage = nil
                           }
                       },
                       message: {
                           Text(persistenceErrorMessage ?? "An unknown error occurred.")
                       })
        }
    }

    private var isShowingPersistenceError: Binding<Bool> {
        Binding(
            get: { persistenceErrorMessage != nil },
            set: { isPresented in
                if !isPresented {
                    persistenceErrorMessage = nil
                }
            }
        )
    }

    private func loadDays() async {
        do {
            try await store.load()
        } catch {
            persistenceErrorMessage = "Failed to load saved days. \(error.localizedDescription)"
        }
    }

    private func saveDays() {
        Task {
            do {
                try await store.save(days: store.days)
            } catch {
                persistenceErrorMessage = "Failed to save changes. \(error.localizedDescription)"
            }
        }
    }
}
