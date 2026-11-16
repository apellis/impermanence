//
//  AboutView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/15/23.
//
//  Shown from Settings.
//

import SwiftUI

struct AboutView: View {
    private let privacyPolicyUrl = URL(string: "https://github.com/apellis/impermanence/blob/master/PRIVACY.md")!

    static var versionText: String {
        let info = Bundle.main.infoDictionary
        let version = info?["CFBundleShortVersionString"] as? String
        let build = info?["CFBundleVersion"] as? String
        switch (version, build) {
        case let (v?, b?) where v != b: return "\(v) (\(b))"
        case let (v?, _): return v
        default: return "—"
        }
    }

    var body: some View {
        Form {
            Section {
                LabeledContent("Version", value: Self.versionText)
                Text("For the benefit of all beings ☸️")
            }
            Section {
                Text("© 2026 Alex Ellis")
                Link("Privacy Policy", destination: privacyPolicyUrl)
            }
        }
        .navigationTitle("About Impermanence")
        .navigationBarTitleDisplayMode(.inline)
    }
}

struct AboutView_Previews: PreviewProvider {
    static var previews: some View {
        NavigationStack { AboutView() }
    }
}
