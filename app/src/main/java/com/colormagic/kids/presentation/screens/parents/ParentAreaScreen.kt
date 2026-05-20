package com.colormagic.kids.presentation.screens.parents

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.auth.AuthViewModel
import com.colormagic.kids.presentation.components.AccountCard
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.domain.model.SketchLimit
import com.colormagic.kids.presentation.components.ParentBrandHeader
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun ParentAreaScreen(
    onManageSubscription: () -> Unit,
    viewModel: ParentAreaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    val user by authViewModel.authState.collectAsStateWithLifecycle()

    // Refresh credit count + plan label on every resume — a sketch made
    // (which deducts a credit) or a purchase (which adds them) should
    // reflect immediately when the parent comes back to this screen.
    androidx.lifecycle.compose.LifecycleEventEffect(
        androidx.lifecycle.Lifecycle.Event.ON_RESUME
    ) {
        viewModel.refreshQuota()
    }

    val accountCard: @Composable (Modifier) -> Unit = { modifier ->
        AccountCard(
            user = user,
            onSignOut = authViewModel::signOut,
            modifier = modifier
        )
    }

    if (info.isCompactWidth) {
        ParentAreaContent(
            state = state,
            onManageSubscription = onManageSubscription,
            onBuyMore = onManageSubscription,
            onSketchLimitChanged = viewModel::onSketchLimitChanged,
            onAllowFreeTextPromptsChanged = viewModel::onAllowFreeTextPromptsChanged,
            onClearArtwork = viewModel::onClearArtwork,
            accountCard = accountCard
        )
    } else {
        ParentAreaTabletContent(
            state = state,
            onManageSubscription = onManageSubscription,
            onBuyMore = onManageSubscription,
            onSketchLimitChanged = viewModel::onSketchLimitChanged,
            onAllowFreeTextPromptsChanged = viewModel::onAllowFreeTextPromptsChanged,
            onClearArtwork = viewModel::onClearArtwork,
            accountCard = accountCard
        )
    }
}

@Composable
private fun ParentAreaTabletContent(
    state: ParentAreaUiState,
    onManageSubscription: () -> Unit,
    onBuyMore: () -> Unit,
    onSketchLimitChanged: (SketchLimit) -> Unit,
    onAllowFreeTextPromptsChanged: (Boolean) -> Unit,
    onClearArtwork: () -> Unit,
    accountCard: @Composable (Modifier) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp, vertical = 24.dp)
        ) {
            // Welcome heading
            BrandHeading(
                text = "Welcome to the Parent Area",
                fontSize = 30.sp,
                lineHeight = 36.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Manage your child's magical creative journey here.",
                fontSize = 15.sp,
                color = BrandTokens.MutedInk
            )

            Spacer(Modifier.height(20.dp))

            // Two columns of cards
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    accountCard(Modifier)
                    CurrentPlanCard(planName = state.planName, onManage = onManageSubscription)
                    SparkleCreditsCard(credits = state.sparkleCredits, onBuyMore = onBuyMore)
                    PrivacyCard()
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ChildSafetyCard(
                        sketchLimit = state.sketchLimit,
                        onSketchLimitChanged = onSketchLimitChanged,
                        allowFreeTextPrompts = state.allowFreeTextPrompts,
                        onAllowFreeTextPromptsChanged = onAllowFreeTextPromptsChanged
                    )
                    ClearArtworkCard(onDelete = onClearArtwork)
                }
            }
        }
    }
}

