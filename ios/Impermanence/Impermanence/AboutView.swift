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
            VStack{
                Text("Impermanece")
                Text("Version \(appVersion)")
                Text("For the benefit of all beings ☸️")
                Divider()
                    .padding()
                Text("Copyright 2025, Neversink LLC")
                Text("Contact: apellis@gmail.com")
                Divider()
                    .padding()
                Text("Future features")
                Text("""
                    * More bell sound options
                    * Multi-day retreat plans
                    """)
                    .padding()
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button(action: {
                            isPresentingAboutView = false
                        }) {
                            Image(systemName: "arrow.left")
                        }
                    }
                }
                .navigationTitle("About")
            }
        }
    }
}

struct AboutView_Previews: PreviewProvider {
    static var previews: some View {
        AboutView(isPresentingAboutView: .constant(true))
    }
}
