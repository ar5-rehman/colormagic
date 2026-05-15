package com.colormagic.kids.presentation.screens.coloring

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.PaintColor
import com.colormagic.kids.domain.model.Sketch
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.BrandTopBar
import com.colormagic.kids.presentation.components.BrushSizeDot
import com.colormagic.kids.presentation.components.ColorSwatch
import com.colormagic.kids.presentation.components.TactileSurface
import com.colormagic.kids.presentation.components.ToolButton
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun ColoringScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ColoringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    val onSave = {
        viewModel.onSave()
        onSaved()
    }
    if (info.isCompactWidth) {
        ColoringContent(
            state = state,
            onBack = onBack,
            onColorSelected = viewModel::onColorSelected,
            onToolSelected = viewModel::onToolSelected,
            onBrushSizeSelected = viewModel::onBrushSizeSelected,
            onUndo = viewModel::onUndo,
            onClear = viewModel::onClear,
            onSave = onSave
        )
    } else {
        ColoringTabletContent(
            state = state,
            onBack = onBack,
            onColorSelected = viewModel::onColorSelected,
            onToolSelected = viewModel::onToolSelected,
            onBrushSizeSelected = viewModel::onBrushSizeSelected,
            onUndo = viewModel::onUndo,
            onClear = viewModel::onClear,
            onSave = onSave
        )
    }
}

@Composable
private fun ColoringTabletContent(
    state: ColoringUiState,
    onBack: () -> Unit,
    onColorSelected: (String) -> Unit,
    onToolSelected: (ColoringTool) -> Unit,
    onBrushSizeSelected: (BrushSize) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val selectedColor = state.palette.firstOrNull { it.id == state.selectedColorId }
        ?: state.palette.first()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Left — canvas card with back arrow + status label
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0x14000000),
                        spotColor = Color(0x14000000)
                    )
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Surface(
                        onClick = onBack,
                        shape = CircleShape,
                        color = BrandTokens.SubtleSurface,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = BrandTokens.HeadingInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color(state.sketch.placeholderTint)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "🖍️", fontSize = 140.sp)
                    }
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = BrandTokens.SubtleSurface,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = "${state.tool.label} • ${selectedColor.name} • ${state.brushSize.label}",
                            fontSize = 14.sp,
                            color = BrandTokens.MutedInk,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Right — tools panel
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                modifier = Modifier
                    .weight(0.32f)
                    .fillMaxHeight()
                    .shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(28.dp),
                        ambientColor = Color(0x14000000),
                        spotColor = Color(0x14000000)
                    )
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                    ) {
                        ToolButton(
                            label = "Brush",
                            icon = Icons.Filled.Brush,
                            selected = state.tool == ColoringTool.Brush,
                            onClick = { onToolSelected(ColoringTool.Brush) }
                        )
                        ToolButton(
                            label = "Eraser",
                            icon = Icons.Outlined.AutoFixOff,
                            selected = state.tool == ColoringTool.Eraser,
                            onClick = { onToolSelected(ColoringTool.Eraser) }
                        )
                        ToolButton(
                            label = "Undo",
                            icon = Icons.AutoMirrored.Filled.Undo,
                            selected = false,
                            onClick = onUndo
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Colors",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.palette.chunked(3).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
                            ) {
                                rowColors.forEach { paintColor ->
                                    ColorSwatch(
                                        color = Color(paintColor.argb),
                                        selected = paintColor.id == state.selectedColorId,
                                        onClick = { onColorSelected(paintColor.id) }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BrushSize.entries.forEach { size ->
                            BrushSizeDot(
                                size = size,
                                selected = size == state.brushSize,
                                onClick = { onBrushSizeSelected(size) }
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    TabletClearChip(onClick = onClear)
                    Spacer(Modifier.height(10.dp))
                    TabletSaveButton(onClick = onSave)
                }
            }
        }
    }
}

@Composable
private fun TabletClearChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFFFDE0E0),
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Clear",
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun TabletSaveButton(onClick: () -> Unit) {
    TactileSurface(
        onClick = onClick,
        fill = MaterialTheme.colorScheme.primary,
        edge = BrandTokens.PrimaryEdge,
        shape = RoundedCornerShape(50),
        height = 52.dp,
        edgeThickness = 6.dp,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Save Picture",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun ColoringContent(
    state: ColoringUiState,
    onBack: () -> Unit,
    onColorSelected: (String) -> Unit,
    onToolSelected: (ColoringTool) -> Unit,
    onBrushSizeSelected: (BrushSize) -> Unit,
    onUndo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    val selectedColor = state.palette.firstOrNull { it.id == state.selectedColorId }
        ?: state.palette.first()

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
                centered = true,
                title = {
                    Text(
                        text = "Color Your Picture",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
                    )
                },
                trailing = {
                    Icon(
                        imageVector = Icons.Filled.MoreVert,
                        contentDescription = "More",
                        tint = BrandTokens.HeadingInk
                    )
                }
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                SketchCanvasCard(state.sketch)
            }

            Spacer(Modifier.height(20.dp))

            BrushSizeRow(
                selected = state.brushSize,
                onSelect = onBrushSizeSelected
            )

            Spacer(Modifier.height(6.dp))

            Text(
                text = "${state.tool.label} • ${selectedColor.name} • ${state.brushSize.label}",
                fontSize = 15.sp,
                color = BrandTokens.MutedInk,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(Modifier.height(14.dp))

            ColorPaletteRow(
                palette = state.palette,
                selectedId = state.selectedColorId,
                onSelect = onColorSelected
            )

            Spacer(Modifier.height(16.dp))

            ToolRow(
                tool = state.tool,
                onToolSelected = onToolSelected,
                onUndo = onUndo
            )

            Spacer(Modifier.height(16.dp))

            FooterRow(
                onClear = onClear,
                onSave = onSave
            )

            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SketchCanvasCard(sketch: Sketch) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            ),
        color = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(
            modifier = Modifier
                .padding(14.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(sketch.placeholderTint))
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Swap to AsyncImage(sketch.imageUrl) when backend is wired.
            Text(
                text = "🖍️",
                fontSize = 96.sp
            )
        }
    }
}

