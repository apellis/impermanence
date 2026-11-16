//
//  TimingActivity.swift
//  Impermanence
//
//  What the Lock Screen and Dynamic Island show while a day or Quick Sit is
//  being timed (a Live Activity). Shared by the app, which starts, updates
//  and ends it from BellEngine, and the ImpermanenceWidgets extension, which
//  draws it. Display only: bells never depend on it.
//

#if canImport(ActivityKit)
import ActivityKit
import Foundation

struct TimingActivityAttributes: ActivityAttributes {
    struct ContentState: Codable, Hashable {
        /// The current segment, "Day queued", or "Quick Sit".
        var title: String
        /// "Next bell", "Starts in" or "Time remaining".
        var countdownLabel: String
        /// The countdown runs from `start` to `end` (the next bell).
        var start: Date
        var end: Date
        /// For example "Next bell 7:45 AM"; nil for Quick Sit.
        var detail: String?
    }

    /// The day's name, or "Quick Sit".
    var name: String
    /// Identifies the session, so a new session gets a new activity.
    var sessionKey: String
}
#endif
