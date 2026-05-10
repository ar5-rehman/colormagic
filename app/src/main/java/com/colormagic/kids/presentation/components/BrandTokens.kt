package com.colormagic.kids.presentation.components

import androidx.compose.ui.graphics.Color

// Palette tokens that aren't part of the Material color scheme but recur
// across screens. Keep these here so individual screens never hard-code them.
object BrandTokens {
    val HeadingInk = Color(0xFF101012)
    val MutedInk = Color(0xFF6F6E76)
    val PrimaryEdge = Color(0xFF6F4FB0)        // tactile back face for primary surfaces
    val SubtleSurface = Color(0xFFF1EFF4)      // grey pill / chip background
    val SubtleOutline = Color(0xFFE3E1E6)      // hairline borders on cards
}
