package com.colormagic.kids.presentation.screens.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.PhotoLibrary
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
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.CreditPill
import com.colormagic.kids.presentation.components.CreditPillStyle
import com.colormagic.kids.presentation.components.ShimmerBox
import com.colormagic.kids.presentation.components.StreakWeekStrip
import com.colormagic.kids.presentation.components.TactileSurface
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

private val GalleryAccent = Color(0xFF1F6F8B)
private val ParentAccent = Color(0xFF2E7D45)

@Composable
fun HomeScreen(
    onCreateNewSketch: () -> Unit = {},
    onCategoryClick: (HomeCategory) -> Unit = {},
    onOpenGallery: () -> Unit = {},
    onOpenParentArea: () -> Unit = {},
    onGetCredits: () -> Unit = {},
    onDailyIdea: (String) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()

    // Refresh "Sketches left" on every resume so a sketch made elsewhere
    // (Loading screen → backend deducts a credit) reflects immediately when
    // the kid returns to Home — no kill + relaunch needed.
    androidx.lifecycle.compose.LifecycleEventEffect(
        androidx.lifecycle.Lifecycle.Event.ON_RESUME
    ) {
        viewModel.refreshQuota()
    }

    // Streak-advance celebration: confetti + chime, once per new day.
    if (state.showStreakCelebration) {
        androidx.compose.runtime.LaunchedEffect(Unit) {
            com.colormagic.kids.presentation.util.CelebrationFx.playSuccess()
            kotlinx.coroutines.delay(3200)
            viewModel.streakCelebrationShown()
        }
    }

    androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
        if (info.isCompactWidth) {
            HomeContent(
                state = state,
                onCreateNewSketch = onCreateNewSketch,
                onCategoryClick = onCategoryClick,
                onOpenGallery = onOpenGallery,
                onOpenParentArea = onOpenParentArea,
                onGetCredits = onGetCredits,
                onDailyIdea = onDailyIdea
            )
        } else {
            HomeTabletContent(
                state = state,
                onCreateNewSketch = onCreateNewSketch,
                onCategoryClick = onCategoryClick,
                onOpenGallery = onOpenGallery,
                onOpenParentArea = onOpenParentArea,
                onGetCredits = onGetCredits,
                onDailyIdea = onDailyIdea
            )
        }
        if (state.showStreakCelebration) {
            com.colormagic.kids.presentation.components.ConfettiOverlay(
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun HomeTabletContent(
    state: HomeUiState,
    onCreateNewSketch: () -> Unit,
    onCategoryClick: (HomeCategory) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenParentArea: () -> Unit,
    onGetCredits: () -> Unit = {},
    onDailyIdea: (String) -> Unit = {}
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
            // Centre the two columns vertically so a portrait tablet doesn't
            // leave a large empty area beneath the content.
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left sidebar — greeting + child illustration
            Column(
                modifier = Modifier
                    .weight(0.42f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                CreditPillButton(sketchesLeft = state.sketchesLeft, onClick = onGetCredits)
                BrandHeading(
                    text = "What shall we\ncolor today?",
                    fontSize = 34.sp,
                    lineHeight = 42.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                if (state.streak > 0) {
                    StreakCard(streak = state.streak, best = state.streakBest)
                }
                DailyIdeaCard(idea = state.dailyIdea, onClick = { onDailyIdea(state.dailyIdea) })
                ChildIllustrationCard()
            }

            // Right panel — actions
            Column(
                modifier = Modifier
                    .weight(0.58f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                CreateNewSketchCard(onClick = onCreateNewSketch)
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    TabletActionTile(
                        title = "My Gallery",
                        icon = Icons.Outlined.PhotoLibrary,
                        container = Color(0xFFB3E5FC),
                        ink = Color(0xFF01579B),
                        onClick = onOpenGallery,
                        modifier = Modifier.weight(1f)
                    )
                    TabletActionTile(
                        title = "Parent Area",
                        icon = Icons.Outlined.ManageAccounts,
                        container = Color(0xFFA9C690),
                        ink = Color(0xFF1B3A1F),
                        onClick = onOpenParentArea,
                        modifier = Modifier.weight(1f)
                    )
                }
                MagicCategoriesBar(
                    categories = state.categories,
                    onCategoryClick = onCategoryClick
                )
            }
        }
    }
}

@Composable
private fun ChildIllustrationCard() {
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = Color(0xFFCFB9EF),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
    ) {
        Box(
            modifier = Modifier.padding(14.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.child_coloring),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
            )
        }
    }
}

@Composable
private fun TabletActionTile(
    title: String,
    icon: ImageVector,
    container: Color,
    ink: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(26.dp),
        color = container,
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(28.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = ink
            )
        }
    }
}

