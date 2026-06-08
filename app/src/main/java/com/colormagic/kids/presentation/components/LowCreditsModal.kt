package com.colormagic.kids.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.monetization.CreditConfig
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

/**
 * Shown when the user tries to generate a coloring page with no credits.
 * Always offers a way to earn credits without forcing any action.
 */
@Composable
fun LowCreditsModal(
    onWatchAd: () -> Unit,
    onGoPremium: () -> Unit,
    onDismiss: () -> Unit,
    /** When true, the parent is already subscribed — hide ad + upgrade options
     *  and offer a Credit Refill instead (premium users never see ads). */
    isPremium: Boolean = false
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Icon(
                    imageVector = Icons.Filled.AutoAwesome,
                    contentDescription = null,
                    tint = Color(0xFF7E66A6),
                    modifier = Modifier.size(36.dp)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Need more credits?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk,
                    textAlign = TextAlign.Center
                )
            }
        },
        text = {
            Text(
                text = if (isPremium)
                    "You've used today's credits — they refresh tomorrow. " +
                        "Need more right now? Grab a Credit Refill."
                else
                    "You're out of credits. Watch a short ad to earn " +
                        "${CreditConfig.REWARDED_AD_CREDITS} credits, or upgrade for more " +
                        "credits and no ads.",
                fontSize = 15.sp,
                lineHeight = 22.sp,
                color = BrandTokens.MutedInk,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Primary CTA — Watch Ad (free users only; premium never sees ads)
                if (!isPremium) {
                    Surface(
                        onClick = onWatchAd,
                        shape = RoundedCornerShape(50),
                        color = Color(0xFF2E7D32),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = "Watch Ad  +${CreditConfig.REWARDED_AD_CREDITS} Credits",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                // Secondary CTA — Go Premium (free) / Buy Credit Refill (premium)
                Surface(
                    onClick = onGoPremium,
                    shape = RoundedCornerShape(50),
                    color = Color(0xFF5E35B1),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.WorkspacePremium,
                            contentDescription = null,
                            tint = Color(0xFFFFD700),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPremium) "Buy Credit Refill" else "Go Premium",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Dismiss — no hard sell
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(50),
                    color = BrandTokens.SubtleSurface,
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Not now",
                            fontSize = 15.sp,
                            color = BrandTokens.MutedInk
                        )
                    }
                }
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 360)
@Composable
private fun LowCreditsModalPreview() {
    ColorMagicKidsTheme {
        LowCreditsModal(onWatchAd = {}, onGoPremium = {}, onDismiss = {})
    }
}
