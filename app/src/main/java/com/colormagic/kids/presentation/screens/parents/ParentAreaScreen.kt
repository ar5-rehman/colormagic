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
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ChildCare
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.presentation.util.AppLinks
import com.colormagic.kids.presentation.util.openUrl
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
    onOpenSupport: () -> Unit = {},
    onGetCredits: () -> Unit = {},
    viewModel: ParentAreaViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val info = currentWindowAdaptiveInfo()
    val user by authViewModel.authState.collectAsStateWithLifecycle()
    val isAuthWorking by authViewModel.isWorking.collectAsStateWithLifecycle()
    val authMessage by authViewModel.message.collectAsStateWithLifecycle()
    val authError by authViewModel.authError.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val onTerms = { context.openUrl(AppLinks.TERMS_URL) }
    val onPrivacy = { context.openUrl(AppLinks.PRIVACY_URL) }

    // Show sign-in feedback (success / cancelled) as a quick toast.
    androidx.compose.runtime.LaunchedEffect(authMessage) {
        authMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_SHORT).show()
            authViewModel.messageShown()
        }
    }

    // Refresh credit count + plan label on every resume — a sketch made
    // (which deducts a credit) or a purchase (which adds them) should
    // reflect immediately when the parent comes back to this screen.
    androidx.lifecycle.compose.LifecycleEventEffect(
        androidx.lifecycle.Lifecycle.Event.ON_RESUME
    ) {
        viewModel.refreshQuota()
    }

    // Refresh credits/plan whenever the account identity changes — e.g. after a
    // sign-out mints a fresh anonymous uid — so the new account's balance shows
    // right away instead of waiting for the next resume.
    androidx.compose.runtime.LaunchedEffect(user?.uid) {
        if (user?.uid != null) viewModel.refreshQuota()
    }

    val accountCard: @Composable (Modifier) -> Unit = { modifier ->
        AccountCard(
            user = user,
            onSignOut = authViewModel::signOut,
            onSignInWithGoogle = {
                (context as? android.app.Activity)?.let { authViewModel.signInWithGoogle(it) }
            },
            isWorking = isAuthWorking,
            errorText = authError,
            onRetry = authViewModel::retrySignIn,
            modifier = modifier
        )
    }

    if (info.isCompactWidth) {
        ParentAreaContent(
            state = state,
            onManageSubscription = onManageSubscription,
            onBuyMore = onGetCredits,
            onSketchLimitChanged = viewModel::onSketchLimitChanged,
            onAllowFreeTextPromptsChanged = viewModel::onAllowFreeTextPromptsChanged,
            onSessionLimitChanged = viewModel::onSessionLimitChanged,
            onClearArtwork = viewModel::onClearArtwork,
            onOpenSupport = onOpenSupport,
            onTerms = onTerms,
            onPrivacy = onPrivacy,
            accountCard = accountCard
        )
    } else {
        ParentAreaTabletContent(
            state = state,
            onManageSubscription = onManageSubscription,
            onBuyMore = onGetCredits,
            onSketchLimitChanged = viewModel::onSketchLimitChanged,
            onAllowFreeTextPromptsChanged = viewModel::onAllowFreeTextPromptsChanged,
            onSessionLimitChanged = viewModel::onSessionLimitChanged,
            onClearArtwork = viewModel::onClearArtwork,
            onOpenSupport = onOpenSupport,
            onTerms = onTerms,
            onPrivacy = onPrivacy,
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
    onSessionLimitChanged: (Int?) -> Unit,
    onClearArtwork: () -> Unit,
    onOpenSupport: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
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
                    ProgressCard(streakCurrent = state.streakCurrent, streakBest = state.streakBest)
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
                        onAllowFreeTextPromptsChanged = onAllowFreeTextPromptsChanged,
                        sessionLimitMinutes = state.sessionLimitMinutes,
                        onSessionLimitChanged = onSessionLimitChanged
                    )
                    HelpSupportCard(onOpenSupport = onOpenSupport)
                    ClearArtworkCard(onDelete = onClearArtwork)
                    LegalFooter(onTerms = onTerms, onPrivacy = onPrivacy)
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
    onSessionLimitChanged: (Int?) -> Unit,
    onClearArtwork: () -> Unit,
    onOpenSupport: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
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
                ProgressCard(
                    streakCurrent = state.streakCurrent,
                    streakBest = state.streakBest,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                ChildSafetyCard(
                    sketchLimit = state.sketchLimit,
                    onSketchLimitChanged = onSketchLimitChanged,
                    allowFreeTextPrompts = state.allowFreeTextPrompts,
                    onAllowFreeTextPromptsChanged = onAllowFreeTextPromptsChanged,
                    sessionLimitMinutes = state.sessionLimitMinutes,
                    onSessionLimitChanged = onSessionLimitChanged,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                PrivacyCard(modifier = Modifier.padding(horizontal = 16.dp))
            }

            item {
                HelpSupportCard(
                    onOpenSupport = onOpenSupport,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                ClearArtworkCard(
                    onDelete = onClearArtwork,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            item {
                LegalFooter(
                    onTerms = onTerms,
                    onPrivacy = onPrivacy,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
                label = "Get Credits",
                onClick = onBuyMore,
                leadingIcon = Icons.Filled.Add,
                height = 52.dp,
                edgeThickness = 6.dp
            )
        }
    }
}

// ─── Card: Coloring Progress (streak) ──────────────────────────────

@Composable
private fun ProgressCard(
    streakCurrent: Int,
    streakBest: Int,
    modifier: Modifier = Modifier
) {
    SettingsCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "🔥", fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = "Coloring Progress",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            ProgressStat(
                label = "Current streak",
                value = if (streakCurrent == 1) "1 day" else "$streakCurrent days",
                modifier = Modifier.weight(1f)
            )
            ProgressStat(
                label = "Best streak",
                value = if (streakBest == 1) "1 day" else "$streakBest days",
                modifier = Modifier.weight(1f)
            )
        }
        if (streakCurrent > 0) {
            Spacer(Modifier.height(16.dp))
            com.colormagic.kids.presentation.components.StreakWeekStrip(streak = streakCurrent)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Days in a row your child opened the app to color.",
            fontSize = 13.sp,
            color = BrandTokens.MutedInk
        )
    }
}

@Composable
private fun ProgressStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTokens.HeadingInk
        )
        Spacer(Modifier.height(2.dp))
        Text(text = label, fontSize = 13.sp, color = BrandTokens.MutedInk)
    }
}

