import Foundation

struct BellSound: Identifiable, Hashable {
    let id: Int
    let name: String
    /// Bundled sound file, Core Audio Format so that it can also be used as
    /// a notification sound (iOS notification sounds must be aiff/wav/caf,
    /// under 30 seconds).
    let fileName: String

    var displayName: String { name }

    /// The sound a backup notification plays for `chimes` strikes 3 s apart
    /// (1...ChimePlanner.chimesPerNotification), mixed into one file of at
    /// most 30 s by scripts/make_bell_sounds.py.
    func notificationFileName(chimes: Int) -> String {
        guard chimes > 1 else { return fileName }
        let base = (fileName as NSString).deletingPathExtension
        let ext = (fileName as NSString).pathExtension
        return "\(base)_x\(min(chimes, ChimePlanner.chimesPerNotification)).\(ext)"
    }

    var url: URL? {
        let base = (fileName as NSString).deletingPathExtension
        let ext = (fileName as NSString).pathExtension
        return Bundle.main.url(forResource: base, withExtension: ext)
    }
}

enum BellCatalog {
    static let sounds: [BellSound] = [
        BellSound(id: 0, name: "Classic Bell", fileName: "bell_0.caf")
    ]

    static var defaultSound: BellSound { sounds.first! }

    static func sound(for id: Int) -> BellSound {
        sounds.first { $0.id == id } ?? defaultSound
    }
}
