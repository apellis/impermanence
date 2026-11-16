//
//  Theme.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import Foundation
#if canImport(SwiftUI)
import SwiftUI
#endif

enum Theme : String, CaseIterable, Identifiable, Codable {
    case bubblegum
    case buttercup
    case indigo
    case lavender
    case magenta
    case navy
    case orange
    case oxblood
    case periwinkle
    case poppy
    case purple
    case seafoam
    case sky
    case tan
    case teal
    case yellow

#if canImport(SwiftUI)
    var accentColor: Color {
        switch self {
        case .bubblegum, .buttercup, .lavender, .orange, .periwinkle, .poppy, .seafoam, .sky, .tan, .teal, .yellow: return .black
        case .indigo, .magenta, .navy, .oxblood, .purple: return .white
        }
    }

    var mainColor: Color {
        Color(rawValue)
    }
#endif

    var name: String {
        rawValue.capitalized
    }

    var id: String {
        name
    }
}
