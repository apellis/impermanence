//
//  ImpermanenceWidgets.swift
//  ImpermanenceWidgets
//
//  The Live Activity shown on the Lock Screen and in the Dynamic Island while
//  a day or Quick Sit is being timed. The countdowns tick on their own
//  (Text(timerInterval:)), so the app only updates it when a segment changes.
//

import ActivityKit
import SwiftUI
import WidgetKit

@main
struct ImpermanenceWidgets: WidgetBundle {
    var body: some Widget {
        TimingLiveActivity()
    }
}

struct TimingLiveActivity: Widget {
    var body: some WidgetConfiguration {
        ActivityConfiguration(for: TimingActivityAttributes.self) { context in
            LockScreenTimingView(name: context.attributes.name, state: context.state)
                .activityBackgroundTint(Color(.systemBackground).opacity(0.85))
                .activitySystemActionForegroundColor(.primary)
        } dynamicIsland: { context in
            DynamicIsland {
                DynamicIslandExpandedRegion(.leading) {
                    Label(context.state.title, systemImage: "bell")
                        .font(.headline)
                        .lineLimit(1)
                }
                DynamicIslandExpandedRegion(.trailing) {
                    Countdown(state: context.state)
                        .font(.title3.weight(.semibold))
                }
                DynamicIslandExpandedRegion(.bottom) {
                    HStack {
                        Text(context.attributes.name)
                        Spacer()
                        if let detail = context.state.detail {
                            Text(detail)
                        } else {
                            Text(context.state.countdownLabel)
                        }
                    }
                    .font(.caption)
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                }
            } compactLeading: {
                Image(systemName: "bell")
            } compactTrailing: {
                Countdown(state: context.state)
                    .frame(maxWidth: 56)
            } minimal: {
                Image(systemName: "bell")
            }
        }
    }
}

/// A countdown to `state.end` that the system keeps ticking.
private struct Countdown: View {
    let state: TimingActivityAttributes.ContentState

    var body: some View {
        Text(timerInterval: min(state.start, state.end)...state.end, countsDown: true)
            .monospacedDigit()
            .multilineTextAlignment(.trailing)
    }
}

private struct LockScreenTimingView: View {
    let name: String
    let state: TimingActivityAttributes.ContentState

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack {
                Label(name, systemImage: "bell")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(.secondary)
                Spacer()
                if let detail = state.detail {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }
            .lineLimit(1)
            HStack(alignment: .firstTextBaseline) {
                Text(state.title)
                    .font(.title3.weight(.semibold))
                    .lineLimit(1)
                Spacer()
                VStack(alignment: .trailing, spacing: 0) {
                    Text(state.countdownLabel)
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                    Countdown(state: state)
                        .font(.title2.weight(.semibold))
                }
            }
            ProgressView(timerInterval: min(state.start, state.end)...state.end, countsDown: false) {
                EmptyView()
            } currentValueLabel: {
                EmptyView()
            }
            .tint(.primary)
        }
        .padding(16)
        .accessibilityElement(children: .combine)
    }
}
