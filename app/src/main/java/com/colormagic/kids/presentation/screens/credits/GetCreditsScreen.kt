package com.colormagic.kids.presentation.screens.credits

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.monetization.AdLoadState
import com.colormagic.kids.monetization.CreditConfig
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun GetCreditsScreen(
    onBack: () -> Unit,
    onGoToPremium: () -> Unit,
    viewModel: GetCreditsViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onScreenOpened()
    }

    // Show one-shot feedback via Snackbar
    LaunchedEffect(state.toastMessage) {
        state.toastMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.onToastShown()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(snackbarData = data)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
      // Cap width and centre on large screens (tablets) so cards don't stretch
      // edge-to-edge; on phones widthIn(max=640) is a no-op.
      Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        LazyColumn(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 640.dp)
                .align(Alignment.TopCenter),
            contentPadding = PaddingValues(
                top = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding(),
                bottom = 24.dp
            )
        ) {
            item { TopBar(onBack = onBack) }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(8.dp))
                    BrandHeading(
                        text = "Get Credits",
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Credits power your coloring pages.",
                        fontSize = 15.sp,
                        color = BrandTokens.MutedInk,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // ── Balance card ──────────────────────────────────────────
            item {
                Spacer(Modifier.height(20.dp))
                BalanceCard(
                    total = state.quota.totalAvailableCredits,
                    isPremium = state.quota.isPremium,
                    isLoading = state.isLoading,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            // ── Rewarded ad section (hidden for premium users) ────────
            if (!state.quota.isPremium) {
                item {
                    Spacer(Modifier.height(16.dp))
                    RewardedAdCard(
                        buttonLabel = state.watchAdButtonLabel,
                        canWatch = state.canWatchAd,
                        adLoadState = state.adLoadState,
                        isGranting = state.isGranting,
                        adsRemaining = state.quota.rewardedAdsRemaining,
                        maxAds = CreditConfig.MAX_REWARDED_ADS_PER_DAY,
                        onWatchAd = {
                            viewModel.onWatchAdTapped(context as Activity)
                        },
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // ── Premium upsell card ───────────────────────────────────
            item {
                Spacer(Modifier.height(16.dp))
                PremiumUpsellCard(
                    isPremium = state.quota.isPremium,
                    onGoToPremium = onGoToPremium,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(Modifier.height(12.dp))
            }

            // ── How credits work ─────────────────────────────────────
            item {
                HowCreditsWorkSection(modifier = Modifier.padding(horizontal = 24.dp))
            }
        }
      }
    }
}

@Composable
private fun TopBar(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = BrandTokens.HeadingInk
            )
        }
    }
}

@Composable
private fun BalanceCard(
    total: Int,
    isPremium: Boolean,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFF7E66A6),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(8.dp))
                if (isLoading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "$total",
                        fontSize = 48.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            Text(
                text = "credits available",
                fontSize = 14.sp,
                color = Color(0xFFE7DBFB)
            )
            Spacer(Modifier.height(14.dp))

            if (isPremium) {
                StatusPill(
                    text = "Premium — ${CreditConfig.PREMIUM_DAILY_CREDITS} credits/day",
                    container = Color(0xFFFFD700),
                    ink = Color(0xFF3F2C00)
                )
            } else {
                StatusPill(
                    text = "Watch ads to earn credits",
                    container = Color(0xFFB7A8D9),
                    ink = Color.White
                )
            }
        }
    }
}

@Composable
private fun StatusPill(text: String, container: Color, ink: Color) {
    Surface(shape = RoundedCornerShape(50), color = container) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = ink,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun RewardedAdCard(
    buttonLabel: String,
    canWatch: Boolean,
    adLoadState: AdLoadState,
    isGranting: Boolean,
    adsRemaining: Int,
    maxAds: Int,
    onWatchAd: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F5E9)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.PlayCircle,
                        contentDescription = null,
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Watch a short ad",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTokens.HeadingInk
                    )
                    Text(
                        text = "Earn +${CreditConfig.REWARDED_AD_CREDITS} credits — always optional",
                        fontSize = 13.sp,
                        color = BrandTokens.MutedInk
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            WatchAdButton(
                label = buttonLabel,
                enabled = canWatch,
                isLoading = adLoadState == AdLoadState.Loading || isGranting,
                onClick = onWatchAd
            )

            Spacer(Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Ads remaining today",
                    fontSize = 13.sp,
                    color = BrandTokens.MutedInk
                )
                Text(
                    text = "$adsRemaining of $maxAds",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (adsRemaining > 0) Color(0xFF2E7D32) else BrandTokens.MutedInk
                )
            }
        }
    }
}

@Composable
private fun WatchAdButton(
    label: String,
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = if (enabled) Color(0xFF2E7D32) else BrandTokens.SubtleSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = if (enabled) Color.White else BrandTokens.MutedInk,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Filled.PlayCircle,
                    contentDescription = null,
                    tint = if (enabled) Color.White else BrandTokens.MutedInk,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (enabled) Color.White else BrandTokens.MutedInk
            )
        }
    }
}

@Composable
private fun PremiumUpsellCard(
    isPremium: Boolean,
    onGoToPremium: () -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(
        listOf(Color(0xFF5E35B1), Color(0xFF7B1FA2))
    )
    Surface(
        onClick = if (isPremium) ({}) else onGoToPremium,
        enabled = !isPremium,
        shape = RoundedCornerShape(20.dp),
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.background(gradient)) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.WorkspacePremium,
                        contentDescription = null,
                        tint = Color(0xFFFFD700),
                        modifier = Modifier.size(26.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = if (isPremium) "You're Premium!" else "Go Premium",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (isPremium)
                        "No ads, ${CreditConfig.PREMIUM_DAILY_CREDITS} daily credits, and all premium tools."
                    else
                        "No ads, more credits, and premium coloring tools.",
                    fontSize = 14.sp,
                    color = Color(0xFFE1BEE7),
                    lineHeight = 20.sp
                )
                if (!isPremium) {
                    Spacer(Modifier.height(12.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White
                    ) {
                        Text(
                            text = "$${CreditConfig.PREMIUM_MONTHLY_PRICE_USD}/month — Upgrade Now",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF5E35B1),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HowCreditsWorkSection(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "How credits work",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTokens.HeadingInk
        )
        Spacer(Modifier.height(10.dp))
        CreditRuleRow("Generate a coloring page", "1 credit")
        CreditRuleRow("Apply a premium style", "2 credits")
        CreditRuleRow("Save to your gallery", "Free")
        CreditRuleRow("Watch a rewarded ad", "+${CreditConfig.REWARDED_AD_CREDITS} credits")
        CreditRuleRow("Rewarded ads per day", "up to ${CreditConfig.MAX_REWARDED_ADS_PER_DAY}")
        CreditRuleRow("Premium subscription", "${CreditConfig.PREMIUM_DAILY_CREDITS} credits/day")
    }
}

@Composable
private fun CreditRuleRow(action: String, cost: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = action, fontSize = 14.sp, color = BrandTokens.MutedInk)
        Text(
            text = cost,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = BrandTokens.HeadingInk
        )
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Composable
private fun GetCreditsPreview() {
    ColorMagicKidsTheme {
        GetCreditsScreen(onBack = {}, onGoToPremium = {})
    }
}
