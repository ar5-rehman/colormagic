package com.colormagic.kids.presentation.screens.sketchpreview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandTertiaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.BrandTopBar
import com.colormagic.kids.presentation.components.CreditPill
import com.colormagic.kids.presentation.components.CreditPillStyle
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun SketchPreviewScreen(
    onBack: () -> Unit,
    onColorThis: (Sketch) -> Unit,
    onTryAnother: () -> Unit,
    viewModel: SketchPreviewViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    SketchPreviewContent(
        sketch = state.sketch,
        onBack = onBack,
        onColorThis = { onColorThis(state.sketch) },
        onTryAnother = onTryAnother
    )
}

@Composable
private fun SketchPreviewContent(
    sketch: Sketch,
    onBack: () -> Unit,
    onColorThis: () -> Unit,
    onTryAnother: () -> Unit
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
                .padding(top = safeTop, bottom = safeBottom)
        ) {
            BrandTopBar(
                onBack = onBack,
                title = {
                    Text(
                        text = "ColorMagic",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
                    )
                },
                trailing = {
                    // Reserved badge slot — wire to plan/credits later.
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(50)
                            )
                    ) {
                        Text(
                            text = "PP",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            )

            Spacer(Modifier.height(28.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 32.dp)
                    .fillMaxWidth()
            ) {
                SketchCard(sketch = sketch)
            }

            Spacer(Modifier.weight(1f))

            Column(
                modifier = Modifier.padding(horizontal = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BrandPrimaryButton(
                    label = "Color This",
                    onClick = onColorThis,
                    leadingIcon = Icons.Filled.Palette
                )
                BrandTertiaryButton(
                    label = "Try Another",
                    onClick = onTryAnother,
                    leadingIcon = Icons.Filled.Refresh
                )
                CreditPill(
                    text = "Trying another sketch uses 1 more credit",
                    icon = Icons.Filled.Info,
                    style = CreditPillStyle.Subtle
                )
            }

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun SketchCard(sketch: Sketch) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            ),
        color = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(sketch.placeholderTint)),
                contentAlignment = Alignment.Center
            ) {
                // Backend will provide imageUrl → swap for AsyncImage here.
                Text(
                    text = "✏️",
                    fontSize = 86.sp
                )
            }
            Icon(
                imageVector = Icons.Filled.AutoAwesome,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(20.dp)
                    .size(22.dp)
            )
        }
    }
}

@Preview(name = "Sketch Preview – phone", showBackground = true, widthDp = 360, heightDp = 820)
@Composable
private fun SketchPreviewPhone() {
    ColorMagicKidsTheme {
        SketchPreviewContent(
            sketch = Sketch(id = "p", prompt = "preview"),
            onBack = {},
            onColorThis = {},
            onTryAnother = {}
        )
    }
}
