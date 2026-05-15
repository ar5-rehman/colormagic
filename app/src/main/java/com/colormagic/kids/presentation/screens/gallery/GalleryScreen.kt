package com.colormagic.kids.presentation.screens.gallery

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.model.GalleryArtwork
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.ArtworkCard
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.FilterPill
import com.colormagic.kids.presentation.components.ParentBrandHeader
import com.colormagic.kids.presentation.components.TactileSurface
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun GalleryScreen(
    onStartNewArt: () -> Unit = {},
    onOpenArtwork: (GalleryArtwork) -> Unit = {},
    onFilter: () -> Unit = {},
    viewModel: GalleryViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    if (info.isCompactWidth) {
        GalleryContent(
            state = state,
            onStartNewArt = onStartNewArt,
            onOpenArtwork = onOpenArtwork,
            onDelete = viewModel::onDelete,
            onFilter = onFilter
        )
    } else {
        GalleryTabletContent(
            state = state,
            onStartNewArt = onStartNewArt,
            onOpenArtwork = onOpenArtwork,
            onDelete = viewModel::onDelete,
            onFilter = onFilter
        )
    }
}

@Composable
private fun GalleryTabletContent(
    state: GalleryUiState,
    onStartNewArt: () -> Unit,
    onOpenArtwork: (GalleryArtwork) -> Unit,
    onDelete: (String) -> Unit,
    onFilter: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp, vertical = 24.dp)) {
            // Heading + Filter row
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                BrandHeading(
                    text = "My Magical Creations",
                    fontSize = 32.sp,
                    lineHeight = 38.sp,
                    modifier = Modifier.weight(1f)
                )
                FilterPill(label = "Filter", onClick = onFilter)
            }

            Spacer(Modifier.height(20.dp))

            // 4-column grid of artworks; the "Start New Art" tile slots in as the last item.
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(state.artworks, key = { it.id }) { artwork ->
                    GalleryTabletCard(
                        artwork = artwork,
                        onOpen = { onOpenArtwork(artwork) },
                        onDelete = { onDelete(artwork.id) }
                    )
                }
                item(key = "start-new") {
                    StartNewArtTabletTile(onClick = onStartNewArt)
                }
            }
        }
    }
}

@Composable
private fun GalleryTabletCard(
    artwork: GalleryArtwork,
    onOpen: () -> Unit,
    onDelete: () -> Unit
) {
    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.85f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(artwork.placeholderTint)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🎨", fontSize = 56.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = artwork.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTokens.HeadingInk
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = artwork.dateLabel,
                fontSize = 13.sp,
                color = BrandTokens.MutedInk
            )
            Spacer(Modifier.height(10.dp))
            androidx.compose.foundation.layout.Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    onClick = onOpen,
                    shape = RoundedCornerShape(50),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier
                        .weight(1f)
                        .height(34.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxSize()
                    ) {
                        Text(
                            text = "Open",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    onClick = onDelete,
                    shape = CircleShape,
                    color = Color(0xFFFADADA),
                    modifier = Modifier.size(34.dp)
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "Delete",
                            tint = Color(0xFFC62828),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StartNewArtTabletTile(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.primaryContainer,
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.7f)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(34.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Start New Art",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GalleryContent(
    state: GalleryUiState,
    onStartNewArt: () -> Unit,
    onOpenArtwork: (GalleryArtwork) -> Unit,
    onDelete: (String) -> Unit,
    onFilter: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = safeTop,
                bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item { ParentBrandHeader() }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BrandHeading(
                        text = "My Magical\nCreations",
                        fontSize = 28.sp,
                        lineHeight = 34.sp,
                        modifier = Modifier.weight(1f)
                    )
                    FilterPill(label = "Filter", onClick = onFilter)
                }
            }

            items(state.artworks, key = { it.id }) { artwork ->
                ArtworkCard(
                    artwork = artwork,
                    onOpen = { onOpenArtwork(artwork) },
                    onDelete = { onDelete(artwork.id) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item {
                StartNewArtCard(
                    onClick = onStartNewArt,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun StartNewArtCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        onClick = onClick,
        fill = MaterialTheme.colorScheme.primary,
        edge = BrandTokens.PrimaryEdge,
        shape = RoundedCornerShape(28.dp),
        height = 200.dp,
        edgeThickness = 10.dp,
        contentColor = Color.White,
        modifier = modifier
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(20.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Start New Art",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(name = "Gallery – phone", showBackground = true, widthDp = 360, heightDp = 1300)
@Composable
private fun GalleryPreviewPhone() {
    ColorMagicKidsTheme {
        GalleryContent(
            state = GalleryUiState(),
            onStartNewArt = {},
            onOpenArtwork = {},
            onDelete = {},
            onFilter = {}
        )
    }
}