// ─── Card: Child Safety ────────────────────────────────────────────

@Composable
private fun ChildSafetyCard(
    sketchLimit: SketchLimit,
    onSketchLimitChanged: (SketchLimit) -> Unit,
    allowFreeTextPrompts: Boolean,
    onAllowFreeTextPromptsChanged: (Boolean) -> Unit,
    sessionLimitMinutes: Int?,
    onSessionLimitChanged: (Int?) -> Unit,
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

        SettingsRowTitle(
            title = "Screen-Time Limit",
            subtitle = "After this much play in one session, a gentle \"time for a break\" screen appears."
        )
        Spacer(Modifier.height(10.dp))
        SessionLimitPicker(selected = sessionLimitMinutes, onSelect = onSessionLimitChanged)

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

private const val SESSION_MIN = 5    // shortest custom screen-time, minutes
private const val SESSION_MAX = 240  // longest custom screen-time (4 hours)

@Composable
private fun SessionLimitPicker(
    selected: Int?,
    onSelect: (Int?) -> Unit
) {
    val presets = listOf(15, 30, 60) // minutes
    // Custom = a non-null minute value that isn't one of the presets.
    val isCustom = selected != null && selected !in presets
    var showCustomDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        LimitChip(text = "Off", selected = selected == null, onClick = { onSelect(null) })
        presets.forEach { minutes ->
            LimitChip(
                text = "${minutes}m",
                selected = !isCustom && selected == minutes,
                onClick = { onSelect(minutes) }
            )
        }
        LimitChip(
            text = if (isCustom) "✏️ ${selected}m" else "✏️ Custom",
            selected = isCustom,
            onClick = { showCustomDialog = true }
        )
    }

    if (showCustomDialog) {
        CustomNumberDialog(
            title = "Custom screen-time",
            prompt = "How many minutes per session? ($SESSION_MIN–$SESSION_MAX)",
            initial = selected,
            min = SESSION_MIN,
            max = SESSION_MAX,
            onDismiss = { showCustomDialog = false },
            onConfirm = { minutes ->
                onSelect(minutes)
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun SketchLimitPicker(
    selected: SketchLimit,
    onSelect: (SketchLimit) -> Unit
) {
    val presets = SketchLimit.presets
    // The current value is "custom" if it's a number that isn't one of the presets.
    val isCustom = selected.perDay != null && presets.none { it.perDay == selected.perDay }
    var showCustomDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        presets.forEach { limit ->
            LimitChip(
                text = limit.label,
                selected = !isCustom && limit == selected,
                onClick = { onSelect(limit) }
            )
        }
        // Custom: shows the chosen number when active, else a "✏️ Custom" prompt.
        LimitChip(
            text = if (isCustom) "✏️ ${selected.perDay}" else "✏️ Custom",
            selected = isCustom,
            onClick = { showCustomDialog = true }
        )
    }

    if (showCustomDialog) {
        CustomNumberDialog(
            title = "Custom daily limit",
            prompt = "How many sketches per day? " +
                "(${SketchLimit.MIN_CUSTOM}–${SketchLimit.MAX_CUSTOM})",
            initial = selected.perDay,
            min = SketchLimit.MIN_CUSTOM,
            max = SketchLimit.MAX_CUSTOM,
            onDismiss = { showCustomDialog = false },
            onConfirm = { n ->
                onSelect(SketchLimit(n))
                showCustomDialog = false
            }
        )
    }
}

@Composable
private fun LimitChip(text: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = if (selected) MaterialTheme.colorScheme.primary else Color.White,
        border = if (selected) null
        else androidx.compose.foundation.BorderStroke(1.5.dp, BrandTokens.SubtleOutline)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp)
        ) {
            Text(
                text = text,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) Color.White else BrandTokens.HeadingInk
            )
        }
    }
}

