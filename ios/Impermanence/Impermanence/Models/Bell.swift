//
//  Bell.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/5/23.
//

import Foundation

struct Bell: Codable, Equatable {
    var soundId: Int
    var numRings: Int

    static let minRings = 1
    static let maxRings = 12

    var sound: BellSound {
        BellCatalog.sound(for: soundId)
    }
}

extension Bell {
    static let singleBell: Bell = Bell(soundId: 0, numRings: 1)
    static let repeatedBell: Bell = Bell(soundId: 0, numRings: 3)

    static func bell(for sound: BellSound, rings: Int) -> Bell {
        Bell(soundId: sound.id, numRings: rings).sanitized()
    }

    func sanitized() -> Bell {
        let validSoundId = BellCatalog.sounds.contains(where: { $0.id == soundId }) ? soundId : Bell.singleBell.soundId
        let validRings = min(max(numRings, Bell.minRings), Bell.maxRings)
        return Bell(soundId: validSoundId, numRings: validRings)
    }
}
