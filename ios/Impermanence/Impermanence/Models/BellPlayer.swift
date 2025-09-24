//
//  BellPlayer.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/16/23.
//

import Foundation
import AVFoundation

final class BellPlayer {
    static let shared = BellPlayer()

    private var audioPlayer: AVAudioPlayer?

    private init() {}

    func play(bell: Bell) {
        guard let url = resolveURL(for: bell) else {
            assertionFailure("Failed to find sound file for bell id \(bell.soundId)")
            return
        }

        do {
            audioPlayer = try AVAudioPlayer(contentsOf: url)
            audioPlayer?.numberOfLoops = max(bell.numRings - 1, 0)
            audioPlayer?.prepareToPlay()
            audioPlayer?.play()
        } catch {
            assertionFailure("Failed to play bell: \(error.localizedDescription)")
        }
    }

    private func resolveURL(for bell: Bell) -> URL? {
        let resourceCandidates = [
            "bell_\(bell.soundId)",
            "bell-\(bell.soundId)",
            "bell\(bell.soundId)",
            "bell"
        ]

        for name in resourceCandidates {
            if let url = Bundle.main.url(forResource: name, withExtension: "mp3") {
                return url
            }
        }

        return nil
    }
}
