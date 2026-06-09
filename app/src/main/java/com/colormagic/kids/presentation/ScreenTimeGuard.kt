package com.colormagic.kids.presentation

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.fragment.app.FragmentActivity
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.colormagic.kids.data.parents.ParentControlsStore
import com.colormagic.kids.presentation.parent.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject
import kotlin.random.Random

/** Exposes the parent-set per-session screen-time cap + the biometric gate. */
@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    parentControls: ParentControlsStore,
    val biometricAuthenticator: BiometricAuthenticator
) : ViewModel() {
    val sessionLimitMinutes: StateFlow<Int?> = parentControls.state
        .map { it.sessionLimitMinutes }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}

/**
 * Wraps the main app and shows a gentle, full-screen "time for a break" screen
 * once the current session exceeds the parent's screen-time limit.
 *
 * Adding more time is a **parent-only** action: it requires the device
 * biometric / PIN (the same gate as the Parent Area). On devices with no lock
 * set, it falls back to a math challenge a young child can't solve. Either way
 * a kid can't dismiss the screen on their own.
 */
@Composable
fun ScreenTimeGuard(
    modifier: Modifier = Modifier,
    viewModel: ScreenTimeViewModel = hiltViewModel(),
    content: @Composable () -> Unit
) {
    val limitMinutes by viewModel.sessionLimitMinutes.collectAsStateWithLifecycle()
    var sessionStart by remember { mutableStateOf(SystemClock.elapsedRealtime()) }
    var onBreak by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val biometricAvailable = remember(context) {
        activity != null &&
            viewModel.biometricAuthenticator.capability(context) ==
            BiometricAuthenticator.Capability.Available
    }

    // Reset the session clock to a fresh full allowance once a grown-up unlocks.
    val extend: () -> Unit = { sessionStart = SystemClock.elapsedRealtime() }

    val onRequestBiometric: () -> Unit = {
        if (activity != null) {
            viewModel.biometricAuthenticator.authenticate(
                activity = activity,
                title = "Grown-ups only",
                subtitle = "Unlock to add more play time",
                onSuccess = extend
            )
        }
    }

    LaunchedEffect(limitMinutes, sessionStart) {
        onBreak = false
        val limit = limitMinutes ?: return@LaunchedEffect // off → never breaks
        while (true) {
            val elapsedMinutes = (SystemClock.elapsedRealtime() - sessionStart) / 60_000.0
            if (elapsedMinutes >= limit) {
                onBreak = true
                break
            }
            delay(5_000)
        }
    }

    Box(modifier.fillMaxSize()) {
        content()
        if (onBreak) {
            BreakOverlay(
                biometricAvailable = biometricAvailable,
                onRequestBiometric = onRequestBiometric,
                onMathPassed = extend
            )
        }
    }
}

@Composable
private fun BreakOverlay(
    biometricAvailable: Boolean,
    onRequestBiometric: () -> Unit,
    onMathPassed: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xF21A1033)) // near-opaque dreamy night scrim
            // Consume all taps so the kid can't interact with the app beneath.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(text = "🌙", fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Time for a break!",
                color = Color.White,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "Great coloring today! 🎨\nOnly a grown-up can add more play time.",
                color = Color(0xFFD9D2F0),
                fontSize = 16.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(28.dp))

            if (biometricAvailable) {
                UnlockPill(
                    label = "🔒  Grown-up unlock",
                    onClick = onRequestBiometric
                )
            } else {
                // No device lock set → math challenge fallback so the gate is
                // still parent-restricted.
                MathGate(onPassed = onMathPassed)
            }
        }
    }
}

@Composable
private fun UnlockPill(label: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = Color.White.copy(alpha = 0.16f)
    ) {
        Text(
            text = label,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            modifier = Modifier.padding(horizontal = 28.dp, vertical = 14.dp)
        )
    }
}

@Composable
private fun MathGate(onPassed: () -> Unit) {
    // Two-digit × one-digit multiplication — beyond a typical 4–10 year-old.
    val a = remember { Random.nextInt(11, 20) }
    val b = remember { Random.nextInt(3, 10) }
    var answer by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(false) }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Grown-ups: what is $a × $b?",
            color = Color.White,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = answer,
            onValueChange = { new ->
                answer = new.filter { it.isDigit() }.take(4)
                wrong = false
            },
            singleLine = true,
            isError = wrong,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(160.dp)
        )
        if (wrong) {
            Spacer(Modifier.height(6.dp))
            Text(text = "Try again", color = Color(0xFFFFAB91), fontSize = 13.sp)
        }
        Spacer(Modifier.height(12.dp))
        UnlockPill(label = "Unlock") {
            if (answer.toIntOrNull() == a * b) onPassed() else wrong = true
        }
    }
}
