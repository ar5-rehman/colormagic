package com.colormagic.kids.presentation.screens.subscription

import android.app.Activity
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
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
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
    dismissAsClose: Boolean = false,
    viewModel: SubscriptionViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    val context = LocalContext.current

    // The parent already has an anonymous Firebase identity (created at
    // launch), so Continue goes straight to billing — no sign-in step.
    // launchBillingFlow needs the host Activity.
    val onContinue: () -> Unit = {
        (context as? Activity)?.let { activity ->
            viewModel.onContinue(activity, onCompleted = onPurchaseSuccessful)
        }
    }

    val dismissIcon = if (dismissAsClose) Icons.Filled.Close else Icons.AutoMirrored.Filled.ArrowBack
    val dismissLabel = if (dismissAsClose) "Close" else "Back"

    if (info.isCompactWidth) {
        SubscriptionContent(
            state = state,
            onBack = onBack,
            onPlanSelected = viewModel::onPlanSelected,
            onContinue = onContinue,
            dismissIcon = dismissIcon,
            dismissLabel = dismissLabel
        )
    } else {
        SubscriptionTabletContent(
            state = state,
            onBack = onBack,
            onPlanSelected = viewModel::onPlanSelected,
            onContinue = onContinue,
            dismissIcon = dismissIcon,
            dismissLabel = dismissLabel
        )
    }
}

@Composable
private fun SubscriptionTabletContent(
    state: SubscriptionUiState,
    onBack: () -> Unit,
    onPlanSelected: (PlanTier) -> Unit,
    onContinue: () -> Unit,
    dismissIcon: ImageVector,
    dismissLabel: String
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
            ParentBrandHeader(
                onBack = onBack,
                backIcon = dismissIcon,
                backContentDescription = dismissLabel
            )
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

            Spacer(Modifier.height(20.dp))

            Box(modifier = Modifier.fillMaxWidth(fraction = 0.5f)) {
                ContinueWithGooglePlayButton(
                    label = if (state.isProcessing) "Please wait…" else "Continue with Google Play",
                    isProcessing = state.isProcessing,
                    onClick = onContinue
                )
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
            if (state.errorMessage != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = state.errorMessage,
                    fontSize = 12.sp,
                    color = Color(0xFFC62828),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(fraction = 0.6f)
                )
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun SubscriptionContent(
    state: SubscriptionUiState,
    onBack: () -> Unit,
    onPlanSelected: (PlanTier) -> Unit,
    onContinue: () -> Unit,
    dismissIcon: ImageVector,
    dismissLabel: String
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
            item {
                ParentBrandHeader(
                    onBack = onBack,
                    backIcon = dismissIcon,
                    backContentDescription = dismissLabel
                )
            }

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
                    label = if (state.isProcessing) "Please wait…" else "Continue with Google Play",
                    isProcessing = state.isProcessing,
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
            if (state.errorMessage != null) {
                item {
                    Text(
                        text = state.errorMessage,
                        fontSize = 12.sp,
                        color = Color(0xFFC62828),
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp)
                    )
                }
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
    val isSelectable = !plan.isCurrent

    // Animated visual feedback for selection — border thickens, card lifts
    // and scales up a hair, shadow gets tinted with the accent colour.
    val borderWidth by animateDpAsState(
        targetValue = if (selected && isSelectable) 3.dp else 1.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "borderWidth"
    )
    val elevation by animateDpAsState(
        targetValue = if (selected && isSelectable) 14.dp else 0.dp,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "elevation"
    )
    val scale by animateFloatAsState(
        targetValue = if (selected && isSelectable) 1.02f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "scale"
    )

    val borderColor = when {
        plan.isCurrent -> BrandTokens.SubtleOutline
        selected -> palette.accent
        else -> BrandTokens.SubtleOutline
    }
    val shadowTint = if (selected && isSelectable) palette.accent else Color.Transparent

    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(22.dp),
        color = palette.container,
        border = BorderStroke(borderWidth, borderColor),
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .shadow(
                elevation = elevation,
                shape = RoundedCornerShape(22.dp),
                ambientColor = shadowTint,
                spotColor = shadowTint
            )
    ) {
        Box {
            // The "SELECTED" corner badge appears only on the currently-active card.
            // Animated alpha keeps the swap from being abrupt.
            val badgeAlpha by animateFloatAsState(
                targetValue = if (selected && isSelectable) 1f else 0f,
                label = "badgeAlpha"
            )
            if (badgeAlpha > 0f) {
                SelectedCornerBadge(
                    accent = palette.accent,
                    onAccent = palette.container,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .scale(badgeAlpha)
                )
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    PlanIcon(plan = plan)
                    Spacer(Modifier.weight(1f))
                    if (plan.isBestValue && !selected) {
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
                    } else if (selected && isSelectable) {
                        // Reserve the slot so the corner badge has room to render.
                        Spacer(Modifier.size(28.dp))
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
                    else -> SelectionPill(
                        selected = selected,
                        label = if (plan.id == PlanTier.Refill) "Select Pack" else "Select Plan",
                        accent = palette.accent,
                        onAccent = palette.container,
                        inactiveInk = palette.titleInk
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectedCornerBadge(
    accent: Color,
    onAccent: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .background(accent),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = "Selected",
            tint = onAccent,
            modifier = Modifier.size(18.dp)
        )
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
private fun SelectionPill(
    selected: Boolean,
    label: String,
    accent: Color,
    onAccent: Color,
    inactiveInk: Color
) {
    // Selected: filled pill in the plan accent — reads as "this is locked in".
    // Not selected: outlined hollow pill — reads as "tap me to choose".
    val container = if (selected) accent else Color.Transparent
    val ink = if (selected) onAccent else inactiveInk.copy(alpha = 0.85f)
    val border = if (selected) BorderStroke(0.dp, Color.Transparent)
    else BorderStroke(1.5.dp, inactiveInk.copy(alpha = 0.35f))

    Surface(
        shape = RoundedCornerShape(50),
        color = container,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (selected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = ink,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Selected",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = ink
                )
            } else {
                Text(
                    text = label,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = ink
                )
            }
        }
    }
}

// ─── Continue button ───────────────────────────────────────────────

@Composable
private fun ContinueWithGooglePlayButton(
    label: String,
    isProcessing: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        enabled = !isProcessing,
        shape = RoundedCornerShape(50),
        color = if (isProcessing) Color(0xFF1A1B25).copy(alpha = 0.7f) else Color(0xFF1A1B25),
        modifier = modifier
            .fillMaxWidth()
            .height(60.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isProcessing) {
                androidx.compose.material3.CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = label,
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
            onContinue = {},
            dismissIcon = Icons.AutoMirrored.Filled.ArrowBack,
            dismissLabel = "Back"
        )
    }
}
