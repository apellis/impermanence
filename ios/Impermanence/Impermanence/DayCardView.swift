//
//  DayCardView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import SwiftUI

struct DayCardView: View {
    let day: Day

    @AppStorage("use24HourClock") private var use24HourClock = false

    init(day: Day) {
        self.day = day
    }

    var body: some View {
        VStack(alignment: .leading) {
            Text(day.name)
                .font(.headline)
                .accessibilityAddTraits(.isHeader)
            Spacer()
            HStack {
                if let rangeText = timeRangeText {
                    Label(rangeText, systemImage: "clock")
                        .accessibilityLabel("duration \(day.segments.count)")
                        .labelStyle(.trailingIcon)
                }
                Spacer()
            }
            .font(.caption)
        }
        .padding()
        .foregroundColor(day.theme.accentColor)
    }

    private var timeRangeText: String? {
        guard let first = day.segmentStartEndTimes.first,
              let last = day.segmentStartEndTimes.last else {
            return nil
        }
        let start = TimeFormatting.formattedTime(from: first.0, use24HourClock: use24HourClock)
        let end = TimeFormatting.formattedTime(from: last.1, use24HourClock: use24HourClock)
        return "\(start) – \(end)"
    }
}

struct DayCardView_Previews: PreviewProvider {
    static var day = Day.fullDay
    static var previews: some View {
        DayCardView(day: day)
            .background(day.theme.mainColor)
            .previewLayout(.fixed(width: 400, height: 60))
    }
}
