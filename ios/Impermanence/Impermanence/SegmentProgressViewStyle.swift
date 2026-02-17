//
//  SegmentProgressViewStyle.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/14/23.
//

import SwiftUI

struct SegmentProgressViewStyle: ProgressViewStyle {
    var theme: Theme

    func makeBody(configuration: Configuration) -> some View {
        let fraction = min(max(configuration.fractionCompleted ?? 0, 0), 1)
        return GeometryReader { proxy in
            ZStack(alignment: .leading) {
                RoundedRectangle(cornerRadius: 10.0)
                    .fill(theme.accentColor.opacity(0.35))

                RoundedRectangle(cornerRadius: 8.0)
                    .fill(theme.mainColor)
                    .frame(width: proxy.size.width * fraction)
                    .padding(.horizontal, 4)
                    .padding(.vertical, 4)
            }
        }
        .frame(height: 20.0)
    }
}

struct SegmentProgressViewStyle_Previews: PreviewProvider {
    static var previews: some View {
        ProgressView(value: 0.4)
            .progressViewStyle(SegmentProgressViewStyle(theme: .buttercup))
            .previewLayout(.sizeThatFits)
    }
}
