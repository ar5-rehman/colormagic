package com.colormagic.kids.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.random.Random

private data class ConfettiPiece(
    val xFraction: Float,
    val delayMs: Int,
    val durationMs: Int,
    val color: Color,
    val sizePx: Float,
    val spins: Int,
    val drift: Float
)

private val CONFETTI_COLORS = listOf(
    Color(0xFFEF5350), Color(0xFFFFA726), Color(0xFFFFEB3B),
    Color(0xFF66BB6A), Color(0xFF29B6F6), Color(0xFFAB47BC),
    Color(0xFFEC407A), Color(0xFF26A69A)
)

/**
 * A one-shot confetti burst that rains colored pieces down the screen, then
 * stops. Purely procedural (no image/sound assets). Drop it as the top layer of
 * a Box; it ignores touches (draw-only) so the screen stays interactive.
 */
@Composable
fun ConfettiOverlay(
    modifier: Modifier = Modifier,
    pieceCount: Int = 90,
    totalDurationMs: Int = 3000
) {
    val pieces = remember {
        List(pieceCount) {
            ConfettiPiece(
                xFraction = Random.nextFloat(),
                delayMs = Random.nextInt(0, 700),
                durationMs = Random.nextInt(1700, 2600),
                color = CONFETTI_COLORS[Random.nextInt(CONFETTI_COLORS.size)],
                sizePx = Random.nextInt(14, 30).toFloat(),
                spins = Random.nextInt(2, 6),
                drift = Random.nextFloat() * 2f - 1f
            )
        }
    }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, animationSpec = tween(totalDurationMs, easing = LinearEasing))
    }

    Canvas(modifier = modifier) {
        val elapsed = progress.value * totalDurationMs
        pieces.forEach { p ->
            val local = ((elapsed - p.delayMs) / p.durationMs).coerceIn(0f, 1f)
            if (local <= 0f) return@forEach
            val y = local * (size.height + 60f) - 30f
            val x = p.xFraction * size.width + p.drift * 80f * local
            rotate(degrees = p.spins * 360f * local, pivot = Offset(x, y)) {
                drawRect(
                    color = p.color,
                    topLeft = Offset(x, y),
                    size = Size(p.sizePx, p.sizePx * 0.5f)
                )
            }
        }
    }
}
