package com.colormagic.kids.presentation.screens.savesuccess

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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Star
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.model.SavedPicture
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandTertiaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.ConfettiOverlay
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun SaveSuccessScreen(
    onGoToGallery: () -> Unit,
    onCreateAnother: () -> Unit,
    viewModel: SaveSuccessViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    // Cheerful chime to go with the confetti — plays once when the screen opens.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        com.colormagic.kids.presentation.util.CelebrationFx.playSuccess()
    }
    Box(modifier = Modifier.fillMaxSize()) {
        if (info.isCompactWidth) {
            SaveSuccessContent(
                picture = state.picture,
                onGoToGallery = onGoToGallery,
                onCreateAnother = onCreateAnother
            )
        } else {
            SaveSuccessTabletContent(
                picture = state.picture,
                onGoToGallery = onGoToGallery,
                onCreateAnother = onCreateAnother
            )
        }
        // A celebratory confetti burst rains over the success screen once.
        // Draw-only (no touch handling) so the buttons stay tappable.
        ConfettiOverlay(modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun SaveSuccessTabletContent(
    picture: SavedPicture,
    onGoToGallery: () -> Unit,
    onCreateAnother: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Confetti accents scattered around the centered card.
            ConfettiDot(MaterialTheme.colorScheme.primaryContainer, 44.dp, 80.dp, 90.dp, Alignment.TopStart)
            ConfettiDot(MaterialTheme.colorScheme.tertiaryContainer, 60.dp, (-60).dp, 120.dp, Alignment.TopEnd)
            ConfettiDot(MaterialTheme.colorScheme.secondaryContainer, 50.dp, 120.dp, (-50).dp, Alignment.BottomStart)
            ConfettiDot(MaterialTheme.colorScheme.primaryContainer, 36.dp, (-100).dp, (-80).dp, Alignment.BottomEnd)
            ConfettiDot(MaterialTheme.colorScheme.tertiaryContainer, 40.dp, 180.dp, 220.dp, Alignment.TopStart)
            ConfettiDot(MaterialTheme.colorScheme.secondaryContainer, 30.dp, (-200).dp, 60.dp, Alignment.TopEnd)

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(36.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(36.dp),
                            ambientColor = Color(0x14000000),
                            spotColor = Color(0x14000000)
                        )
                ) {
                    Row(
                        modifier = Modifier.padding(32.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(36.dp)
                    ) {
                        // Picture preview — square, on the left.
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(22.dp))
                                .background(Color(picture.placeholderTint)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(text = "🎨", fontSize = 96.sp)
                        }

                        // Right column — heading, subtitle, buttons.
                        Column(
                            modifier = Modifier.weight(0.5f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            BrandHeading(
                                text = "Beautiful\npicture saved!",
                                fontSize = 36.sp,
                                lineHeight = 44.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(14.dp))
                            Text(
                                text = "Your masterpiece is safe and sound.",
                                fontSize = 16.sp,
                                color = BrandTokens.MutedInk
                            )
                            Spacer(Modifier.height(24.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.weight(1f)) {
                                    BrandTertiaryButton(
                                        label = "Go to Gallery",
                                        onClick = onGoToGallery,
                                        leadingIcon = Icons.Filled.Palette,
                                        height = 52.dp,
                                        edgeThickness = 5.dp
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    BrandPrimaryButton(
                                        label = "Create Another",
                                        onClick = onCreateAnother,
                                        leadingIcon = Icons.Filled.Brush,
                                        height = 52.dp,
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
}

@Composable
private fun SaveSuccessContent(
    picture: SavedPicture,
    onGoToGallery: () -> Unit,
    onCreateAnother: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Decorative confetti — pulled to constants so a designer can
            // re-position them without touching the layout columns.
            ConfettiDot(color = MaterialTheme.colorScheme.primaryContainer, size = 36.dp,
                offsetX = (-30).dp, offsetY = 30.dp,
                alignment = Alignment.TopStart)
            ConfettiDot(color = MaterialTheme.colorScheme.secondaryContainer, size = 50.dp,
                offsetX = 30.dp, offsetY = 90.dp,
                alignment = Alignment.TopEnd)
            ConfettiDot(color = MaterialTheme.colorScheme.tertiaryContainer, size = 30.dp,
                offsetX = (-40).dp, offsetY = (-30).dp,
                alignment = Alignment.BottomStart)
            ConfettiDot(color = MaterialTheme.colorScheme.secondaryContainer, size = 40.dp,
                offsetX = 50.dp, offsetY = (-60).dp,
                alignment = Alignment.BottomEnd)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 28.dp)
                    .padding(top = safeTop + 32.dp, bottom = safeBottom + 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.weight(0.4f))

                SuccessBadge()

                Spacer(Modifier.height(24.dp))

                BrandHeading(
                    text = "Beautiful\npicture saved!",
                    fontSize = 38.sp,
                    lineHeight = 46.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Your masterpiece is safe and sound.",
                    fontSize = 16.sp,
                    color = BrandTokens.MutedInk,
                    textAlign = TextAlign.Center
                )

                Spacer(Modifier.height(28.dp))

                SavedPicturePreview(picture)

                Spacer(Modifier.weight(1f))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BrandPrimaryButton(
                        label = "Go to Gallery",
                        onClick = onGoToGallery,
                        leadingIcon = Icons.Filled.Palette
                    )
                    BrandTertiaryButton(
                        label = "Create Another",
                        onClick = onCreateAnother,
                        leadingIcon = Icons.Filled.Brush
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessBadge() {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(Color(0xFF8FB892)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = Color(0xFF1B3A1F),
            modifier = Modifier.size(42.dp)
        )
    }
}

@Composable
private fun SavedPicturePreview(picture: SavedPicture) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            ),
        color = Color.White,
        shape = RoundedCornerShape(26.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color(picture.placeholderTint))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Swap to AsyncImage(picture.imageUrl) when backend lands.
            Text(text = "🎨", fontSize = 64.sp)
        }
    }
}

@Composable
private fun ConfettiDot(
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    alignment: Alignment
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .align(alignment)
                .offset(x = offsetX, y = offsetY)
                .size(size)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Preview(name = "Save Success – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun SaveSuccessPreviewPhone() {
    ColorMagicKidsTheme {
        SaveSuccessContent(
            picture = SavedPicture(id = "p", sourceSketchId = "s"),
            onGoToGallery = {},
            onCreateAnother = {}
        )
    }
}
