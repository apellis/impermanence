//
//  SegmentCardView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import SwiftUI

struct SegmentCardView: View {
    let segment: Day.Segment
    let startTime: Date
    let endTime: Date
    let theme: Theme
    let useTheme: Bool
    let highlighted: Bool
    @AppStorage("use24HourClock") private var use24HourClock = false

    init(segment: Day.Segment, startTime: Date, endTime: Date, theme: Theme, useTheme: Bool = false, highlighted: Bool = false) {
        self.segment = segment
        self.startTime = startTime
        self.endTime = endTime
        self.theme = theme
        self.useTheme = useTheme
        self.highlighted = highlighted
    }

    var body: some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(segment.name)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
                .frame(maxWidth: .infinity, alignment: .leading)

            timeColumn
                .frame(maxWidth: .infinity, alignment: .leading)

            bellColumn
                .frame(maxWidth: .infinity, alignment: .trailing)
        }
        .padding()
        .if(self.useTheme) { $0.background(theme.mainColor) }
        .if(self.useTheme) { $0.foregroundColor(theme.accentColor) }
        .if(self.useTheme && self.highlighted) {
            $0.overlay(
                RoundedRectangle(cornerRadius: 10)
                    .strokeBorder(theme.accentColor, lineWidth: 1)
            )
        }
    }

    private var timeRangeText: String {
        let start = TimeFormatting.formattedTime(from: startTime, use24HourClock: use24HourClock)
        let end = TimeFormatting.formattedTime(from: endTime, use24HourClock: use24HourClock)
        return "\(start) – \(end)"
    }

    private var timeColumn: some View {
        HStack(spacing: 4) {
            Image(systemName: "clock")
            Text(timeRangeText)
                .font(.caption)
                .monospacedDigit()
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("duration \(segment.duration)")
    }

    private var bellColumn: some View {
        HStack(spacing: 4) {
            Text("\(segment.endBell.numRings)")
                .font(.caption)
            Image(systemName: "bell")
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("bell rings \(segment.endBell.numRings)")
    }
}

extension View {
    @ViewBuilder
    func `if`<Transform: View>(_ condition: Bool, transform: (Self) -> Transform) -> some View {
        if condition {
            transform(self)
        } else {
            self
        }
    }
}

struct SegmentCardView_Previews: PreviewProvider {
    static var segment = Day.Segment(name: "Sit", duration: TimeInterval(45 * 60), endBell: Bell.singleBell)
    static var previews: some View {
        SegmentCardView(segment: segment, startTime: Date(), endTime: Date().addingTimeInterval(segment.duration), theme: .sky)
            .previewLayout(.fixed(width: 400, height: 60))
    }
}
