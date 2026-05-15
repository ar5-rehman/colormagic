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
import com.colormagic.kids.domain.model.UserProfile

// Single card that handles all three auth states:
//   • Not signed in → "Continue with Google" CTA
//   • Loading       → spinner
//   • Signed in     → avatar + name + email + sign-out button
//
// Reusable: drops into Parent Area, Paywall, or any future "account" pane.
@Composable
fun AccountCard(
    user: UserProfile?,
    isLoading: Boolean,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = modifier.fillMaxWidth()
    ) {
        when {
            isLoading -> LoadingState()
            user != null -> SignedInState(user = user, onSignOut = onSignOut)
            else -> SignedOutState(onSignIn = onSignIn)
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
    }
}

@Composable
private fun SignedOutState(onSignIn: () -> Unit) {
    Column(modifier = Modifier.padding(20.dp)) {
        Text(
            text = "Account",
            fontSize = 15.sp,
            color = BrandTokens.MutedInk
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "Sign in to sync sketches across devices.",
            fontSize = 15.sp,
            lineHeight = 21.sp,
            color = BrandTokens.HeadingInk
        )
        Spacer(Modifier.height(14.dp))
        Surface(
            onClick = onSignIn,
            shape = RoundedCornerShape(50),
            color = Color(0xFF1A1B25),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Google "G" placeholder — swap for the official Google asset
                // once integrated with Credential Manager.
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "G",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A73E8)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "Continue with Google",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun SignedInState(
    user: UserProfile,
    onSignOut: () -> Unit
) {
    Column(modifier = Modifier.padding(20.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                // TODO: swap for AsyncImage(user.photoUrl) when image loader is added.
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
                    text = user.displayName.ifBlank { "Signed in" },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk
                )
                Text(
                    text = user.email,
                    fontSize = 13.sp,
                    color = BrandTokens.MutedInk
                )
            }
        }
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
