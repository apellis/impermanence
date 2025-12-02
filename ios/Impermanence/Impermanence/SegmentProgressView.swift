//
//  SegmentProgressView.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/14/23.
//

import SwiftUI

struct SegmentProgressView: View {
    let timeElapsed: TimeInterval
    let timeRemaining: TimeInterval
    let theme: Theme
    var formatter = DateComponentsFormatter()

    init(timeElapsed: TimeInterval, timeRemaining: TimeInterval, theme: Theme) {
        self.timeElapsed = timeElapsed
        self.timeRemaining = timeRemaining
        self.theme = theme

        formatter.allowedUnits = [.hour, .minute, .second]
        formatter.zeroFormattingBehavior = .pad
    }

    private var progress: Double {
        guard timeElapsed + timeRemaining > 0 else { return 1 }
        return Double(timeElapsed) / Double(timeElapsed + timeRemaining)
    }

    var body: some View {
        VStack {
            ProgressView(value: progress)
                .progressViewStyle(SegmentProgressViewStyle(theme: theme))
            HStack {
                VStack(alignment: .leading) {
                    Text("Elapsed")
                        .font(.caption)
                    Label("\(safeString(for: timeElapsed))", systemImage: "hourglass.bottomhalf.fill")
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Remaining")
                        .font(.caption)
                    Label("\(safeString(for: timeRemaining))", systemImage: "hourglass.tophalf.fill")
                        .labelStyle(.trailingIcon)
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Time remaining")
        .accessibilityValue("\(timeRemaining)")
        .padding([.top, .horizontal])
    }

    private func safeString(for interval: TimeInterval) -> String {
        formatter.string(from: max(0, interval)) ?? "--:--"
    }
}

struct SegmentProgressView_Previews: PreviewProvider {
    static var previews: some View {
        SegmentProgressView(timeElapsed: TimeInterval(180), timeRemaining: TimeInterval(420), theme: Theme.sky)
    }
}
