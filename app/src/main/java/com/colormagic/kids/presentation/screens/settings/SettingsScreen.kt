package com.colormagic.kids.presentation.screens.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.SettingsRowCard
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val APP_VERSION = "App Version 2.4.1"

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel()

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onManageSubscription: () -> Unit,
    onDeleteAllArtwork: () -> Unit,
    @Suppress("UNUSED_PARAMETER") viewModel: SettingsViewModel = hiltViewModel()
) {
    val info = currentWindowAdaptiveInfo()
    SettingsContent(
        compact = info.isCompactWidth,
        onBack = onBack,
        onManageSubscription = onManageSubscription,
        onRestorePurchases = {},
        onPrivacyPolicy = {},
        onContactSupport = {},
        onDeleteAllArtwork = onDeleteAllArtwork
    )
}

@Composable
private fun SettingsContent(
    compact: Boolean,
    onBack: () -> Unit,
    onManageSubscription: () -> Unit,
    onRestorePurchases: () -> Unit,
    onPrivacyPolicy: () -> Unit,
    onContactSupport: () -> Unit,
    onDeleteAllArtwork: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeTop)
        ) {
            HeaderRow(onBack = onBack)

            if (compact) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ManageSubscriptionRow(onClick = onManageSubscription)
                    RestorePurchasesRow(onClick = onRestorePurchases)
                    PrivacyPolicyRow(onClick = onPrivacyPolicy)
                    ContactSupportRow(onClick = onContactSupport)
                    Spacer(Modifier.height(8.dp))
                    DangerZoneCard(onDelete = onDeleteAllArtwork)
                    Spacer(Modifier.height(24.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ManageSubscriptionRow(onClick = onManageSubscription)
                        RestorePurchasesRow(onClick = onRestorePurchases)
                        PrivacyPolicyRow(onClick = onPrivacyPolicy)
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ContactSupportRow(onClick = onContactSupport)
                        DangerZoneCard(onDelete = onDeleteAllArtwork)
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            onClick = onBack,
            shape = RoundedCornerShape(50),
            color = BrandTokens.SubtleSurface,
            modifier = Modifier.size(40.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = BrandTokens.HeadingInk,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        BrandHeading(
            text = "Parent Settings",
            fontSize = 28.sp,
            lineHeight = 34.sp,
            modifier = Modifier.weight(1f)
        )
        Surface(
            shape = RoundedCornerShape(50),
            color = BrandTokens.SubtleSurface
        ) {
            Text(
                text = APP_VERSION,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.MutedInk,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
private fun ManageSubscriptionRow(onClick: () -> Unit) {
    SettingsRowCard(
        title = "Manage Subscription",
        subtitle = "View plan, billing, and renewal options.",
        icon = Icons.Filled.CreditCard,
        onClick = onClick
    )
}

@Composable
private fun RestorePurchasesRow(onClick: () -> Unit) {
    SettingsRowCard(
        title = "Restore Purchases",
        subtitle = "Sync previously unlocked tools or packs.",
        icon = Icons.Filled.Refresh,
        onClick = onClick
    )
}

@Composable
private fun PrivacyPolicyRow(onClick: () -> Unit) {
    SettingsRowCard(
        title = "Privacy Policy",
        subtitle = "Read how we protect your child's data.",
        icon = Icons.Filled.Lock,
        onClick = onClick
    )
}

@Composable
private fun ContactSupportRow(onClick: () -> Unit) {
    SettingsRowCard(
        title = "Contact Support",
        subtitle = "Need help? Send us a message.",
        icon = Icons.Filled.HelpOutline,
        onClick = onClick,
        iconTint = Color(0xFF055680),
        iconBackground = Color(0xFFB7E0F2)
    )
}

@Composable
private fun DangerZoneCard(onDelete: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color(0xFFFDEAEA),
        border = BorderStroke(1.dp, Color(0xFFE7B5B5)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = null,
                    tint = Color(0xFFB0192C),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Danger Zone",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFB0192C)
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = "This action is permanent and cannot be undone.",
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFB0192C)
            )
            Spacer(Modifier.height(14.dp))
            Surface(
                onClick = onDelete,
                shape = RoundedCornerShape(50),
                color = Color(0xFFB0192C)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
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

@Preview(name = "Settings – tablet", showBackground = true, widthDp = 1000, heightDp = 700)
@Composable
private fun SettingsPreviewTablet() {
    ColorMagicKidsTheme {
        SettingsContent(
            compact = false,
            onBack = {},
            onManageSubscription = {},
            onRestorePurchases = {},
            onPrivacyPolicy = {},
            onContactSupport = {},
            onDeleteAllArtwork = {}
        )
    }
}

@Preview(name = "Settings – phone", showBackground = true, widthDp = 360, heightDp = 880)
@Composable
private fun SettingsPreviewPhone() {
    ColorMagicKidsTheme {
        SettingsContent(
            compact = true,
            onBack = {},
            onManageSubscription = {},
            onRestorePurchases = {},
            onPrivacyPolicy = {},
            onContactSupport = {},
            onDeleteAllArtwork = {}
        )
    }
}
