package com.colormagic.kids.presentation.screens.createsketch

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.horizontalScroll
import com.colormagic.kids.domain.model.CategoryIdeas
import com.colormagic.kids.domain.model.ColoringIdea
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandPromptInput
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.CreditPill
import com.colormagic.kids.presentation.components.CreditPillStyle
import com.colormagic.kids.presentation.components.IdeaCard
import com.colormagic.kids.presentation.components.LowCreditsModal
import com.colormagic.kids.presentation.components.SectionTitle
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun CreateSketchScreen(
    onBack: () -> Unit,
    onMakeSketchRequested: (prompt: String) -> Unit,
    onUpgrade: () -> Unit,
    onGetCredits: () -> Unit = onUpgrade,
    viewModel: CreateSketchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()

    // Refresh the credit count every time the screen resumes — a purchase
    // made on the Subscription screen should re-enable "Make My Sketch"
    // the moment the parent navigates back.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshQuota()
        // Reshuffle the "Need ideas?" pool each time the screen opens — the
        // kid sees fresh suggestions rather than the same 6 every visit.
        viewModel.shuffleIdeas()
    }

    // Low-credits modal: shown when the user taps "Make My Sketch" without credits
    if (state.showLowCreditsModal) {
        LowCreditsModal(
            isPremium = state.isPremium,
            onWatchAd = {
                viewModel.onLowCreditsModalDismissed()
                onGetCredits()
            },
            onGoPremium = {
                viewModel.onLowCreditsModalDismissed()
                onUpgrade()
            },
            onDismiss = viewModel::onLowCreditsModalDismissed
        )
    }

    val onMakeSketch: () -> Unit = {
        when {
            state.canMakeSketch -> onMakeSketchRequested(state.prompt.trim())
            state.outOfCredits -> viewModel.onMakeSketchBlockedByCredits()
        }
    }

    if (info.isCompactWidth) {
        CreateSketchContent(
            state = state,
            onBack = onBack,
            onPromptChanged = viewModel::onPromptChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onMakeSketch = onMakeSketch,
            onIdeaSelected = viewModel::onIdeaSelected,
            onUpgrade = onUpgrade,
            onGetCredits = onGetCredits
        )
    } else {
        CreateSketchTabletContent(
            state = state,
            onBack = onBack,
            onPromptChanged = viewModel::onPromptChanged,
            onCategorySelected = viewModel::onCategorySelected,
            onMakeSketch = onMakeSketch,
            onIdeaSelected = viewModel::onIdeaSelected,
            onUpgrade = onUpgrade,
            onGetCredits = onGetCredits
        )
    }
}

@Composable
private fun CreateSketchTabletContent(
    state: CreateSketchUiState,
    onBack: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onMakeSketch: () -> Unit,
    onIdeaSelected: (ColoringIdea) -> Unit,
    onUpgrade: () -> Unit,
    onGetCredits: () -> Unit = onUpgrade
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
            BackChip(onClick = onBack)
            Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier
                .fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(28.dp)
        ) {
            // Left — prompt + categories + submit
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandTokens.SubtleOutline),
                modifier = Modifier
                    .weight(0.45f)
                    .verticalScroll(rememberScrollState())
            ) {
                Column(modifier = Modifier.padding(28.dp)) {
                    BrandHeading(
                        text = "Describe your coloring page",
                        fontSize = 24.sp,
                        lineHeight = 30.sp
                    )
                    Spacer(Modifier.height(18.dp))
                    BrandPromptInput(
                        value = state.prompt,
                        onValueChange = onPromptChanged,
                        placeholder = if (state.allowFreeText) {
                            "A cute dinosaur eating an apple..."
                        } else {
                            "Tap a category below to pick an idea"
                        },
                        enabled = state.allowFreeText
                    )
                    Spacer(Modifier.height(24.dp))
                    Text(
                        text = "Categories",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTokens.HeadingInk
                    )
                    Spacer(Modifier.height(10.dp))
                    CategoryChipRow(
                        categories = state.categories,
                        onCategorySelected = onCategorySelected
                    )
                    Spacer(Modifier.height(28.dp))
                    if (!state.isOnline) {
                        OfflineBanner()
                        Spacer(Modifier.height(14.dp))
                    }
                    BrandPrimaryButton(
                        label = "Make My Sketch",
                        onClick = onMakeSketch,
                        leadingIcon = Icons.Filled.AutoFixHigh,
                        enabled = state.canMakeSketch
                    )
                    Spacer(Modifier.height(14.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CreditCostPill(state = state, onGetCredits = onGetCredits)
                    }
                }
            }

            // Right — ideas grid
            Column(modifier = Modifier.weight(0.55f)) {
                Text(
                    text = "✨ Tap a fun idea!",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk
                )
                Spacer(Modifier.height(14.dp))
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(state.ideas, key = { it.id }) { idea ->
                        IdeaCard(
                            idea = idea,
                            onClick = { onIdeaSelected(idea) }
                        )
                    }
                }
            }
        }
        }
    }
}

