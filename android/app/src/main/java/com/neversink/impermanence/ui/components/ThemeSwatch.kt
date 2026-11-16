package com.neversink.impermanence.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.neversink.impermanence.model.Theme

/** A small square in the theme's color, shown next to a theme's name. */
@Composable
fun ThemeSwatch(theme: Theme, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .background(theme.mainColor, RoundedCornerShape(4.dp))
            .border(width = 1.dp, color = MaterialTheme.colorScheme.outline, shape = RoundedCornerShape(4.dp))
    )
}
