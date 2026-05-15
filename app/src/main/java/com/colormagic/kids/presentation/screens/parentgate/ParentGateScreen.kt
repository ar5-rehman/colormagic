package com.colormagic.kids.presentation.screens.parentgate

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import com.colormagic.kids.presentation.adaptive.isCompactWidth
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.parent.BiometricAuthenticator
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val HOLD_DURATION_MS = 3000

@Composable
fun ParentGateScreen(
    onUnlocked: () -> Unit,
    onCancel: () -> Unit,
    viewModel: ParentGateViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val capability = remember(context) { viewModel.biometricAuthenticator.capability(context) }

    val onHoldComplete: () -> Unit = {
        // After 3-second hold, the device biometric / device credential
        // prompt is the actual unlock step. If the device has no biometric
        // hardware OR nothing is enrolled, just pass — the hold itself is
        // the gate (kids won't think to hold steady for 3s).
        if (activity != null && capability == BiometricAuthenticator.Capability.Available) {
            viewModel.biometricAuthenticator.authenticate(
                activity = activity,
                onSuccess = {
                    viewModel.grantAccess()
                    onUnlocked()
                },
                onCancelled = { /* stays on the gate */ },
                onError = { /* stays on the gate */ }
            )
        } else {
            viewModel.grantAccess()
            onUnlocked()
        }
    }

    val info = currentWindowAdaptiveInfo()
    if (info.isCompactWidth) {
        ParentGateContent(onHoldComplete = onHoldComplete, onCancel = onCancel)
    } else {
        ParentGateTabletContent(onHoldComplete = onHoldComplete, onCancel = onCancel)
    }
}

@Composable
private fun ParentGateTabletContent(
    onHoldComplete: () -> Unit,
    onCancel: () -> Unit
) {
    // Full pastel gradient background; the gate is a centered modal card
    // that visually floats above it.
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = RoundedCornerShape(36.dp),
                color = Color.White,
                modifier = Modifier
                    .fillMaxWidth(0.45f)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(36.dp),
                        ambientColor = Color(0x22000000),
                        spotColor = Color(0x22000000)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(BrandTokens.SubtleSurface),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Filled.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "Grown-ups only",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Hold for 3 seconds to continue",
                        fontSize = 15.sp,
                        color = BrandTokens.MutedInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(28.dp))
                    HoldToContinueButton(onComplete = onHoldComplete)
                    Spacer(Modifier.height(28.dp))
                    Text(
                        text = "Cancel",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = BrandTokens.MutedInk,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onCancel)
                            .padding(horizontal = 28.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ParentGateContent(
    onHoldComplete: () -> Unit,
    onCancel: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = safeTop, bottom = safeBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Soft pastel header band echoing the design's bubble-glass illustration.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.28f)
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                Color.White
                            )
                        )
                    )
            )

            Spacer(Modifier.height(8.dp))

            ShieldBadge()

            Spacer(Modifier.height(20.dp))

            Text(
                text = "Grown-ups only",
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
            )

            Spacer(Modifier.height(8.dp))

            Text(
                text = "Hold for 3 seconds to continue",
                fontSize = 16.sp,
                color = BrandTokens.MutedInk,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(1f))

            HoldToContinueButton(onComplete = onHoldComplete)

            Spacer(Modifier.weight(1f))

            Text(
                text = "Cancel",
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.HeadingInk,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .pointerInput(Unit) {
                        detectTapGestures(onTap = { onCancel() })
                    }
                    .padding(horizontal = 32.dp, vertical = 12.dp)
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ShieldBadge() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun HoldToContinueButton(onComplete: () -> Unit) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var hasFired by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(180.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        hasFired = false
                        val animJob = scope.launch {
                            progress.animateTo(
                                targetValue = 1f,
                                animationSpec = tween(HOLD_DURATION_MS, easing = LinearEasing)
                            )
                            if (!hasFired) {
                                hasFired = true
                                onComplete()
                            }
                        }
                        val released = tryAwaitRelease()
                        animJob.cancel()
                        if (released && !hasFired) {
                            // Released before the hold finished — reset.
                            scope.launch { progress.animateTo(0f, tween(250)) }
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { progress.value },
            modifier = Modifier.size(180.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
            strokeWidth = 8.dp
        )
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.TouchApp,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(48.dp)
            )
        }
    }

    // Reset on first composition (the screen has just been shown).
    LaunchedEffect(Unit) {
        delay(50)
        progress.snapTo(0f)
    }
}
