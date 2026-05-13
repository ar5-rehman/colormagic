package com.colormagic.kids.presentation.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

// Bold near-black display heading used by every "title screen" on the brand
// (Home greeting, Onboarding hero copy, sub-flow headers). Colour can be
// overridden for celebratory headings (e.g. SaveSuccess uses brand primary).
@Composable
fun BrandHeading(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 30.sp,
    lineHeight: TextUnit = 38.sp,
    textAlign: TextAlign = TextAlign.Start,
    color: Color = BrandTokens.HeadingInk
) {
    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = lineHeight,
        fontWeight = FontWeight.Bold,
        color = color,
        textAlign = textAlign,
        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
    )
}