@Composable
private fun MagicCategoriesBar(
    categories: List<HomeCategory>,
    onCategoryClick: (HomeCategory) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = BrandTokens.SubtleSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Magic Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTokens.HeadingInk
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                categories.forEach { category ->
                    OutlineCategoryChip(
                        category = category,
                        onClick = { onCategoryClick(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun OutlineCategoryChip(category: HomeCategory, onClick: () -> Unit) {
    val accent = when (category.tone) {
        CategoryTone.Blue -> Color(0xFF4FA8C7)
        CategoryTone.Lavender -> Color(0xFF8E6EBE)
        CategoryTone.Grey -> Color(0xFF7A7A7A)
        CategoryTone.GreenDeep -> Color(0xFF4F7C53)
        CategoryTone.GreenLight -> Color(0xFF6FA773)
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.5.dp, accent),
        modifier = Modifier.height(38.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 18.dp)
        ) {
            Text(
                text = category.label,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onCreateNewSketch: () -> Unit,
    onCategoryClick: (HomeCategory) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenParentArea: () -> Unit,
    onGetCredits: () -> Unit = {},
    onDailyIdea: (String) -> Unit = {}
) {
    // Even with system bars hidden, devices still have a display cutout / camera
    // hole at the top. Pad for it so the heading never bleeds into the notch.
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(
                start = 24.dp,
                end = 24.dp,
                top = safeTop + 32.dp,
                bottom = 16.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            fullWidth {
                BrandHeading(text = "What shall we color\ntoday?")
                Spacer(Modifier.height(20.dp))
            }

            fullWidth {
                CreditPillButton(sketchesLeft = state.sketchesLeft, onClick = onGetCredits)
                Spacer(Modifier.height(16.dp))
                if (state.streak > 0) {
                    StreakCard(streak = state.streak, best = state.streakBest)
                    Spacer(Modifier.height(16.dp))
                }
                DailyIdeaCard(idea = state.dailyIdea, onClick = { onDailyIdea(state.dailyIdea) })
                Spacer(Modifier.height(24.dp))
            }

            fullWidth {
                CreateNewSketchCard(onClick = onCreateNewSketch)
                Spacer(Modifier.height(20.dp))
            }

            items(state.categories, key = { it.id }) { category ->
                CategoryChip(category = category, onClick = { onCategoryClick(category) })
            }

            fullWidth {
                Spacer(Modifier.height(22.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    ActionTile(
                        title = "My Gallery",
                        icon = Icons.Outlined.PhotoLibrary,
                        accent = GalleryAccent,
                        onClick = onOpenGallery,
                        modifier = Modifier.weight(1f)
                    )
                    ActionTile(
                        title = "Parent Area",
                        icon = Icons.Outlined.ManageAccounts,
                        accent = ParentAccent,
                        onClick = onOpenParentArea,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun CreateNewSketchCard(onClick: () -> Unit) {
    TactileSurface(
        onClick = onClick,
        fill = MaterialTheme.colorScheme.primary,
        edge = BrandTokens.PrimaryEdge,
        shape = RoundedCornerShape(36.dp),
        height = 210.dp,
        edgeThickness = 12.dp,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Filled.Edit,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = Color.White
            )
            Spacer(Modifier.height(18.dp))
            Text(
                text = "Create New\nSketch",
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White,
                fontFamily = MaterialTheme.typography.headlineSmall.fontFamily
            )
        }
    }
}

@Composable
private fun CategoryChip(category: HomeCategory, onClick: () -> Unit) {
    val (fill, ink, edge) = when (category.tone) {
        CategoryTone.Blue -> Triple(Color(0xFFB3E5FC), Color(0xFF01579B), Color(0xFF4FA8C7))
        CategoryTone.Lavender -> Triple(Color(0xFFE1D3F7), Color(0xFF311B92), Color(0xFF8E6EBE))
        CategoryTone.Grey -> Triple(Color(0xFFE6E6E6), Color(0xFF424242), Color(0xFFB0B0B0))
        CategoryTone.GreenDeep -> Triple(Color(0xFF8FB892), Color(0xFF1B3A1F), Color(0xFF4F7C53))
        CategoryTone.GreenLight -> Triple(Color(0xFFB7E1B9), Color(0xFF1B5E20), Color(0xFF6FA773))
    }
    TactileSurface(
        onClick = onClick,
        fill = fill,
        edge = edge,
        shape = RoundedCornerShape(50),
        height = 58.dp,
        edgeThickness = 6.dp,
        contentColor = ink,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = category.label,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = ink
        )
    }
}

@Composable
private fun ActionTile(
    title: String,
    icon: ImageVector,
    accent: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .aspectRatio(1f)
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x1F000000),
                spotColor = Color(0x1F000000)
            )
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                shape = RoundedCornerShape(28.dp)
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(40.dp)
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = accent
            )
        }
    }
}

private fun LazyGridScope.fullWidth(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    // Wrap in a Column so multiple children (e.g. credit pill + streak card +
    // daily-idea card) STACK vertically instead of overlapping in the cell.
    item(span = { GridItemSpan(maxLineSpan) }) {
        androidx.compose.foundation.layout.Column { content() }
    }
}

/** Prominent weekly-flame streak card — a gentle daily-return reward. The
 *  7-day strip shows which days were colored; today is highlighted. */
@Composable
private fun StreakCard(streak: Int, best: Int) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFFB74D),
        shadowElevation = 3.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        // Warm, prominent amber → orange — reads as "fire" while
                        // staying light enough for the flame strip to pop.
                        listOf(Color(0xFFFFD27A), Color(0xFFFF9E4D))
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            // Header row: flame + "N-day streak!" + Best
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🔥", fontSize = 26.sp)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = if (streak == 1) "1-Day Streak!" else "$streak-Day Streak!",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.weight(1f)
                )
                if (best > 1) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Best",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xCCFFFFFF)
                        )
                        Text(
                            text = "$best",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
            Spacer(Modifier.height(14.dp))
            StreakWeekStrip(
                streak = streak,
                colors = com.colormagic.kids.presentation.components.StreakStripColors(
                    active = Color.White,
                    empty = Color(0x33FFFFFF),
                    future = Color(0x22FFFFFF),
                    letter = Color.White,
                    futureLetter = Color(0x88FFFFFF),
                    ring = Color.White
                )
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Color tomorrow to reach ${streak + 1}! 🎨",
                fontSize = 13.sp,
                color = Color(0xE6FFFFFF)
            )
        }
    }
}