@Composable
private fun ParentAreaContent(
    state: ParentAreaUiState,
    onManageSubscription: () -> Unit,
    onBuyMore: () -> Unit,
    onSketchLimitChanged: (SketchLimit) -> Unit,
    onAllowFreeTextPromptsChanged: (Boolean) -> Unit,
    onClearArtwork: () -> Unit,
    accountCard: @Composable (Modifier) -> Unit
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { ParentBrandHeader(showProfile = false) }

            item { accountCard(Modifier.padding(horizontal = 16.dp)) }

            item {
                CurrentPlanCard(
                    planName = state.planName,
                    onManage = onManageSubscription,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                SparkleCreditsCard(
                    credits = state.sparkleCredits,
                    onBuyMore = onBuyMore,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                ChildSafetyCard(
                    sketchLimit = state.sketchLimit,
                    onSketchLimitChanged = onSketchLimitChanged,
                    allowFreeTextPrompts = state.allowFreeTextPrompts,
                    onAllowFreeTextPromptsChanged = onAllowFreeTextPromptsChanged,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                PrivacyCard(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                ClearArtworkCard(
                    onDelete = onClearArtwork,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

// ─── Card: Current Plan ─────────────────────────────────────────────

@Composable
private fun CurrentPlanCard(
    planName: String,
    onManage: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        Text(
            text = "Current Plan",
            fontSize = 15.sp,
            color = BrandTokens.MutedInk
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = planName,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = onManage,
            shape = RoundedCornerShape(50),
            color = BrandTokens.SubtleSurface
        ) {
            Text(
                text = "Manage Subscription",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.HeadingInk,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

// ─── Card: Sparkle Credits ─────────────────────────────────────────

@Composable
private fun SparkleCreditsCard(
    credits: Int?,
    onBuyMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFC4B0E7),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Sparkle Credits",
                fontSize = 15.sp,
                color = Color(0xFF3D2A6E)
            )
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                // While the first quota fetch is still in flight we show an
                // ellipsis rather than a misleading hardcoded number.
                Text(
                    text = credits?.let { "%,d".format(it) } ?: "…",
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF231148)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "left",
                    fontSize = 16.sp,
                    color = Color(0xFF3D2A6E),
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            BrandPrimaryButton(
                label = "Buy More",
                onClick = onBuyMore,
                leadingIcon = Icons.Filled.Add,
                height = 52.dp,
                edgeThickness = 6.dp
            )
        }
    }
}

// ─── Card: Child Safety ────────────────────────────────────────────

@Composable
private fun ChildSafetyCard(
    sketchLimit: SketchLimit,
    onSketchLimitChanged: (SketchLimit) -> Unit,
    allowFreeTextPrompts: Boolean,
    onAllowFreeTextPromptsChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.ChildCare,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Child Safety Settings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(Modifier.height(18.dp))

        SettingsRowTitle(title = "Daily Sketch Limit", subtitle = "Controls how many prompts can be generated per day.")
        Spacer(Modifier.height(10.dp))
        SketchLimitPicker(selected = sketchLimit, onSelect = onSketchLimitChanged)

        Spacer(Modifier.height(20.dp))
        Divider()
        Spacer(Modifier.height(20.dp))

        ToggleRow(
            title = "Allow Free Text Prompts",
            subtitle = "If off, only picture-buttons can be used to create art.",
            checked = allowFreeTextPrompts,
            onCheckedChange = onAllowFreeTextPromptsChanged
        )
    }
}

@Composable
private fun SettingsRowTitle(title: String, subtitle: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = BrandTokens.HeadingInk
    )
    Spacer(Modifier.height(2.dp))
    Text(
        text = subtitle,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        color = BrandTokens.MutedInk
    )
}

@Composable
private fun SketchLimitPicker(
    selected: SketchLimit,
    onSelect: (SketchLimit) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = BrandTokens.SubtleSurface
    ) {
        Row(
            modifier = Modifier.padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            SketchLimit.entries.forEach { limit ->
                val isSelected = limit == selected
                Surface(
                    onClick = { onSelect(limit) },
                    shape = RoundedCornerShape(50),
                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 8.dp)
                    ) {
                        if (limit == SketchLimit.Unlimited) {
                            Icon(
                                imageVector = Icons.Filled.AllInclusive,
                                contentDescription = "Unlimited",
                                tint = if (isSelected) Color.White else BrandTokens.HeadingInk,
                                modifier = Modifier.size(18.dp)
                            )
                        } else {
                            Text(
                                text = limit.label,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) Color.White else BrandTokens.HeadingInk
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    leadingIcon: ImageVector? = null
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk
                )
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                color = BrandTokens.MutedInk
            )
        }
        Spacer(Modifier.width(12.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = BrandTokens.SubtleOutline
            )
        )
    }
}

// ─── Card: Privacy ─────────────────────────────────────────────────

@Composable
private fun PrivacyCard(modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFA9C690),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF2D3E2A)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = Color(0xFFE8F5E9),
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Privacy & Security Focus",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B3A1F)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Your child's creations are completely private. We do not use generated artwork to train public AI models. All generated images are stored securely and only accessible via this device.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFF1B3A1F)
            )
        }
    }
}

// ─── Card: Clear Artwork ───────────────────────────────────────────

@Composable
private fun ClearArtworkCard(
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFADADA),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Clear Canvas History",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB0192C)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Permanently remove all saved artwork and history from this device.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = Color(0xFFB0192C)
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(50),
                color = Color(0xFFB0192C)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Delete All Artwork",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ─── Shared building blocks ────────────────────────────────────────

@Composable
private fun SettingsCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        modifier = modifier.fillMaxWidth(),
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandTokens.SubtleOutline)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            content()
        }
    }
}

@Composable
private fun Divider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(BrandTokens.SubtleOutline)
    )
}

@Preview(name = "Parent Area – phone", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun ParentAreaPreviewPhone() {
    ColorMagicKidsTheme {
        ParentAreaContent(
            state = ParentAreaUiState(),
            onManageSubscription = {},
            onBuyMore = {},
            onSketchLimitChanged = {},
            onAllowFreeTextPromptsChanged = {},
            onClearArtwork = {},
            accountCard = { modifier ->
                AccountCard(
                    user = null,
                    onSignOut = {},
                    modifier = modifier
                )
            }
        )
    }
}
