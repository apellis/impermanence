package com.impermanence.impermanence.model

import kotlinx.serialization.Serializable

@Serializable
data class BellSound(
    val id: Int,
    val name: String,
    val resourcePrefix: String
) {
    val resourceCandidates: List<String>
        get() = listOf(
            "${resourcePrefix}_${id}",
            "${resourcePrefix}-${id}",
            "${resourcePrefix}${id}",
            resourcePrefix
        )
}
