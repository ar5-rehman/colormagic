package com.colormagic.kids.presentation.screens.coloring

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.ui.res.painterResource
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
import com.colormagic.kids.R
import com.colormagic.kids.domain.model.BrushSize
import com.colormagic.kids.domain.model.ColorPalettes
import com.colormagic.kids.domain.model.ColoringTool
import com.colormagic.kids.domain.model.PaintColor
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

    // ── Delight: light haptics + spoken color names (free for everyone) ──
    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
    // TextToSpeech instance, kept for the life of the screen and shut down on exit.
    val tts = remember {
        val ref = arrayOfNulls<android.speech.tts.TextToSpeech>(1)
        ref[0] = android.speech.tts.TextToSpeech(context) { status ->
            if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                ref[0]?.language = java.util.Locale.getDefault()
            }
        }
        ref[0]!!
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { tts.stop(); tts.shutdown() }
    }

    fun tick() = haptics.performHapticFeedback(
        androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress
    )

    val onColorSelected: (String) -> Unit = { id ->
        tick()
        // Say the color name out loud — kids learn their colors as they paint.
        state.palette.firstOrNull { it.id == id }?.let {
            tts.speak(it.name, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "color")
        }
        viewModel.onColorSelected(id)
    }
    val onToolSelected: (ColoringTool) -> Unit = { t -> tick(); viewModel.onToolSelected(t) }
    val onPaletteSelected: (String) -> Unit = { id -> tick(); viewModel.onPaletteSelected(id) }

    // The actual on-screen size of the canvas, in pixels. Captured by the
    // SketchCanvasCard via Modifier.onSizeChanged so the saved PNG matches
    // exactly what the kid sees — strokes were recorded in this coordinate
    // space, so re-rendering at the same dimensions keeps everything aligned.
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val onSave = {
        // Don't double-fire while a previous save is still running.
        if (!state.isSaving && canvasSize.width > 0 && canvasSize.height > 0) {
            coroutineScope.launch {
                val saved = viewModel.saveArtwork(canvasSize.width, canvasSize.height)
                if (saved) onSaved()
            }
        }
        Unit
    }
    if (info.isCompactWidth) {
        ColoringContent(
            state = state,
            onBack = onBack,
            onColorSelected = onColorSelected,
            onToolSelected = onToolSelected,
            onPaletteSelected = onPaletteSelected,
            onBrushSizeSelected = viewModel::onBrushSizeSelected,
            onStrokeFinished = viewModel::onStrokeFinished,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onClear = viewModel::onClear,
            onSave = onSave,
            onCanvasSizeChanged = { canvasSize = it }
        )
    } else {
        ColoringTabletContent(
            state = state,
            onBack = onBack,
            onColorSelected = onColorSelected,
            onToolSelected = onToolSelected,
            onPaletteSelected = onPaletteSelected,
            onBrushSizeSelected = viewModel::onBrushSizeSelected,
            onStrokeFinished = viewModel::onStrokeFinished,
            onUndo = viewModel::onUndo,
            onRedo = viewModel::onRedo,
            onClear = viewModel::onClear,
            onSave = onSave,
            onCanvasSizeChanged = { canvasSize = it }
        )
    }
}

