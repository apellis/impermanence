import Foundation

struct BellSound: Identifiable, Hashable {
    let id: Int
    let name: String
    let resourcePrefix: String

    var displayName: String { name }
    var resourceCandidates: [String] {
        ["\(resourcePrefix)_\(id)", "\(resourcePrefix)-\(id)", "\(resourcePrefix)\(id)", resourcePrefix]
    }
}

enum BellCatalog {
    static let sounds: [BellSound] = [
        BellSound(id: 0, name: "Classic Bell", resourcePrefix: "bell")
    ]

    static var defaultSound: BellSound { sounds.first! }

    static func sound(for id: Int) -> BellSound {
        sounds.first { $0.id == id } ?? defaultSound
    }
}
