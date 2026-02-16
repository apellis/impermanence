//
//  AboutView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/15/23.
//

import SwiftUI

struct AboutView: View {
    @Binding var isPresentingAboutView: Bool
    let appVersion: String = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "(unknown!)"

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text("Impermanence")
                    Text("Version \(appVersion)")
                    Text("For the benefit of all beings ☸️")
                }
                Section("Copyright") {
                    Text("© 2026 Alex Ellis")
                    Text("Contact: apellis@gmail.com")
                }
                Section("Future features") {
                    Text("More bell sound options")
                    Text("Multi-day retreat plans")
                }
            }
            .navigationTitle("About")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Close") {
                        isPresentingAboutView = false
                    }
                    .accessibilityLabel("Close")
                }
            }
        }
    }
}

struct AboutView_Previews: PreviewProvider {
    static var previews: some View {
        AboutView(isPresentingAboutView: .constant(true))
    }
}
