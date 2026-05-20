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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
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
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        if (user == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(30.dp)
                )
            }
        } else {
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
                            text = "Guest account",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTokens.HeadingInk
                        )
                        Text(
                            // Short uid fragment — enough to identify the
                            // device account without showing the full id.
                            text = "ID: ${user.uid.take(8)}",
                            fontSize = 13.sp,
                            color = BrandTokens.MutedInk
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Credits and saved art are tied to this account. " +
                        "Signing out starts a fresh one.",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = BrandTokens.MutedInk
                )
                Spacer(Modifier.height(14.dp))
                Surface(
                    onClick = onSignOut,
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
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = BrandTokens.HeadingInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Sign out",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTokens.HeadingInk
                        )
                    }
                }
            }
        }
    }
}
