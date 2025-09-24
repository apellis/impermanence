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

    static func formattedTime(from date: Date, use24HourClock: Bool) -> String {
        let formatter = use24HourClock ? twentyFourHourFormatter : twelveHourFormatter
        return formatter.string(from: date)
    }
}
