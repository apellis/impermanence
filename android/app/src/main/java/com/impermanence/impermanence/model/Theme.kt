package com.impermanence.impermanence.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class Theme {
    @SerialName("bubblegum") BUBBLEGUM,
    @SerialName("buttercup") BUTTERCUP,
    @SerialName("indigo") INDIGO,
    @SerialName("lavender") LAVENDER,
    @SerialName("magenta") MAGENTA,
    @SerialName("navy") NAVY,
    @SerialName("orange") ORANGE,
    @SerialName("oxblood") OXBLOOD,
    @SerialName("periwinkle") PERIWINKLE,
    @SerialName("poppy") POPPY,
    @SerialName("purple") PURPLE,
    @SerialName("seafoam") SEAFOAM,
    @SerialName("sky") SKY,
    @SerialName("tan") TAN,
    @SerialName("teal") TEAL,
    @SerialName("yellow") YELLOW;

    val displayName: String
        get() = name.lowercase().replaceFirstChar { it.titlecase() }

    val mainColor: Color
        get() = when (this) {
            BUBBLEGUM -> Color(0xFFED80D1)
            BUTTERCUP -> Color(0xFFFFF095)
            INDIGO -> Color(0xFF360070)
            LAVENDER -> Color(0xFFCFCEFF)
            MAGENTA -> Color(0xFFA41377)
            NAVY -> Color(0xFF001341)
            ORANGE -> Color(0xFFFF8A42)
            OXBLOOD -> Color(0xFF49060A)
            PERIWINKLE -> Color(0xFF8582FF)
            POPPY -> Color(0xFFFF5E5E)
            PURPLE -> Color(0xFF914AF1)
            SEAFOAM -> Color(0xFFCAEAE4)
            SKY -> Color(0xFF6D92FF)
            TAN -> Color(0xFFC29B7D)
            TEAL -> Color(0xFF218F9E)
            YELLOW -> Color(0xFFFFDF4D)
        }

    val accentColor: Color
        get() = when (this) {
            BUBBLEGUM, BUTTERCUP, LAVENDER, ORANGE, PERIWINKLE, POPPY, SEAFOAM, SKY, TAN, TEAL, YELLOW -> Color(0xFF000000)
            INDIGO, MAGENTA, NAVY, OXBLOOD, PURPLE -> Color(0xFFFFFFFF)
        }
}