/** "Today's magic word" card — a fresh suggested idea; tap to start it. */
@Composable
private fun DailyIdeaCard(idea: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFEDE7F6),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp)
        ) {
            Text(text = "✨", fontSize = 26.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Today's magic idea",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF7E6BA8)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = idea.replaceFirstChar { it.uppercase() },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4A347E)
                )
            }
            Text(text = "→", fontSize = 22.sp, color = Color(0xFF4A347E))
        }
    }
}

/**
 * Tappable credit balance pill. Tapping opens the Get Credits screen so the
 * parent can top up. Shows "…" while the first server fetch is in-flight.
 */
@Composable
private fun CreditPillButton(sketchesLeft: Int?, onClick: () -> Unit) {
    // Still loading the balance from Firebase → shimmer a pill-shaped placeholder
    // instead of flashing a misleading "Out of credits".
    if (sketchesLeft == null) {
        ShimmerBox(
            shape = RoundedCornerShape(50),
            modifier = Modifier
                .height(34.dp)
                .width(128.dp)
        )
        return
    }
    val label = if (sketchesLeft == 0) "Out of credits — tap to get more"
    else "Credits: $sketchesLeft"
    val style = if (sketchesLeft == 0) CreditPillStyle.Primary else CreditPillStyle.Subtle
    CreditPill(
        text = label,
        style = style,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Preview(name = "Home – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun HomePreviewPhone() {
    ColorMagicKidsTheme {
        HomeContent(
            state = HomeUiState(),
            onCreateNewSketch = {},
            onCategoryClick = {},
            onOpenGallery = {},
            onOpenParentArea = {}
        )
    }
}

@Preview(name = "Home – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun HomePreviewTablet() {
    ColorMagicKidsTheme {
        HomeContent(
            state = HomeUiState(),
            onCreateNewSketch = {},
            onCategoryClick = {},
            onOpenGallery = {},
            onOpenParentArea = {}
        )
    }
}
