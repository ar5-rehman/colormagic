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
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Filter1
import androidx.compose.material.icons.filled.Flip
import androidx.compose.material.icons.filled.FormatColorFill
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ZoomOutMap
import androidx.compose.material.icons.outlined.AutoFixOff
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import androidx.compose.foundation.border

@Composable
fun ColoringScreen(
    onBack: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ColoringViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()

    val context = androidx.compose.ui.platform.LocalContext.current
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current
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
        state.palette.firstOrNull { it.id == id }?.let {
            tts.speak(it.name, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "color")
        }
        viewModel.onColorSelected(id)
    }
    val onToolSelected: (ColoringTool) -> Unit = { t -> tick(); viewModel.onToolSelected(t) }
    val onPaletteSelected: (String) -> Unit = { id -> tick(); viewModel.onPaletteSelected(id) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val densityScale = androidx.compose.ui.platform.LocalDensity.current.density
    val coroutineScope = androidx.compose.runtime.rememberCoroutineScope()
    val onSave = {
        if (!state.isSaving && canvasSize.width > 0 && canvasSize.height > 0) {
            coroutineScope.launch {
                val saved = viewModel.saveArtwork(canvasSize.width, canvasSize.height, densityScale)
                if (saved) onSaved()
            }
        }
        Unit
    }
    val onCanvasSizeChanged: (IntSize) -> Unit = {
        canvasSize = it
        viewModel.onCanvasSizeChanged(it.width, it.height)
    }

    val commonCallbacks = ColoringCallbacks(
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
        onCanvasSizeChanged = onCanvasSizeChanged,
        onToggleSymmetry = viewModel::onToggleSymmetry,
        onToggleColorByNumber = viewModel::onToggleColorByNumber,
        onStrokeWidthChanged = viewModel::onStrokeWidthChanged,
        onOpacityChanged = viewModel::onOpacityChanged,
        onZoomChanged = viewModel::onZoomChanged,
        onZoomIn = viewModel::onZoomIn,
        onZoomOut = viewModel::onZoomOut,
        onResetZoom = viewModel::onResetZoom,
        onPanLeft = viewModel::onPanLeft,
        onPanRight = viewModel::onPanRight,
        onPanUp = viewModel::onPanUp,
        onPanDown = viewModel::onPanDown,
        onEyedropperPick = viewModel::onEyedropperPick,
        onTextToolTap = viewModel::onTextToolTap,
        onTextDragStart = viewModel::onTextDragStart,
        onTextDragMove = viewModel::onTextDragMove,
        onTextDragEnd = viewModel::onTextDragEnd,
        onSubmitChallenge = viewModel::submitChallenge,
        onTextButtonClick = viewModel::onTextButtonClicked
    )

    if (info.isCompactWidth) {
        ColoringContent(state = state, cb = commonCallbacks)
    } else {
        ColoringTabletContent(state = state, cb = commonCallbacks)
    }

    // Overlays rendered AFTER content so they stack on top
    if (state.showTextDialog) {
        FancyTextInputSheet(
            selectedColor = state.selectedColor,
            selectedFont = state.selectedTextFont,
            onFontSelected = viewModel::onTextFontSelected,
            onDismiss = viewModel::onTextDialogDismiss,
            onConfirm = viewModel::onTextConfirmed
        )
    }

    val challengeScore = state.challengeScore
    if (state.showChallengeResult && challengeScore != null) {
        ChallengeResultOverlay(
            score = challengeScore,
            onDismiss = viewModel::dismissChallengeResult
        )
    }
}

private data class ColoringCallbacks(
    val onBack: () -> Unit,
    val onColorSelected: (String) -> Unit,
    val onToolSelected: (ColoringTool) -> Unit,
    val onPaletteSelected: (String) -> Unit,
    val onBrushSizeSelected: (BrushSize) -> Unit,
    val onStrokeFinished: (Stroke) -> Unit,
    val onUndo: () -> Unit,
    val onRedo: () -> Unit,
    val onClear: () -> Unit,
    val onSave: () -> Unit,
    val onCanvasSizeChanged: (IntSize) -> Unit,
    val onToggleSymmetry: () -> Unit,
    val onToggleColorByNumber: () -> Unit,
    val onStrokeWidthChanged: (Float) -> Unit,
    val onOpacityChanged: (Float) -> Unit,
    val onZoomChanged: (Float, Offset) -> Unit,
    val onZoomIn: () -> Unit,
    val onZoomOut: () -> Unit,
    val onResetZoom: () -> Unit,
    val onPanLeft: () -> Unit,
    val onPanRight: () -> Unit,
    val onPanUp: () -> Unit,
    val onPanDown: () -> Unit,
    val onEyedropperPick: (Float, Float) -> Unit,
    val onTextToolTap: (Float, Float) -> Unit,
    val onTextDragStart: (Float, Float) -> Boolean,
    val onTextDragMove: (Float, Float) -> Unit,
    val onTextDragEnd: () -> Unit,
    val onSubmitChallenge: () -> Unit,
    val onTextButtonClick: () -> Unit
)

