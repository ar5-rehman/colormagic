package com.colormagic.kids.presentation.screens.purchasesuccess

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Sparkle / star confetti scattered around the centered card.
            SparkleDecor(Alignment.TopStart, (-40).dp, 60.dp, size = 64.dp, isStar = true)
            SparkleDecor(Alignment.TopEnd, 0.dp, 120.dp, size = 44.dp, isStar = false)
            SparkleDecor(Alignment.BottomStart, 100.dp, 0.dp, size = 56.dp, isStar = false)
            SparkleDecor(Alignment.BottomEnd, (-30).dp, (-60).dp, size = 50.dp, isStar = true)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = Color(0x14000000),
                            spotColor = Color(0x14000000)
                        )
                ) {
                    Column(
                        modifier = Modifier.padding(40.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                        Spacer(Modifier.height(20.dp))
                        BrandHeading(
                            text = "You're all set!",
                            fontSize = 30.sp,
                            lineHeight = 36.sp,
                            color = MaterialTheme.colorScheme.primary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = "Sketch credits are ready to use.",
                            fontSize = 16.sp,
                            color = BrandTokens.MutedInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(28.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                BrandTertiaryButton(
                                    label = "Back to Home",
                                    onClick = onBackToHome,
                                    leadingIcon = Icons.Filled.Palette,
                                    height = 54.dp,
                                    edgeThickness = 5.dp
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                BrandPrimaryButton(
                                    label = "Create a Sketch",
                                    onClick = onCreateSketch,
                                    leadingIcon = Icons.Filled.Brush,
                                    height = 54.dp,
                                    edgeThickness = 5.dp
                                )
                            }
                        }
                    }
                }
            }
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
