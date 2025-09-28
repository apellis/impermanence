package com.impermanence.impermanence.model

object BellCatalog {
    private val sounds: List<BellSound> = listOf(
        BellSound(id = 0, name = "Classic Bell", resourcePrefix = "bell")
    )

    val defaultSound: BellSound
        get() = sounds.first()

    fun soundFor(id: Int): BellSound = sounds.firstOrNull { it.id == id } ?: defaultSound

    val all: List<BellSound>
        get() = sounds
}
