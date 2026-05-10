package com.colormagic.kids.presentation.screens.createsketch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.model.ColoringIdea
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandPromptInput
import com.colormagic.kids.presentation.components.CreditPill
import com.colormagic.kids.presentation.components.CreditPillStyle
import com.colormagic.kids.presentation.components.IdeaCard
import com.colormagic.kids.presentation.components.SectionTitle
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun CreateSketchScreen(
    onMakeSketchRequested: (prompt: String) -> Unit,
    viewModel: CreateSketchViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    CreateSketchContent(
        state = state,
        onPromptChanged = viewModel::onPromptChanged,
        onMakeSketch = {
            // Validation: only fire when the prompt has real content.
            // The button is also disabled in that state for visual feedback,
            // this is a defence-in-depth check.
            if (state.prompt.isNotBlank()) {
                viewModel.onMakeSketch()
                onMakeSketchRequested(state.prompt.trim())
            }
        },
        onIdeaSelected = viewModel::onIdeaSelected
    )
}

@Composable
private fun CreateSketchContent(
    state: CreateSketchUiState,
    onPromptChanged: (String) -> Unit,
    onMakeSketch: () -> Unit,
    onIdeaSelected: (ColoringIdea) -> Unit
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
                BrandPrimaryButton(
                    label = "Make My Sketch",
                    onClick = onMakeSketch,
                    leadingIcon = Icons.Filled.AutoFixHigh,
                    enabled = state.prompt.isNotBlank()
                )
                Spacer(Modifier.height(12.dp))
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    CreditPill(
                        text = "Uses ${state.sketchCreditsCost} sketch credit",
                        style = CreditPillStyle.Subtle
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
            item {
                SectionTitle(
                    text = "Need ideas?",
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

@Preview(name = "Create Sketch – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun CreateSketchPreviewPhone() {
    ColorMagicKidsTheme {
        CreateSketchContent(
            state = CreateSketchUiState(),
            onPromptChanged = {},
            onMakeSketch = {},
            onIdeaSelected = {}
        )
    }
}

@Preview(name = "Create Sketch – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun CreateSketchPreviewTablet() {
    ColorMagicKidsTheme {
        CreateSketchContent(
            state = CreateSketchUiState(),
            onPromptChanged = {},
            onMakeSketch = {},
            onIdeaSelected = {}
        )
    }
}
