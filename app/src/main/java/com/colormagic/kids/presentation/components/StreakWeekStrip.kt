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
import java.time.LocalDate

/**
 * A Duolingo-style 7-day flame strip (Mon→Sun of the current week).
 *
 * A day shows a 🔥 when it's part of the current streak; days before the streak
 * started are empty, days later this week (the future) are dimmed. Today gets a
 * highlight ring. The pattern is derived purely from [streak] + today's weekday,
 * so no per-day history is needed: the streak always ends "today" (Home records
 * the open), so the last [streak] days up to today are the flames.
 */
@Composable
fun StreakWeekStrip(
    streak: Int,
    modifier: Modifier = Modifier
) {
    val letters = listOf("M", "T", "W", "T", "F", "S", "S")
    val todayIdx = LocalDate.now().dayOfWeek.value - 1 // Mon=0 … Sun=6

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        for (i in 0..6) {
            val isToday = i == todayIdx
            val isFuture = i > todayIdx
            val daysAgo = todayIdx - i
            val active = !isFuture && daysAgo < streak
            DayDot(letter = letters[i], active = active, isToday = isToday, isFuture = isFuture)
        }
    }
}

@Composable
private fun DayDot(letter: String, active: Boolean, isToday: Boolean, isFuture: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = letter,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isFuture) Color(0xFFD3C4A8) else Color(0xFFB06A1F)
        )
        Spacer(Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(
                    when {
                        active -> Color(0xFFFFB74D)      // lit flame day
                        isFuture -> Color(0xFFF6EEDD)    // upcoming (faint)
                        else -> Color(0xFFEFE2C8)        // missed / before streak
                    }
                )
                .then(
                    if (isToday) Modifier.border(2.dp, Color(0xFF8A4B00), CircleShape)
                    else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (active) Text(text = "🔥", fontSize = 15.sp)
        }
    }
}