@Composable
private fun ColoringTabletContent(
    state: ColoringUiState,
    onBack: () -> Unit,
    onColorSelected: (String) -> Unit,
    onToolSelected: (ColoringTool) -> Unit,
    onPaletteSelected: (String) -> Unit,
    onBrushSizeSelected: (BrushSize) -> Unit,
    onStrokeFinished: (Stroke) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit
) {
    val selectedColor = state.selectedColor
    val fillableMask = state.fillableMask

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
                    SketchCanvas(
                        tool = state.tool,
                        selectedColor = selectedColor,
                        brushSize = state.brushSize,
                        strokes = state.strokes,
                        fillableMask = fillableMask,
                        sketchImage = state.sketchImage,
                        onStrokeFinished = onStrokeFinished,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .onSizeChanged(onCanvasSizeChanged)
                    )
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
                    // Brush family — six paint styles in a 3×2 grid.
                    TabletBrushesGrid(
                        tool = state.tool,
                        onToolSelected = onToolSelected
                    )

                    Spacer(Modifier.height(10.dp))

                    // Action tools — Fill + Eraser, separate row so kids
                    // never confuse them with brushes.
                    TabletActionsRow(
                        tool = state.tool,
                        onToolSelected = onToolSelected
                    )

                    Spacer(Modifier.height(18.dp))

                    Text(
                        text = "Colors",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(10.dp))
                    PaletteSwitcherRow(
                        selectedPaletteId = state.selectedPaletteId,
                        onSelect = onPaletteSelected
                    )
                    Spacer(Modifier.height(10.dp))
                    // palette → 4 columns fits the right pane cleanly.
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.palette.chunked(4).forEach { rowColors ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
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

                    Spacer(Modifier.height(14.dp))

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

                    Spacer(Modifier.weight(1f))

                    // Actions — things that change HISTORY, not the active tool.
                    UndoRedoRow(
                        canUndo = state.canUndo,
                        canRedo = state.canRedo,
                        onUndo = onUndo,
                        onRedo = onRedo
                    )

                    Spacer(Modifier.height(14.dp))

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
    onPaletteSelected: (String) -> Unit,
    onBrushSizeSelected: (BrushSize) -> Unit,
    onStrokeFinished: (Stroke) -> Unit,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberScrollState()
    val selectedColor = state.selectedColor

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = safeTop, bottom = safeBottom)) {
            // Top bar stays pinned — back button always reachable.
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
                // 3-dot menu removed — clear/undo/redo already live in the
                // tools toolbar below, so the top-bar overflow had no unique
                // actions to host.
            )

            // The rest scrolls — canvas + tools + actions + footer all reachable
            // on small phones. Pointer events inside SketchCanvas consume drags so
            // drawing never accidentally scrolls the screen.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                Spacer(Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .fillMaxWidth()
                ) {
                    SketchCanvasCard(
                        tool = state.tool,
                        selectedColor = selectedColor,
                        brushSize = state.brushSize,
                        strokes = state.strokes,
                        fillableMask = state.fillableMask,
                        sketchImage = state.sketchImage,
                        onStrokeFinished = onStrokeFinished,
                        onCanvasSizeChanged = onCanvasSizeChanged
                    )
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

                PaletteSwitcherRow(
                    selectedPaletteId = state.selectedPaletteId,
                    onSelect = onPaletteSelected
                )

                Spacer(Modifier.height(10.dp))

                ColorPaletteRow(
                    palette = state.palette,
                    selectedId = state.selectedColorId,
                    onSelect = onColorSelected
                )

                Spacer(Modifier.height(16.dp))

                BrushesRow(
                    tool = state.tool,
                    onToolSelected = onToolSelected
                )

                Spacer(Modifier.height(10.dp))

                ActionsRow(
                    tool = state.tool,
                    onToolSelected = onToolSelected
                )

                Spacer(Modifier.height(12.dp))

                UndoRedoRow(
                    canUndo = state.canUndo,
                    canRedo = state.canRedo,
                    onUndo = onUndo,
                    onRedo = onRedo
                )

                Spacer(Modifier.height(16.dp))

                FooterRow(
                    onClear = onClear,
                    onSave = onSave
                )

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SketchCanvasCard(
    tool: ColoringTool,
    selectedColor: PaintColor,
    brushSize: BrushSize,
    strokes: List<Stroke>,
    fillableMask: androidx.compose.ui.graphics.ImageBitmap?,
    sketchImage: androidx.compose.ui.graphics.ImageBitmap?,
    onStrokeFinished: (Stroke) -> Unit,
    onCanvasSizeChanged: (androidx.compose.ui.unit.IntSize) -> Unit = {}
) {
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
        SketchCanvas(
            tool = tool,
            selectedColor = selectedColor,
            brushSize = brushSize,
            strokes = strokes,
            fillableMask = fillableMask,
            sketchImage = sketchImage,
            onStrokeFinished = onStrokeFinished,
            // Capture the on-screen pixel size — the save pipeline renders
            // the PNG at this exact size so stroke positions stay aligned.
            modifier = Modifier
                .padding(14.dp)
                .onSizeChanged(onCanvasSizeChanged)
        )
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

// Theme switcher — Classic / Pastel / Neon / Ocean. Changing the theme swaps
// the whole swatch set so kids can paint in different moods.
@Composable
private fun PaletteSwitcherRow(
    selectedPaletteId: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(ColorPalettes.all, key = { it.id }) { palette ->
            val selected = palette.id == selectedPaletteId
            Surface(
                onClick = { onSelect(palette.id) },
                shape = RoundedCornerShape(50),
                color = if (selected) MaterialTheme.colorScheme.primary
                else BrandTokens.SubtleSurface
            ) {
                Text(
                    text = palette.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (selected) Color.White else BrandTokens.HeadingInk,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
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

// The six brushes the kid can pick — same set on phone and tablet so the
// "what brushes do I have" mental model never changes. Each maps to a
// full-colour 3D vector drawable; order is sequenced simple → playful.
private val BRUSHES: List<Pair<ColoringTool, Int>> = listOf(
    ColoringTool.Crayon to R.drawable.ic_brush_crayon,
    ColoringTool.Marker to R.drawable.ic_brush_marker,
    ColoringTool.Pencil to R.drawable.ic_brush_pencil,
    ColoringTool.Watercolor to R.drawable.ic_brush_watercolor,
    ColoringTool.Highlighter to R.drawable.ic_brush_highlighter,
    ColoringTool.Magic to R.drawable.ic_brush_magic
)

@Composable
private fun BrushesRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
    // Horizontally scrollable on phone so all six brushes always fit even
    // on a 320dp-wide screen.
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(BRUSHES, key = { it.first }) { (brushTool, iconRes) ->
            ToolButton(
                label = brushTool.label,
                iconPainter = painterResource(iconRes),
                selected = tool == brushTool,
                onClick = { onToolSelected(brushTool) }
            )
        }
    }
}

@Composable
private fun ActionsRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
    // Action tools (Fill / Eraser) sit on their own row so kids never confuse
    // them with the brush family.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        ToolButton(
            label = "Fill",
            icon = Icons.Filled.FormatColorFill,
            selected = tool == ColoringTool.Fill,
            onClick = { onToolSelected(ColoringTool.Fill) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            label = "Eraser",
            icon = Icons.Outlined.AutoFixOff,
            selected = tool == ColoringTool.Eraser,
            onClick = { onToolSelected(ColoringTool.Eraser) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabletBrushesGrid(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
    // 3×2 grid in the right pane — comfortable touch targets, all six
    // brushes visible at once.
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BRUSHES.chunked(3).forEach { rowBrushes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
            ) {
                rowBrushes.forEach { (brushTool, iconRes) ->
                    ToolButton(
                        label = brushTool.label,
                        iconPainter = painterResource(iconRes),
                        selected = tool == brushTool,
                        onClick = { onToolSelected(brushTool) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Pad row if it's not a multiple of 3.
                repeat(3 - rowBrushes.size) {
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun TabletActionsRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
    ) {
        ToolButton(
            label = "Fill",
            icon = Icons.Filled.FormatColorFill,
            selected = tool == ColoringTool.Fill,
            onClick = { onToolSelected(ColoringTool.Fill) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            label = "Eraser",
            icon = Icons.Outlined.AutoFixOff,
            selected = tool == ColoringTool.Eraser,
            onClick = { onToolSelected(ColoringTool.Eraser) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun UndoRedoRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    // History actions live in their own row so kids don't confuse them with
    // tool selection. Each is enabled only when there's something to do —
    // ToolButton renders a muted look otherwise so tapping it doesn't feel
    // like a silent failure.
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        ToolButton(
            label = "Undo",
            icon = Icons.AutoMirrored.Filled.Undo,
            selected = false,
            enabled = canUndo,
            onClick = onUndo,
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            label = "Redo",
            icon = Icons.AutoMirrored.Filled.Redo,
            selected = false,
            enabled = canRedo,
            onClick = onRedo,
            modifier = Modifier.weight(1f)
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
        ColoringTool.Crayon -> "Crayon"
        ColoringTool.Marker -> "Marker"
        ColoringTool.Pencil -> "Pencil"
        ColoringTool.Watercolor -> "Watercolor"
        ColoringTool.Highlighter -> "Highlighter"
        ColoringTool.Magic -> "Magic"
        ColoringTool.Eraser -> "Eraser"
        ColoringTool.Fill -> "Fill"
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
            onPaletteSelected = {},
            onBrushSizeSelected = {},
            onStrokeFinished = {},
            onUndo = {},
            onRedo = {},
            onClear = {},
            onSave = {},
            onCanvasSizeChanged = {}
        )
    }
}
