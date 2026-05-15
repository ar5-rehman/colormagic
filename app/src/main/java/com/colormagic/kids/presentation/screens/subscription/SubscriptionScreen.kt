package com.colormagic.kids.presentation.screens.subscription

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.PlayArrow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.ParentBrandHeader
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun SubscriptionScreen(
    onBack: () -> Unit,
    onPurchaseSuccessful: () -> Unit = onBack,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    val onContinue: () -> Unit = {
        viewModel.onContinue()
        // For now we simulate a successful Play Billing handoff by routing
        // straight to the success screen. Real billing wiring goes in the VM.
        onPurchaseSuccessful()
    }
    if (info.isCompactWidth) {
        SubscriptionContent(
            state = state,
            onBack = onBack,
            onPlanSelected = viewModel::onPlanSelected,
            onContinue = onContinue
        )
    } else {
        SubscriptionTabletContent(
            state = state,
            onBack = onBack,
            onPlanSelected = viewModel::onPlanSelected,
            onContinue = onContinue
        )
    }
}

@Composable
private fun SubscriptionTabletContent(
    state: SubscriptionUiState,
    onBack: () -> Unit,
    onPlanSelected: (PlanTier) -> Unit,
    onContinue: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ParentBrandHeader(onBack = onBack)
            Spacer(Modifier.height(12.dp))
            ForParentsOnlyPill()
            Spacer(Modifier.height(14.dp))
            BrandHeading(
                text = "Choose Your Magic",
                fontSize = 38.sp,
                lineHeight = 46.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            // Three plans side by side
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                state.plans.forEach { plan ->
                    Box(modifier = Modifier.weight(1f)) {
                        PlanCard(
                            plan = plan,
                            selected = plan.id == state.selectedPlanId,
                            onSelect = { onPlanSelected(plan.id) }
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            Box(modifier = Modifier.fillMaxWidth(fraction = 0.5f)) {
                ContinueWithGooglePlayButton(onClick = onContinue)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Payment will be charged to your Google Play account at confirmation of purchase.",
                fontSize = 12.sp,
                lineHeight = 18.sp,
                color = BrandTokens.MutedInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(fraction = 0.6f)
            )
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SubscriptionContent(
    state: SubscriptionUiState,
    onBack: () -> Unit,
    onPlanSelected: (PlanTier) -> Unit,
    onContinue: () -> Unit
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { ParentBrandHeader(onBack = onBack) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ForParentsOnlyPill()
                    Spacer(Modifier.height(14.dp))
                    BrandHeading(
                        text = "Magic Passes",
                        fontSize = 36.sp,
                        lineHeight = 42.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Keep the creativity flowing. Choose a plan to unlock more magical sketches for your little artist.",
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = BrandTokens.MutedInk,
                        textAlign = TextAlign.Center
                    )
                }
            }

            items(state.plans, key = { it.id }) { plan ->
                PlanCard(
                    plan = plan,
                    selected = plan.id == state.selectedPlanId,
                    onSelect = { onPlanSelected(plan.id) },
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item { Spacer(Modifier.height(8.dp)) }

            item {
                ContinueWithGooglePlayButton(
                    onClick = onContinue,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
            item {
                Text(
                    text = "Payment will be charged to your Google Play account at confirmation of purchase.",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                    color = BrandTokens.MutedInk,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun ForParentsOnlyPill() {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFB7E0F2)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Lock,
                contentDescription = null,
                tint = Color(0xFF055680),
                modifier = Modifier.size(15.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "For Parents Only",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF055680)
            )
        }
    }
}

// ─── Plan cards ────────────────────────────────────────────────────

private data class PlanPalette(
    val container: Color,
    val accent: Color,
    val titleInk: Color,
    val bodyInk: Color,
    val featureInk: Color,
    val featureCheckBg: Color,
    val featureCheckTint: Color
)

private fun paletteFor(plan: SubscriptionPlan, selected: Boolean): PlanPalette = when (plan.id) {
    PlanTier.Starter -> PlanPalette(
        container = Color.White,
        accent = BrandTokens.MutedInk,
        titleInk = BrandTokens.HeadingInk,
        bodyInk = BrandTokens.MutedInk,
        featureInk = BrandTokens.HeadingInk,
        featureCheckBg = Color(0xFFEDE7F6),
        featureCheckTint = Color(0xFF6F4FB0)
    )
    PlanTier.Unlimited -> PlanPalette(
        container = Color(0xFF7E66A6),
        accent = Color.White,
        titleInk = Color.White,
        bodyInk = Color(0xFFE7DBFB),
        featureInk = Color.White,
        featureCheckBg = Color(0xFFB7E1B9),
        featureCheckTint = Color(0xFF1B3A1F)
    )
    PlanTier.Refill -> PlanPalette(
        container = Color(0xFFC5E6F6),
        accent = Color(0xFF055680),
        titleInk = Color(0xFF055680),
        bodyInk = Color(0xFF055680),
        featureInk = Color(0xFF055680),
        featureCheckBg = Color(0xFF055680),
        featureCheckTint = Color.White
    )
}

@Composable
private fun PlanCard(
    plan: SubscriptionPlan,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = paletteFor(plan, selected)
    val borderColor = if (selected && plan.id != PlanTier.Starter) palette.accent
    else BrandTokens.SubtleOutline

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = palette.container,
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                PlanIcon(plan = plan)
                Spacer(Modifier.weight(1f))
                if (plan.isBestValue) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF3F6147)
                    ) {
                        Text(
                            text = "Best Value",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = plan.name,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = palette.titleInk
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = plan.tagline,
                fontSize = 14.sp,
                color = palette.bodyInk
            )

            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = plan.price,
                    fontSize = 38.sp,
                    lineHeight = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = palette.titleInk
                )
                if (plan.priceSuffix != null) {
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = plan.priceSuffix,
                        fontSize = 16.sp,
                        color = palette.bodyInk,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            plan.features.forEach { feature ->
                FeatureLine(
                    text = feature,
                    checkBg = palette.featureCheckBg,
                    checkTint = palette.featureCheckTint,
                    inkColor = palette.featureInk
                )
                Spacer(Modifier.height(6.dp))
            }

            Spacer(Modifier.height(14.dp))

            when {
                plan.isCurrent -> CurrentPlanChip()
                else -> SelectPlanIndicator(
                    selected = selected,
                    label = if (plan.id == PlanTier.Refill) "Select Pack" else "Select Plan",
                    accent = palette.accent,
                    inkColor = palette.titleInk
                )
            }
        }
    }
}

@Composable
private fun PlanIcon(plan: SubscriptionPlan) {
    val (icon, container, tint) = when (plan.id) {
        PlanTier.Starter -> Triple(Icons.Filled.ChildCare, BrandTokens.SubtleSurface, BrandTokens.MutedInk)
        PlanTier.Unlimited -> Triple(Icons.Filled.Bookmark, Color(0xFFA48EC9), Color.White)
        PlanTier.Refill -> Triple(Icons.Filled.Add, Color(0xFF055680), Color.White)
    }
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(container),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun FeatureLine(
    text: String,
    checkBg: Color,
    checkTint: Color,
    inkColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
                .background(checkBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = checkTint,
                modifier = Modifier.size(12.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = text,
            fontSize = 15.sp,
            color = inkColor
        )
    }
}

@Composable
private fun CurrentPlanChip() {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandTokens.SubtleSurface,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Current Plan",
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandTokens.MutedInk,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun SelectPlanIndicator(
    selected: Boolean,
    label: String,
    accent: Color,
    inkColor: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (selected) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF3F6147)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Circle,
                    contentDescription = null,
                    tint = Color(0xFFB7E1B9),
                    modifier = Modifier.size(10.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color.Transparent)
                    .padding(2.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accent.copy(alpha = 0.4f),
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = inkColor
        )
    }
}

// ─── Continue button ───────────────────────────────────────────────

@Composable
private fun ContinueWithGooglePlayButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color(0xFF1A1B25),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Filled.PlayArrow,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Continue with Google Play",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Preview(name = "Subscription – phone", showBackground = true, widthDp = 360, heightDp = 1300)
@Composable
private fun SubscriptionPreviewPhone() {
    ColorMagicKidsTheme {
        SubscriptionContent(
            state = SubscriptionUiState(),
            onBack = {},
            onPlanSelected = {},
            onContinue = {}
        )
    }
}
