//
//  ThemeView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/12/23.
//

import SwiftUI

enum UIStyle {
    static let screenPadding: CGFloat = 16
    static let cardPadding: CGFloat = 14
    static let cardCorner: CGFloat = 12
    static let sectionSpacing: CGFloat = 14
    static let timelineRailWidth: CGFloat = 2
}

struct ThemeView: View {
    let theme: Theme

    var body: some View {
        Text(theme.name)
            .padding(4)
            .frame(maxWidth: .infinity)
            .background(theme.mainColor)
            .foregroundColor(theme.accentColor)
            .clipShape(RoundedRectangle(cornerRadius: 4))
    }
}

struct ThemeView_Previews: PreviewProvider {
    static var previews: some View {
        ThemeView(theme: .buttercup)
    }
}
