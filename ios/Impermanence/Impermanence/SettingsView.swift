//
//  SettingsView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/15/23.
//

import SwiftUI

struct SettingsView: View {
    @Binding var isPresentingSettingsView: Bool

    @AppStorage("use24HourClock") private var use24HourClock = false
    @AppStorage("loopDays") private var loopDays = true

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Toggle(isOn: $use24HourClock) {
                        Text("Use 24-hour clock")
                    }
                    Toggle(isOn: $loopDays) {
                        Text("Loop days at midnight")
                    }
                }
            }
            .navigationTitle("Settings")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(action: {
                        isPresentingSettingsView = false
                    }) {
                        Image(systemName: "arrow.left")
                    }
                }
            }
        }
    }
}

struct SettingsView_Previews: PreviewProvider {
    static var previews: some View {
        SettingsView(isPresentingSettingsView: .constant(true))
    }
}
