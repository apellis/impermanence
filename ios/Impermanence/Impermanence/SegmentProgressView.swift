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

    private var progress: Double {
        guard timeElapsed + timeRemaining > 0 else { return 1 }
        return Double(timeElapsed) / Double(timeElapsed + timeRemaining)
    }

    var body: some View {
        VStack {
            ProgressView(value: progress)
                .progressViewStyle(.linear)
                .tint(theme.mainColor)
                .padding(4)
                .background(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(theme.accentColor.opacity(0.35))
                )
                .transaction { transaction in
                    transaction.animation = nil
                }
            HStack {
                VStack(alignment: .leading) {
                    Text("Elapsed")
                        .font(.caption)
                    HStack(spacing: 4) {
                        Image(systemName: "hourglass.bottomhalf.fill")
                        Text(safeString(for: timeElapsed))
                            .monospacedDigit()
                    }
                }
                Spacer()
                VStack(alignment: .trailing) {
                    Text("Remaining")
                        .font(.caption)
                    HStack(spacing: 4) {
                        Text(safeString(for: timeRemaining))
                            .monospacedDigit()
                        Image(systemName: "hourglass.tophalf.fill")
                    }
                }
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Time remaining")
        .accessibilityValue("\(timeRemaining)")
        .padding([.top, .horizontal])
    }

    private func safeString(for interval: TimeInterval) -> String {
        TimeFormatting.formattedDuration(from: interval)
    }
}

struct SegmentProgressView_Previews: PreviewProvider {
    static var previews: some View {
        SegmentProgressView(timeElapsed: TimeInterval(180), timeRemaining: TimeInterval(420), theme: Theme.sky)
    }
}
