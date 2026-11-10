package com.colormagic.kids.presentation.screens.purchasesuccess

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandTertiaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.ConfettiOverlay
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PurchaseSuccessViewModel @Inject constructor() : ViewModel()

@Composable
fun PurchaseSuccessScreen(
    onBackToHome: () -> Unit,
    onCreateSketch: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: PurchaseSuccessViewModel = hiltViewModel()
) {
    PurchaseSuccessContent(
        onBackToHome = onBackToHome,
        onCreateSketch = onCreateSketch
    )
}

@Composable
private fun PurchaseSuccessContent(
    onBackToHome: () -> Unit,
    onCreateSketch: () -> Unit
) {
    // Gentle "pop" of the badge, then a slow breathing pulse.
    val pop = remember { Animatable(0.4f) }
    LaunchedEffect(Unit) {
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow))
    }
    val pulse by rememberInfiniteTransition(label = "badge").animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "badgePulse"
    )

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            SparkleDecor(Alignment.TopStart, 24.dp, 72.dp, size = 40.dp, isStar = true)
            SparkleDecor(Alignment.TopEnd, (-24).dp, 120.dp, size = 36.dp, isStar = false)
            SparkleDecor(Alignment.BottomStart, 32.dp, (-90).dp, size = 40.dp, isStar = false)
            SparkleDecor(Alignment.BottomEnd, (-28).dp, (-60).dp, size = 44.dp, isStar = true)

            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    modifier = Modifier
                        .widthIn(max = 460.dp)
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0x14000000),
                            spotColor = Color(0x14000000)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .scale(pop.value * pulse)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "\uD83C\uDF89", fontSize = 48.sp)
                        }
                        Spacer(Modifier.height(20.dp))
                        BrandHeading(
                            text = "Woohoo! Magic unlocked!",
                            fontSize = 28.sp,
                            lineHeight = 34.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Thank you! Your new sketch credits are ready. " +
                                "Let's dream up something amazing together!",
                            fontSize = 16.sp,
                            lineHeight = 22.sp,
                            color = BrandTokens.MutedInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                        BrandPrimaryButton(
                            label = "Start Creating!",
                            onClick = onCreateSketch,
                            leadingIcon = Icons.Filled.Brush,
                            modifier = Modifier.fillMaxWidth(),
                            height = 58.dp,
                            edgeThickness = 6.dp
                        )
                        Spacer(Modifier.height(14.dp))
                        BrandTertiaryButton(
                            label = "Back to Home",
                            onClick = onBackToHome,
                            leadingIcon = Icons.Filled.Home,
                            modifier = Modifier.fillMaxWidth(),
                            height = 54.dp,
                            edgeThickness = 5.dp
                        )
                    }
                }
            }

            // Celebration burst on top of everything (doesn't block taps).
            ConfettiOverlay(modifier = Modifier.fillMaxSize())
        }
    }
}

@Composable
private fun SparkleDecor(
    alignment: Alignment,
    offsetX: Dp,
    offsetY: Dp,
    size: Dp,
    isStar: Boolean
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX, y = offsetY)
                .size(size),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (isStar) Icons.Filled.Star else Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = BrandTokens.MutedInk.copy(alpha = 0.45f),
                modifier = Modifier.size(size)
            )
        }
    }
}

@Preview(name = "Purchase Success – tablet", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
private fun PurchaseSuccessPreviewTablet() {
    ColorMagicKidsTheme {
        PurchaseSuccessContent(onBackToHome = {}, onCreateSketch = {})
    }
}

@Preview(name = "Purchase Success – phone", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun PurchaseSuccessPreviewPhone() {
    ColorMagicKidsTheme {
        PurchaseSuccessContent(onBackToHome = {}, onCreateSketch = {})
    }
}
