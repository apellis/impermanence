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
}

extension Bell {
    static let singleBell: Bell = Bell(soundId: 0, numRings: 1)
    static let repeatedBell: Bell = Bell(soundId: 0, numRings: 3)
}
