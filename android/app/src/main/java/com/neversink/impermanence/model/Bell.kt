package com.neversink.impermanence.model

import kotlinx.serialization.Serializable

@Serializable
data class Bell(
    val soundId: Int,
    val numRings: Int
) {
    val sound: BellSound
        get() = BellCatalog.soundFor(soundId)

    /** Mirrors iOS `Bell.sanitized()`: a known sound id and 1...12 rings. */
    fun sanitized(): Bell {
        val validSoundId = if (BellCatalog.all.any { it.id == soundId }) soundId else SingleBell.soundId
        val validRings = numRings.coerceIn(MIN_RINGS, MAX_RINGS)
        return if (validSoundId == soundId && validRings == numRings) this else Bell(validSoundId, validRings)
    }

    companion object {
        const val MIN_RINGS = 1
        const val MAX_RINGS = 12

        val SingleBell = Bell(soundId = 0, numRings = 1)
        val RepeatedBell = Bell(soundId = 0, numRings = 3)

        fun bellFor(sound: BellSound, rings: Int): Bell = Bell(soundId = sound.id, numRings = rings).sanitized()
    }
}