/** Reusable number-entry dialog for a custom limit (sketches or minutes). */
@Composable
private fun CustomNumberDialog(
    title: String,
    prompt: String,
    initial: Int?,
    min: Int,
    max: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val maxLen = max.toString().length
    var text by remember {
        mutableStateOf(initial?.takeIf { it in min..max }?.toString() ?: "")
    }
    val value = text.toIntOrNull()
    val valid = value != null && value in min..max

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(text = prompt, fontSize = 14.sp, color = BrandTokens.MutedInk)
                Spacer(Modifier.height(12.dp))
                androidx.compose.material3.OutlinedTextField(
                    value = text,
                    onValueChange = { new -> text = new.filter { it.isDigit() }.take(maxLen) },
                    singleLine = true,
                    isError = text.isNotEmpty() && !valid,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    )
                )
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { value?.let(onConfirm) },
                enabled = valid
            ) { Text("Set") }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
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

// ─── Card: Help & Support ──────────────────────────────────────────

@Composable
private fun HelpSupportCard(
    onOpenSupport: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onOpenSupport,
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFB7E0F2),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF055680)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.SupportAgent,
                    contentDescription = null,
                    tint = Color(0xFFE3F2FB),
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Help & Support",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF093B57)
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Send a suggestion, report a bug, or ask a question.",
                    fontSize = 14.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF0B4D72)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = Color(0xFF0B4D72),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

// ─── Legal footer (Terms / Privacy) ────────────────────────────────

@Composable
private fun LegalFooter(
    onTerms: () -> Unit,
    onPrivacy: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LegalLink(text = "Terms of Service", onClick = onTerms)
        Text(text = "  •  ", fontSize = 13.sp, color = BrandTokens.MutedInk)
        LegalLink(text = "Privacy Policy", onClick = onPrivacy)
    }
}

@Composable
private fun LegalLink(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 4.dp)
        )
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
            onSessionLimitChanged = {},
            onClearArtwork = {},
            onOpenSupport = {},
            onTerms = {},
            onPrivacy = {},
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
