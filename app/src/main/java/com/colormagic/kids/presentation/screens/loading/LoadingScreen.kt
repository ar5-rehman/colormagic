package com.colormagic.kids.presentation.screens.loading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.R
import com.colormagic.kids.presentation.components.BrandSecondaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.MagicProgressBar
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

private const val SKETCH_GENERATION_MS = 3500

@Composable
fun LoadingScreen(
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // Drives both the progress bar fill and the eventual completion handoff.
    // When the user cancels, the composable leaves composition and this
    // coroutine is cancelled — onComplete will not fire.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = SKETCH_GENERATION_MS, easing = LinearEasing)
        )
        onComplete()
    }
    LoadingContent(progress = progress.value, onCancel = onCancel)
}

@Composable
private fun LoadingContent(
    progress: Float,
    onCancel: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = 28.dp,
                    end = 28.dp,
                    top = safeTop + 24.dp,
                    bottom = safeBottom + 24.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(1f))

            PencilGlowCard()

            Spacer(Modifier.height(36.dp))

            Text(
                text = "Creating your\nsketch...",
                fontSize = 30.sp,
                lineHeight = 38.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
            )

            Spacer(Modifier.height(12.dp))

            Text(
                text = "This may take a little moment",
                fontSize = 15.sp,
                color = BrandTokens.MutedInk
            )

            Spacer(Modifier.height(28.dp))

            MagicProgressBar(progress = progress)

            Spacer(Modifier.weight(1f))

            BrandSecondaryButton(
                label = "Cancel",
                onClick = onCancel,
                leadingIcon = Icons.Filled.Close
            )
        }
    }
}

@Composable
private fun PencilGlowCard() {
    Surface(
        modifier = Modifier
            .size(220.dp)
            .shadow(
                elevation = 32.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            ),
        color = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(R.drawable.magical_illustration_container),
                contentDescription = "Magic pencil",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .padding(8.dp)
            )
        }
    }
}

@Preview(name = "Loading – early", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoadingPreviewEarly() {
    ColorMagicKidsTheme {
        LoadingContent(progress = 0.18f, onCancel = {})
    }
}

@Preview(name = "Loading – mid", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoadingPreviewMid() {
    ColorMagicKidsTheme {
        LoadingContent(progress = 0.55f, onCancel = {})
    }
}

@Preview(name = "Loading – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun LoadingPreviewTablet() {
    ColorMagicKidsTheme {
        LoadingContent(progress = 0.42f, onCancel = {})
    }
}