@Composable
private fun FancyTextInputSheet(
    selectedColor: PaintColor,
    selectedFont: TextFont,
    onFontSelected: (TextFont) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }

    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            onClick = onDismiss,
            modifier = Modifier.fillMaxSize(),
            color = Color.Transparent
        ) {}
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Handle bar
                Box(
                    modifier = Modifier.width(40.dp).height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFFDDDDDD))
                )
                Spacer(Modifier.height(16.dp))

                Text("Add Your Text!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(16.dp))

                // Live preview
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xFFF8F4FF),
                    modifier = Modifier.fillMaxWidth().height(80.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        if (text.isNotEmpty()) {
                            Text(
                                text = text,
                                fontSize = 28.sp,
                                fontWeight = selectedFont.toFontWeight(),
                                color = Color(selectedColor.argb),
                                fontStyle = selectedFont.toFontStyle()
                            )
                        } else {
                            Text("Type something fun...", fontSize = 18.sp, color = Color(0xFFBBBBBB))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Font picker
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextFont.entries.forEach { font ->
                        val isSelected = font == selectedFont
                        Surface(
                            onClick = { onFontSelected(font) },
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color(0xFFF0ECFF),
                            modifier = Modifier.height(44.dp)
                        ) {
                            Box(
                                Modifier.padding(horizontal = 16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = font.label,
                                    fontSize = 14.sp,
                                    fontWeight = font.toFontWeight(),
                                    color = if (isSelected) Color.White else Color(0xFF5E35B1),
                                    fontStyle = font.toFontStyle()
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))

                // Text input
                OutlinedTextField(
                    value = text,
                    onValueChange = { if (it.length <= 30) text = it },
                    label = { Text("Your text") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("${text.length}/30", fontSize = 12.sp, color = BrandTokens.MutedInk, modifier = Modifier.align(Alignment.End))
                Spacer(Modifier.height(14.dp))

                // Action buttons
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFF0ECFF),
                        modifier = Modifier.weight(1f).height(50.dp)
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Cancel", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF5E35B1))
                        }
                    }
                    TactileSurface(
                        onClick = { onConfirm(text) },
                        fill = MaterialTheme.colorScheme.primary,
                        edge = BrandTokens.PrimaryEdge,
                        shape = RoundedCornerShape(50),
                        height = 50.dp,
                        edgeThickness = 5.dp,
                        contentColor = Color.White,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Add Text!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChallengeResultOverlay(
    score: ChallengeScore,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            onClick = {},
            shape = RoundedCornerShape(32.dp),
            color = Color.White,
            shadowElevation = 24.dp,
            modifier = Modifier.fillMaxWidth(0.85f)
        ) {
            Column(
                modifier = Modifier.padding(28.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(score.emoji, fontSize = 56.sp)
                Spacer(Modifier.height(12.dp))
                Text(score.title, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(12.dp))

                // Stars
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    repeat(5) { i ->
                        Icon(
                            imageVector = if (i < score.stars) Icons.Filled.Star else Icons.Filled.StarBorder,
                            contentDescription = null,
                            tint = if (i < score.stars) Color(0xFFFFB300) else Color(0xFFE0E0E0),
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))

                // Score breakdown
                ScoreBar(label = "Coverage", percent = score.coveragePercent, color = Color(0xFF66BB6A))
                Spacer(Modifier.height(8.dp))
                ScoreBar(label = "Stay in Lines", percent = score.lineRespectPercent, color = Color(0xFF42A5F5))
                Spacer(Modifier.height(8.dp))
                ScoreBar(label = "Color Variety", percent = (score.colorVariety * 100 / 8).coerceAtMost(100), color = Color(0xFFAB47BC))

                Spacer(Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF5F0FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFFB300), modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Score: ${score.totalScore}/100", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A347E))
                    }
                }

                Spacer(Modifier.height(20.dp))
                TactileSurface(
                    onClick = onDismiss,
                    fill = MaterialTheme.colorScheme.primary,
                    edge = BrandTokens.PrimaryEdge,
                    shape = RoundedCornerShape(50),
                    height = 50.dp,
                    edgeThickness = 5.dp,
                    contentColor = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Awesome!", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, percent: Int, color: Color) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandTokens.HeadingInk)
            Text("$percent%", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier.fillMaxWidth().height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color(0xFFF0F0F0))
        ) {
            val animatedWidth by animateFloatAsState(
                targetValue = percent / 100f,
                animationSpec = tween(800), label = "score"
            )
            Box(
                Modifier.fillMaxWidth(animatedWidth).fillMaxSize()
                    .clip(RoundedCornerShape(5.dp))
                    .background(color)
            )
        }
    }
}

@Composable
private fun ColoringTabletContent(
    state: ColoringUiState,
    cb: ColoringCallbacks
) {
    val selectedColor = state.selectedColor
    val fillableMask = state.fillableMask
    val onBack = cb.onBack; val onColorSelected = cb.onColorSelected; val onToolSelected = cb.onToolSelected
    val onPaletteSelected = cb.onPaletteSelected; val onBrushSizeSelected = cb.onBrushSizeSelected
    val onStrokeFinished = cb.onStrokeFinished; val onUndo = cb.onUndo; val onRedo = cb.onRedo
    val onClear = cb.onClear; val onSave = cb.onSave; val onCanvasSizeChanged = cb.onCanvasSizeChanged
    val onToggleSymmetry = cb.onToggleSymmetry; val onToggleColorByNumber = cb.onToggleColorByNumber
    val onStrokeWidthChanged = cb.onStrokeWidthChanged; val onOpacityChanged = cb.onOpacityChanged
    val onZoomChanged = cb.onZoomChanged; val onZoomIn = cb.onZoomIn; val onZoomOut = cb.onZoomOut
    val onResetZoom = cb.onResetZoom; val onEyedropperPick = cb.onEyedropperPick
    val onTextToolTap = cb.onTextToolTap

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
            // Left — canvas
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = Color.White,
                modifier = Modifier
                    .weight(0.68f)
                    .fillMaxHeight()
                    .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            ) {
                Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            onClick = onBack,
                            shape = CircleShape,
                            color = BrandTokens.SubtleSurface,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = BrandTokens.HeadingInk, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        ZoomButtonsRow(
                            zoomScale = state.zoomScale,
                            onZoomIn = onZoomIn,
                            onZoomOut = onZoomOut,
                            onResetZoom = onResetZoom,
                            onPanLeft = cb.onPanLeft,
                            onPanRight = cb.onPanRight,
                            onPanUp = cb.onPanUp,
                            onPanDown = cb.onPanDown
                        )
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
                        symmetryEnabled = state.symmetryEnabled,
                        colorByNumberEnabled = state.colorByNumberEnabled,
                        colorRegions = state.colorRegions,
                        zoomScale = state.zoomScale,
                        zoomOffset = state.zoomOffset,
                        onZoomChanged = onZoomChanged,
                        onEyedropperPick = onEyedropperPick,
                        onTextToolTap = onTextToolTap,
                        onTextDragStart = cb.onTextDragStart,
                        onTextDragMove = cb.onTextDragMove,
                        onTextDragEnd = cb.onTextDragEnd,
                        strokeWidthBase = state.strokeWidthBase,
                        opacity = state.opacity,
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
                            fontSize = 14.sp, color = BrandTokens.MutedInk,
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
                    .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000))
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    TabletBrushesGrid(tool = state.tool, onToolSelected = onToolSelected)
                    Spacer(Modifier.height(10.dp))
                    TabletActionsRow(tool = state.tool, onToolSelected = onToolSelected, onTextButtonClick = cb.onTextButtonClick)
                    Spacer(Modifier.height(18.dp))

                    Text("Colors", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.align(Alignment.CenterHorizontally))
                    Spacer(Modifier.height(10.dp))
                    PaletteSwitcherRow(selectedPaletteId = state.selectedPaletteId, onSelect = onPaletteSelected)
                    Spacer(Modifier.height(10.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.palette.chunked(4).forEach { rowColors ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
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

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally), verticalAlignment = Alignment.CenterVertically) {
                        BrushSize.entries.forEach { size ->
                            BrushSizeDot(size = size, selected = size == state.brushSize, onClick = { onBrushSizeSelected(size) })
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    SliderRow(label = "Size", value = state.strokeWidthBase, range = 2f..40f, onValueChange = onStrokeWidthChanged)
                    SliderRow(label = "Opacity", value = state.opacity, range = 0.1f..1f, onValueChange = onOpacityChanged)

                    Spacer(Modifier.height(12.dp))
                    ModeToggleRow(symmetryEnabled = state.symmetryEnabled, colorByNumberEnabled = state.colorByNumberEnabled, onToggleSymmetry = onToggleSymmetry, onToggleColorByNumber = onToggleColorByNumber)

                    Spacer(Modifier.height(14.dp))
                    UndoRedoRow(canUndo = state.canUndo, canRedo = state.canRedo, onUndo = onUndo, onRedo = onRedo)
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
        modifier = Modifier.fillMaxWidth().height(48.dp)
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.DeleteOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Clear", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Save Picture", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ColoringContent(
    state: ColoringUiState,
    cb: ColoringCallbacks
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    val scrollState = rememberScrollState()
    val selectedColor = state.selectedColor
    val onBack = cb.onBack; val onColorSelected = cb.onColorSelected; val onToolSelected = cb.onToolSelected
    val onPaletteSelected = cb.onPaletteSelected; val onBrushSizeSelected = cb.onBrushSizeSelected
    val onStrokeFinished = cb.onStrokeFinished; val onUndo = cb.onUndo; val onRedo = cb.onRedo
    val onClear = cb.onClear; val onSave = cb.onSave; val onCanvasSizeChanged = cb.onCanvasSizeChanged
    val onToggleSymmetry = cb.onToggleSymmetry; val onToggleColorByNumber = cb.onToggleColorByNumber
    val onStrokeWidthChanged = cb.onStrokeWidthChanged; val onOpacityChanged = cb.onOpacityChanged
    val onZoomChanged = cb.onZoomChanged; val onZoomIn = cb.onZoomIn; val onZoomOut = cb.onZoomOut
    val onResetZoom = cb.onResetZoom; val onEyedropperPick = cb.onEyedropperPick
    val onTextToolTap = cb.onTextToolTap

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = safeTop, bottom = safeBottom)) {
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
            )

            Column(modifier = Modifier.fillMaxSize().verticalScroll(scrollState)) {
                Spacer(Modifier.height(4.dp))

                Box(modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth()) {
                    SketchCanvasCard(
                        state = state,
                        onStrokeFinished = onStrokeFinished,
                        onCanvasSizeChanged = onCanvasSizeChanged,
                        onZoomChanged = onZoomChanged,
                        onEyedropperPick = onEyedropperPick,
                        onTextToolTap = onTextToolTap,
                        onTextDragStart = cb.onTextDragStart,
                        onTextDragMove = cb.onTextDragMove,
                        onTextDragEnd = cb.onTextDragEnd
                    )
                }

                Spacer(Modifier.height(6.dp))
                ZoomButtonsRow(
                    zoomScale = state.zoomScale,
                    onZoomIn = onZoomIn,
                    onZoomOut = onZoomOut,
                    onResetZoom = onResetZoom,
                    onPanLeft = cb.onPanLeft,
                    onPanRight = cb.onPanRight,
                    onPanUp = cb.onPanUp,
                    onPanDown = cb.onPanDown
                )

                Spacer(Modifier.height(10.dp))

                ModeToggleRow(
                    symmetryEnabled = state.symmetryEnabled,
                    colorByNumberEnabled = state.colorByNumberEnabled,
                    onToggleSymmetry = onToggleSymmetry,
                    onToggleColorByNumber = onToggleColorByNumber
                )

                Spacer(Modifier.height(8.dp))

                if (state.colorByNumberEnabled && state.colorRegions.isNotEmpty()) {
                    ColorByNumberLegend(regions = state.colorRegions, selectedColorId = state.selectedColorId, onSelect = onColorSelected)
                    Spacer(Modifier.height(8.dp))
                }

                BrushSizeRow(selected = state.brushSize, onSelect = onBrushSizeSelected)

                Spacer(Modifier.height(2.dp))

                // Sliders
                SliderRow(label = "Size", value = state.strokeWidthBase, range = 2f..40f, onValueChange = onStrokeWidthChanged)
                SliderRow(label = "Opacity", value = state.opacity, range = 0.1f..1f, onValueChange = onOpacityChanged)

                Spacer(Modifier.height(4.dp))

                Text(
                    text = "${state.tool.label} • ${selectedColor.name} • ${state.brushSize.label}",
                    fontSize = 15.sp, color = BrandTokens.MutedInk,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Spacer(Modifier.height(10.dp))

                PaletteSwitcherRow(selectedPaletteId = state.selectedPaletteId, onSelect = onPaletteSelected)
                Spacer(Modifier.height(8.dp))
                ColorPaletteRow(palette = state.palette, selectedId = state.selectedColorId, onSelect = onColorSelected)
                Spacer(Modifier.height(12.dp))
                BrushesRow(tool = state.tool, onToolSelected = onToolSelected)
                Spacer(Modifier.height(8.dp))
                ActionsRow(tool = state.tool, onToolSelected = onToolSelected, onTextButtonClick = cb.onTextButtonClick)
                Spacer(Modifier.height(10.dp))
                UndoRedoRow(canUndo = state.canUndo, canRedo = state.canRedo, onUndo = onUndo, onRedo = onRedo)
                Spacer(Modifier.height(12.dp))
                FooterRow(onClear = onClear, onSave = onSave)
                if (state.isChallenge) {
                    Spacer(Modifier.height(10.dp))
                    ChallengeSubmitButton(onClick = cb.onSubmitChallenge)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun SketchCanvasCard(
    state: ColoringUiState,
    onStrokeFinished: (Stroke) -> Unit,
    onCanvasSizeChanged: (IntSize) -> Unit = {},
    onZoomChanged: (Float, Offset) -> Unit = { _, _ -> },
    onEyedropperPick: (Float, Float) -> Unit = { _, _ -> },
    onTextToolTap: (Float, Float) -> Unit = { _, _ -> },
    onTextDragStart: (Float, Float) -> Boolean = { _, _ -> false },
    onTextDragMove: (Float, Float) -> Unit = { _, _ -> },
    onTextDragEnd: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.88f)
            .shadow(14.dp, RoundedCornerShape(28.dp), ambientColor = Color(0x14000000), spotColor = Color(0x14000000)),
        color = Color.White,
        shape = RoundedCornerShape(28.dp)
    ) {
        SketchCanvas(
            tool = state.tool,
            selectedColor = state.selectedColor,
            brushSize = state.brushSize,
            strokes = state.strokes,
            fillableMask = state.fillableMask,
            sketchImage = state.sketchImage,
            onStrokeFinished = onStrokeFinished,
            symmetryEnabled = state.symmetryEnabled,
            colorByNumberEnabled = state.colorByNumberEnabled,
            colorRegions = state.colorRegions,
            zoomScale = state.zoomScale,
            zoomOffset = state.zoomOffset,
            onZoomChanged = onZoomChanged,
            onEyedropperPick = onEyedropperPick,
            onTextToolTap = onTextToolTap,
            onTextDragStart = onTextDragStart,
            onTextDragMove = onTextDragMove,
            onTextDragEnd = onTextDragEnd,
            strokeWidthBase = state.strokeWidthBase,
            opacity = state.opacity,
            modifier = Modifier
                .padding(10.dp)
                .onSizeChanged(onCanvasSizeChanged)
        )
    }
}

@Composable
private fun ZoomButtonsRow(
    zoomScale: Float,
    onZoomIn: () -> Unit,
    onZoomOut: () -> Unit,
    onResetZoom: () -> Unit,
    onPanLeft: () -> Unit = {},
    onPanRight: () -> Unit = {},
    onPanUp: () -> Unit = {},
    onPanDown: () -> Unit = {}
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        // Zoom controls
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ZoomPillButton(label = "−", enabled = zoomScale > 1.05f, onClick = onZoomOut)
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = BrandTokens.SubtleSurface,
                modifier = Modifier.height(36.dp)
            ) {
                Box(Modifier.padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                    Text("${(zoomScale * 100).toInt()}%", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BrandTokens.HeadingInk)
                }
            }
            ZoomPillButton(label = "+", enabled = zoomScale < 4.95f, onClick = onZoomIn)
            if (zoomScale > 1.05f) {
                Surface(
                    onClick = onResetZoom,
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.height(36.dp)
                ) {
                    Box(Modifier.padding(horizontal = 10.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.ZoomOutMap, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }

        // Pan d-pad — visible only when zoomed, centered
        if (zoomScale > 1.05f) {
            Spacer(Modifier.height(8.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(108.dp)
            ) {
                PanArrowButton(label = "↑", onClick = onPanUp, modifier = Modifier.align(Alignment.TopCenter))
                PanArrowButton(label = "←", onClick = onPanLeft, modifier = Modifier.align(Alignment.CenterStart))
                PanArrowButton(label = "→", onClick = onPanRight, modifier = Modifier.align(Alignment.CenterEnd))
                PanArrowButton(label = "↓", onClick = onPanDown, modifier = Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}

@Composable
private fun ZoomPillButton(label: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (enabled) BrandTokens.SubtleSurface else Color(0xFFF0F0F0),
        modifier = Modifier.size(40.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 20.sp, fontWeight = FontWeight.Bold,
                color = if (enabled) BrandTokens.HeadingInk else Color(0xFFBBBBBB))
        }
    }
}

@Composable
private fun PanArrowButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = Color(0xFFE8E0F8),
        modifier = modifier.size(32.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5E35B1))
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandTokens.MutedInk,
            modifier = Modifier.width(56.dp)
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary
            )
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
            BrushSizeDot(size = size, selected = size == selected, onClick = { onSelect(size) })
            Spacer(Modifier.width(4.dp))
        }
    }
}

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
                color = if (selected) MaterialTheme.colorScheme.primary else BrandTokens.SubtleSurface
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

private val BRUSHES: List<Pair<ColoringTool, Int>> = listOf(
    ColoringTool.Crayon to R.drawable.ic_brush_crayon,
    ColoringTool.Marker to R.drawable.ic_brush_marker,
    ColoringTool.Pencil to R.drawable.ic_brush_pencil,
    ColoringTool.Watercolor to R.drawable.ic_brush_watercolor,
    ColoringTool.Highlighter to R.drawable.ic_brush_highlighter,
    ColoringTool.Magic to R.drawable.ic_brush_magic,
    ColoringTool.Glitter to R.drawable.ic_brush_glitter
)

@Composable
private fun BrushesRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
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
    onToolSelected: (ColoringTool) -> Unit,
    onTextButtonClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
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
        ToolButton(
            label = "Picker",
            icon = Icons.Filled.Colorize,
            selected = tool == ColoringTool.Eyedropper,
            onClick = { onToolSelected(ColoringTool.Eyedropper) },
            modifier = Modifier.weight(1f)
        )
        ToolButton(
            label = "Text",
            icon = Icons.Filled.TextFields,
            selected = tool == ColoringTool.TextTool,
            onClick = onTextButtonClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TabletBrushesGrid(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        BRUSHES.chunked(3).forEach { rowBrushes ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
                rowBrushes.forEach { (brushTool, iconRes) ->
                    ToolButton(
                        label = brushTool.label,
                        iconPainter = painterResource(iconRes),
                        selected = tool == brushTool,
                        onClick = { onToolSelected(brushTool) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat(3 - rowBrushes.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun TabletActionsRow(
    tool: ColoringTool,
    onToolSelected: (ColoringTool) -> Unit,
    onTextButtonClick: () -> Unit = {}
) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        ToolButton(label = "Fill", icon = Icons.Filled.FormatColorFill, selected = tool == ColoringTool.Fill, onClick = { onToolSelected(ColoringTool.Fill) }, modifier = Modifier.weight(1f))
        ToolButton(label = "Eraser", icon = Icons.Outlined.AutoFixOff, selected = tool == ColoringTool.Eraser, onClick = { onToolSelected(ColoringTool.Eraser) }, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(6.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)) {
        ToolButton(label = "Picker", icon = Icons.Filled.Colorize, selected = tool == ColoringTool.Eyedropper, onClick = { onToolSelected(ColoringTool.Eyedropper) }, modifier = Modifier.weight(1f))
        ToolButton(label = "Text", icon = Icons.Filled.TextFields, selected = tool == ColoringTool.TextTool, onClick = onTextButtonClick, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun UndoRedoRow(
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally)
    ) {
        ToolButton(label = "Undo", icon = Icons.AutoMirrored.Filled.Undo, selected = false, enabled = canUndo, onClick = onUndo, modifier = Modifier.weight(1f))
        ToolButton(label = "Redo", icon = Icons.AutoMirrored.Filled.Redo, selected = false, enabled = canRedo, onClick = onRedo, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ModeToggleRow(
    symmetryEnabled: Boolean,
    colorByNumberEnabled: Boolean,
    onToggleSymmetry: () -> Unit,
    onToggleColorByNumber: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally)
    ) {
        ModeToggleChip(label = "Mirror", icon = Icons.Filled.Flip, active = symmetryEnabled, onClick = onToggleSymmetry, modifier = Modifier.weight(1f))
        ModeToggleChip(label = "Color by #", icon = Icons.Filled.Filter1, active = colorByNumberEnabled, onClick = onToggleColorByNumber, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ModeToggleChip(
    label: String,
    icon: ImageVector,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bg = if (active) MaterialTheme.colorScheme.primary else BrandTokens.SubtleSurface
    val ink = if (active) Color.White else BrandTokens.HeadingInk
    val borderColor = if (active) MaterialTheme.colorScheme.primary else Color.Transparent

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        modifier = modifier.height(46.dp).border(1.5.dp, borderColor, RoundedCornerShape(14.dp))
    ) {
        Row(Modifier.fillMaxSize().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(icon, null, tint = ink, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ink)
        }
    }
}

@Composable
private fun ColorByNumberLegend(
    regions: List<ColorRegion>,
    selectedColorId: String,
    onSelect: (String) -> Unit
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(regions, key = { it.number }) { region ->
            Surface(
                onClick = { onSelect("cbn_${region.number}") },
                shape = RoundedCornerShape(12.dp),
                color = Color(region.assignedColor),
                modifier = Modifier.size(width = 52.dp, height = 52.dp)
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(shape = CircleShape, color = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(28.dp)) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("${region.number}", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF212121))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FooterRow(
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ClearChip(onClick = onClear)
        SavePictureButton(onClick = onSave, modifier = Modifier.weight(1f))
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
        Row(Modifier.padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DeleteOutline, null, tint = Color(0xFFC62828), modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text("Clear", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFC62828))
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
            Icon(Icons.Filled.Save, null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text("Save Picture", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}

@Composable
private fun ChallengeSubmitButton(onClick: () -> Unit) {
    TactileSurface(
        onClick = onClick,
        fill = Color(0xFFFF6D3F),
        edge = Color(0xFFCC4400),
        shape = RoundedCornerShape(50),
        height = 52.dp,
        edgeThickness = 6.dp,
        contentColor = Color.White,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Icon(Icons.Filled.EmojiEvents, null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(8.dp))
            Text("Submit Challenge!", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
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
        ColoringTool.Glitter -> "Glitter"
        ColoringTool.Eraser -> "Eraser"
        ColoringTool.Fill -> "Fill"
        ColoringTool.Eyedropper -> "Picker"
        ColoringTool.TextTool -> "Text"
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
            cb = ColoringCallbacks(
                onBack = {}, onColorSelected = {}, onToolSelected = {},
                onPaletteSelected = {}, onBrushSizeSelected = {},
                onStrokeFinished = {}, onUndo = {}, onRedo = {},
                onClear = {}, onSave = {}, onCanvasSizeChanged = {},
                onToggleSymmetry = {}, onToggleColorByNumber = {},
                onStrokeWidthChanged = {}, onOpacityChanged = {},
                onZoomChanged = { _, _ -> }, onZoomIn = {}, onZoomOut = {}, onResetZoom = {},
                onPanLeft = {}, onPanRight = {}, onPanUp = {}, onPanDown = {},
                onEyedropperPick = { _, _ -> }, onTextToolTap = { _, _ -> },
                onTextDragStart = { _, _ -> false }, onTextDragMove = { _, _ -> },
                onTextDragEnd = {}, onSubmitChallenge = {},
                onTextButtonClick = {}
            )
        )
    }
}