@Composable
private fun CreateSketchContent(
    state: CreateSketchUiState,
    onBack: () -> Unit,
    onPromptChanged: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
    onMakeSketch: () -> Unit,
    onIdeaSelected: (ColoringIdea) -> Unit,
    onUpgrade: () -> Unit,
    onGetCredits: () -> Unit = onUpgrade
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = safeTop + 24.dp,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            item {
                BackChip(onClick = onBack)
                Spacer(Modifier.height(12.dp))
                BrandHeading(text = "Describe your\ncoloring page")
                Spacer(Modifier.height(20.dp))
            }
            item {
                BrandPromptInput(
                    value = state.prompt,
                    onValueChange = onPromptChanged,
                    placeholder = "A cute dinosaur eating an apple..."
                )
                Spacer(Modifier.height(20.dp))
            }
            item {
                Text(
                    text = "Categories",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTokens.HeadingInk
                )
                Spacer(Modifier.height(8.dp))
                CategoryChipRow(
                    categories = state.categories,
                    onCategorySelected = onCategorySelected
                )
                Spacer(Modifier.height(20.dp))
            }
            if (!state.isOnline) {
                item {
                    OfflineBanner()
                    Spacer(Modifier.height(12.dp))
                }
            }
            item {
                BrandPrimaryButton(
                    label = "Make My Sketch",
                    onClick = onMakeSketch,
                    leadingIcon = Icons.Filled.AutoFixHigh,
                    enabled = state.canMakeSketch
                )
                Spacer(Modifier.height(12.dp))
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CreditCostPill(state = state, onGetCredits = onGetCredits)
                }
                Spacer(Modifier.height(28.dp))
            }
            item {
                SectionTitle(
                    text = "Need an idea? Tap one!",
                    icon = Icons.Filled.Lightbulb
                )
                Spacer(Modifier.height(14.dp))
            }
            items(state.ideas, key = { it.id }) { idea ->
                IdeaCard(
                    idea = idea,
                    onClick = { onIdeaSelected(idea) }
                )
                Spacer(Modifier.height(14.dp))
            }
        }
    }
}

// Horizontally scrolling row of category chips. Tapping a chip drops a random
// idea from that category into the prompt input — saves the kid having to
// think of a subject from scratch.
@Composable
private fun CategoryChipRow(
    categories: List<String>,
    onCategorySelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { key ->
            Surface(
                onClick = { onCategorySelected(key) },
                shape = RoundedCornerShape(50),
                color = BrandTokens.SubtleSurface,
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "${CategoryIdeas.emoji[key].orEmpty()} ${CategoryIdeas.labels[key] ?: key}".trim(),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTokens.HeadingInk,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                )
            }
        }
    }
}

// Small circular back arrow chip used as the workflow-screen back affordance.
// Placed at the top-left of CreateSketch so the kid (or a parent helping out)
// has an obvious way back to Home — the bottom-nav is intentionally hidden
// on nested workflow screens.
@Composable
private fun BackChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = androidx.compose.foundation.shape.CircleShape,
        color = BrandTokens.SubtleSurface,
        modifier = Modifier.size(42.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BrandTokens.HeadingInk,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// Shown when the device has no internet. Generation runs on the server, so
// "Make My Sketch" is disabled — but the saved gallery still works offline,
// which this banner makes clear in kid-friendly language.
@Composable
private fun OfflineBanner() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFFFFF3E0),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "📴", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Text(
                text = "You're offline. Connect to the internet to make new pictures — " +
                    "you can still color the ones in your gallery!",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF8A4B00)
            )
        }
    }
}

// Shows the per-sketch cost, or — when credits are spent — a tappable
// "out of credits" prompt that links to the Get Credits screen.
@Composable
private fun CreditCostPill(state: CreateSketchUiState, onGetCredits: () -> Unit) {
    when {
        state.outOfCredits -> CreditPill(
            text = "Out of credits — tap to get more",
            style = CreditPillStyle.Primary,
            modifier = Modifier.clickable(onClick = onGetCredits)
        )
        state.dailyLimitReached -> CreditPill(
            text = "Today's sketch limit reached — come back tomorrow!",
            style = CreditPillStyle.Primary
        )
        else -> CreditPill(
            text = "Uses ${state.sketchCreditsCost} sketch credit",
            style = CreditPillStyle.Subtle
        )
    }
}

@Preview(name = "Create Sketch – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun CreateSketchPreviewPhone() {
    ColorMagicKidsTheme {
        CreateSketchContent(
            state = CreateSketchUiState(),
            onBack = {},
            onPromptChanged = {},
            onCategorySelected = {},
            onMakeSketch = {},
            onIdeaSelected = {},
            onUpgrade = {}
        )
    }
}

@Preview(name = "Create Sketch – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun CreateSketchPreviewTablet() {
    ColorMagicKidsTheme {
        CreateSketchContent(
            state = CreateSketchUiState(),
            onBack = {},
            onPromptChanged = {},
            onCategorySelected = {},
            onMakeSketch = {},
            onIdeaSelected = {},
            onUpgrade = {}
        )
    }
}
