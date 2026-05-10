package com.colormagic.kids.presentation.screens.home

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.CreditPill
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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HomeContent(
        state = state,
        onCreateNewSketch = onCreateNewSketch,
        onCategoryClick = onCategoryClick,
        onOpenGallery = onOpenGallery,
        onOpenParentArea = onOpenParentArea
    )
}

@Composable
private fun HomeContent(
    state: HomeUiState,
    onCreateNewSketch: () -> Unit,
    onCategoryClick: (HomeCategory) -> Unit,
    onOpenGallery: () -> Unit,
    onOpenParentArea: () -> Unit
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
                CreditPill(text = "Sketches left: ${state.sketchesLeft}")
                Spacer(Modifier.height(28.dp))
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

private fun LazyGridScope.fullWidth(content: @Composable () -> Unit) {
    item(span = { GridItemSpan(maxLineSpan) }) { content() }
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
