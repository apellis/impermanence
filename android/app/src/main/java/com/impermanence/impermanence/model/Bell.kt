package com.impermanence.impermanence.model

import kotlinx.serialization.Serializable

@Serializable
data class Bell(
    val soundId: Int,
    val numRings: Int
) {
    val sound: BellSound
        get() = BellCatalog.soundFor(soundId)

    companion object {
        val SingleBell = Bell(soundId = 0, numRings = 1)
        val RepeatedBell = Bell(soundId = 0, numRings = 3)

        fun bellFor(sound: BellSound, rings: Int): Bell = Bell(soundId = sound.id, numRings = rings)
    }
}
