//
//  BellPlayer.swift
//  Impermanence
//
//  Created by Alex Ellis on 8/16/23.
//

import Foundation
import AVFoundation

final class BellPlayer: NSObject, AVAudioPlayerDelegate {
    static let shared = BellPlayer()

    private var activePlayers: [AVAudioPlayer] = []
    private let schedulingQueue = DispatchQueue(label: "com.neversink.impermanence.bellplayer")

    private override init() {
        super.init()
    }

    func play(bell: Bell) {
        let sanitizedBell = bell.sanitized()
        let rings = sanitizedBell.numRings
        for index in 0..<rings {
            let delay = TimeInterval(index) * 3.0
            schedulingQueue.asyncAfter(deadline: .now() + delay) { [weak self] in
                self?.playSingleBell(sanitizedBell)
            }
        }
    }

    private func playSingleBell(_ bell: Bell) {
        guard let url = resolveURL(for: bell) else {
            assertionFailure("Failed to find sound file for bell id \(bell.soundId)")
            return
        }

        do {
            let player = try AVAudioPlayer(contentsOf: url)
            player.delegate = self
            player.prepareToPlay()

            DispatchQueue.main.async { [weak self] in
                guard let self else { return }
                self.activePlayers.append(player)
                player.play()
            }
        } catch {
            assertionFailure("Failed to play bell: \(error.localizedDescription)")
        }
    }

    private func resolveURL(for bell: Bell) -> URL? {
        for name in bell.sound.resourceCandidates {
            if let url = Bundle.main.url(forResource: name, withExtension: "mp3") {
                return url
            }
        }

        return nil
    }
    func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        DispatchQueue.main.async { [weak self] in
            self?.activePlayers.removeAll { $0 === player }
        }
    }
}