@Composable
private fun BrushSizeRow(
    selected: BrushSize,
    onSelect: (BrushSize) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        BrushSize.entries.forEach { size ->
            BrushSizeDot(
                size = size,
                selected = size == selected,
                onClick = { onSelect(size) }
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@Composable
private fun ColorPaletteRow(
    palette: List<PaintColor>,
    selectedId: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(palette, key = { it.id }) { color ->
            ColorSwatch(
                color = Color(color.argb),
                selected = color.id == selectedId,
                onClick = { onSelect(color.id) }
            )
        }
    }
}

@Composable
private fun ToolRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit,
    onUndo: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        ToolButton(
            label = "Brush",
            icon = Icons.Filled.Brush,
            selected = tool == ColoringTool.Brush,
            onClick = { onToolSelected(ColoringTool.Brush) }
        )
        ToolButton(
            label = "Eraser",
            icon = Icons.Outlined.AutoFixOff,
            selected = tool == ColoringTool.Eraser,
            onClick = { onToolSelected(ColoringTool.Eraser) }
        )
        ToolButton(
            label = "Undo",
            icon = Icons.AutoMirrored.Filled.Undo,
            selected = false,
            onClick = onUndo
        )
    }
}

@Composable
private fun FooterRow(
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClearChip(onClick = onClear)
        SavePictureButton(
            onClick = onSave,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun ClearChip(onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFFFDE0E0),
        modifier = Modifier.height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Clear",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFC62828)
            )
        }
    }
}

@Composable
private fun SavePictureButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TactileSurface(
        onClick = onClick,
        fill = MaterialTheme.colorScheme.primary,
        edge = BrandTokens.PrimaryEdge,
        shape = RoundedCornerShape(50),
        height = 52.dp,
        edgeThickness = 6.dp,
        contentColor = Color.White,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 20.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Save,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Save Picture",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

private val ColoringTool.label: String
    get() = when (this) {
        ColoringTool.Brush -> "Brush"
        ColoringTool.Eraser -> "Eraser"
    }

private val BrushSize.label: String
    get() = when (this) {
        BrushSize.XSmall -> "X-Small"
        BrushSize.Small -> "Small"
        BrushSize.Medium -> "Medium"
        BrushSize.Large -> "Large"
    }

@Preview(name = "Coloring – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun ColoringPreviewPhone() {
    ColorMagicKidsTheme {
        ColoringContent(
            state = ColoringUiState(),
            onBack = {},
            onColorSelected = {},
            onToolSelected = {},
            onBrushSizeSelected = {},
            onUndo = {},
            onClear = {},
            onSave = {}
        )
    }
}
