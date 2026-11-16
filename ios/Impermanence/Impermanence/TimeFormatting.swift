import Foundation

enum TimeFormatting {
    private static let twelveHourFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.amSymbol = "AM"
        formatter.pmSymbol = "PM"
        formatter.dateFormat = "h:mm a"
        return formatter
    }()

    private static let twentyFourHourFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm"
        return formatter
    }()

    private static let twelveHourSecondsFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.amSymbol = "AM"
        formatter.pmSymbol = "PM"
        formatter.dateFormat = "h:mm:ss a"
        return formatter
    }()

    private static let twentyFourHourSecondsFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "HH:mm:ss"
        return formatter
    }()

    /// `h:mm AM` or `HH:mm`, with seconds added only when a time isn't on a
    /// whole minute (segments can be any number of seconds long).
    static func formattedTime(from date: Date, use24HourClock: Bool) -> String {
        let hasSeconds = Calendar.current.component(.second, from: date) != 0
        let formatter: DateFormatter
        switch (use24HourClock, hasSeconds) {
        case (true, false): formatter = twentyFourHourFormatter
        case (true, true): formatter = twentyFourHourSecondsFormatter
        case (false, false): formatter = twelveHourFormatter
        case (false, true): formatter = twelveHourSecondsFormatter
        }
        formatter.timeZone = .current
        return formatter.string(from: date)
    }

    static func formattedDuration(from interval: TimeInterval) -> String {
        let totalSeconds = max(0, Int(interval.rounded(.down)))
        let hours = totalSeconds / 3600
        let minutes = (totalSeconds % 3600) / 60
        let seconds = totalSeconds % 60
        return String(format: "%02d:%02d:%02d", hours, minutes, seconds)
    }
}
