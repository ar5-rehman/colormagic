package com.colormagic.kids.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Calendar

/**
 * A Duolingo-style 7-day flame strip (Mon→Sun of the current week).
 *
 * A day shows a 🔥 when it's part of the current streak; days before the streak
 * started are empty, days later this week (the future) are dimmed. Today gets a
 * highlight ring. The pattern is derived purely from [streak] + today's weekday,
 * so no per-day history is needed: the streak always ends "today" (Home records
 * the open), so the last [streak] days up to today are the flames.
 */
/** Colour set for the strip, so it reads on both a light card and a warm
 *  (orange) card. Defaults suit a light/white background. */
data class StreakStripColors(
    val active: Color = Color(0xFFFF7043),      // vibrant coral-orange
    val empty: Color = Color(0xFFF3ECE7),       // warm neutral off-white
    val future: Color = Color(0xFFF8F4F0),      // very faint warm
    val letter: Color = Color(0xFF4E342E),      // clean warm-dark
    val futureLetter: Color = Color(0xFFBCAAA4), // muted warm
    val ring: Color = Color(0xFFE64A19)         // deep orange accent ring
)

@Composable
fun StreakWeekStrip(
    streak: Int,
    modifier: Modifier = Modifier,
    colors: StreakStripColors = StreakStripColors()
) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    // Calendar (API 1+) instead of java.time.LocalDate (API 26+). DAY_OF_WEEK
    // is 1=Sun..7=Sat; map to Mon=0 … Sun=6 so the strip starts on Monday.
    val todayIdx = (Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + 5) % 7

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0..6) {
            val isToday = i == todayIdx
            val isFuture = i > todayIdx
            val daysAgo = todayIdx - i
            val active = !isFuture && daysAgo < streak
            DayDot(letters[i], active, isToday, isFuture, colors)
        }
    }
}

@Composable
private fun DayDot(
    letter: String,
    active: Boolean,
    isToday: Boolean,
    isFuture: Boolean,
    colors: StreakStripColors
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = letter,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFuture) colors.futureLetter else colors.letter
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active -> colors.active
                        isFuture -> colors.future
                        else -> colors.empty
                    }
                )
                .then(
                    if (isToday) Modifier.border(2.dp, colors.ring, CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (active) Text(text = "🔥", fontSize = 15.sp)
        }
    }
}
