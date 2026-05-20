package com.colormagic.kids.presentation.screens.loading

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material3.Icon
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.R
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandSecondaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.MagicProgressBar
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

// The progress bar creeps toward 95% over this window while the backend
// works; if generation finishes sooner the screen just navigates on, if it
// takes longer the bar simply holds near full. Image models typically take
// 10–30s, so this is a believable "still working" pace, not a fake timer.
private const val PROGRESS_CREEP_MS = 25_000

@Composable
fun LoadingScreen(
    onSketchReady: () -> Unit,
    onCancel: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    // Once the backend confirms the sketch, hand off to SketchPreview.
    LaunchedEffect(state) {
        if (state is LoadingUiState.Ready) onSketchReady()
    }

    when (val s = state) {
        LoadingUiState.Generating, LoadingUiState.Ready ->
            GeneratingContent(onCancel = onCancel)
        LoadingUiState.NoCredits ->
            ProblemContent(
                title = "Out of sketches",
                message = "No sketch credits left. Ask a grown-up to unlock more.",
                primaryLabel = null,
                onPrimary = {},
                onBack = onCancel
            )
        is LoadingUiState.Error ->
            ProblemContent(
                title = "Hmm, that didn't work",
                message = s.message,
                primaryLabel = "Try Again",
                onPrimary = viewModel::generate,
                onBack = onCancel
            )
    }
}

@Composable
private fun GeneratingContent(onCancel: () -> Unit) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    // Indeterminate-feel progress: eases to 95% and holds.
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        progress.animateTo(
            targetValue = 0.95f,
            animationSpec = tween(durationMillis = PROGRESS_CREEP_MS, easing = LinearEasing)
        )
    }

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
            MagicProgressBar(progress = progress.value)
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
private fun ProblemContent(
    title: String,
    message: String,
    primaryLabel: String?,
    onPrimary: () -> Unit,
    onBack: () -> Unit
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
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SentimentDissatisfied,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(48.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
            Text(
                text = title,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = message,
                fontSize = 15.sp,
                lineHeight = 21.sp,
                color = BrandTokens.MutedInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            if (primaryLabel != null) {
                BrandPrimaryButton(label = primaryLabel, onClick = onPrimary)
                Spacer(Modifier.height(12.dp))
            }
            BrandSecondaryButton(
                label = "Go Back",
                onClick = onBack,
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

@Preview(name = "Loading – generating", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoadingPreviewGenerating() {
    ColorMagicKidsTheme {
        GeneratingContent(onCancel = {})
    }
}

@Preview(name = "Loading – no credits", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun LoadingPreviewNoCredits() {
    ColorMagicKidsTheme {
        ProblemContent(
            title = "Out of sketches",
            message = "No sketch credits left. Ask a grown-up to unlock more.",
            primaryLabel = null,
            onPrimary = {},
            onBack = {}
        )
    }
}
