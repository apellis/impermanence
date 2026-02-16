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
    @State private var pendingSaveTask: Task<Void, Never>?

    var body: some Scene {
        WindowGroup {
            DaysView(days: $store.days, saveAction: saveDays)
                .task {
                    await loadDays()
                }
                .onReceive(store.$days.dropFirst()) { _ in
                    scheduleSaveDays()
                }
                .onDisappear {
                    pendingSaveTask?.cancel()
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
        let snapshot = store.days
        Task {
            do {
                try await store.save(days: snapshot)
            } catch {
                persistenceErrorMessage = "Failed to save changes. \(error.localizedDescription)"
            }
        }
    }

    private func scheduleSaveDays() {
        pendingSaveTask?.cancel()
        pendingSaveTask = Task {
            try? await Task.sleep(nanoseconds: 300_000_000)
            guard !Task.isCancelled else { return }
            saveDays()
        }
    }
}
