package com.colormagic.kids.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.AuthUser

// Account panel for the Parent Area.
//
// ColorMagic Kids uses anonymous Firebase auth, so there's no "sign in" —
// every device just *has* a guest account. This card surfaces that account
// and offers a sign-out (really a reset). While [user] is null the anonymous
// sign-in is still resolving, so we show a spinner.
@Composable
fun AccountCard(
    user: AuthUser?,
    onSignOut: () -> Unit,
    onSignInWithGoogle: () -> Unit = {},
    isWorking: Boolean = false,
    errorText: String? = null,
    onRetry: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        if (user == null) {
            // No identity yet. If sign-in actually FAILED, show the reason + a
            // Retry button instead of spinning forever.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                if (errorText != null) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Couldn't connect",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTokens.HeadingInk
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = errorText,
                            fontSize = 13.sp,
                            lineHeight = 18.sp,
                            color = BrandTokens.MutedInk,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(14.dp))
                        PrimaryPillButton(
                            label = "Try again",
                            icon = Icons.Filled.Refresh,
                            loading = false,
                            enabled = true,
                            onClick = onRetry
                        )
                    }
                } else {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        } else {
            val isGuest = user.isAnonymous
            Column(modifier = Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isGuest) "Guest account"
                            else (user.displayName ?: "Signed in"),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTokens.HeadingInk
                        )
                        Text(
                            text = if (isGuest) "ID: ${user.uid.take(8)}"
                            else (user.email ?: "Google account"),
                            fontSize = 13.sp,
                            color = BrandTokens.MutedInk
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (isGuest)
                        "Sign in with Google to save your credits and art — and " +
                            "keep them if you reinstall or switch phones."
                    else
                        "Your credits and art are safely saved to your Google account.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BrandTokens.MutedInk
                )
                Spacer(Modifier.height(14.dp))

                if (isGuest) {
                    // Guests get one clear action: upgrade to Google so their
                    // credits/art are saved. (No destructive "reset" button.)
                    PrimaryPillButton(
                        label = if (isWorking) "Signing in…" else "Sign in with Google",
                        icon = Icons.AutoMirrored.Filled.Login,
                        loading = isWorking,
                        enabled = !isWorking,
                        onClick = onSignInWithGoogle
                    )
                } else {
                    // Signed in with Google → a real, non-destructive sign-out
                    // (drops back to a guest account).
                    SubtlePillButton(
                        label = "Sign out",
                        icon = Icons.AutoMirrored.Filled.Logout,
                        onClick = onSignOut
                    )
                }
            }
        }
    }
}

@Composable
private fun PrimaryPillButton(
    label: String,
    icon: ImageVector,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }
    }
}

@Composable
private fun SubtlePillButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = BrandTokens.SubtleSurface,
        modifier = Modifier
            .fillMaxWidth()
            .height(44.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = BrandTokens.HeadingInk,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = label,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.HeadingInk
            )
        }
    }
}
