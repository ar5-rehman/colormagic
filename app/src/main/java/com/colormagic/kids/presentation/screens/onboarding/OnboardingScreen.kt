package com.colormagic.kids.presentation.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.R
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun OnboardingScreen(onStartCreating: () -> Unit) {
    OnboardingContent(onStartCreating = onStartCreating)
}

@Composable
private fun OnboardingContent(onStartCreating: () -> Unit) {
    val canvas = Color(0xFFF6F5F8) // soft off-white background from the mock
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(Color.White, canvas),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    ) {
        // Single soft lavender ambient wash bleeding from the top-left corner.
        Box(
            modifier = Modifier
                .size(260.dp)
                .offset(x = (-110).dp, y = (-110).dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 28.dp)
                .padding(top = 56.dp, bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeroCard()

            Spacer(Modifier.height(40.dp))

            Text(
                text = "Turn your\nideas into\ncoloring\npages",
                fontSize = 48.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF101012),
                fontFamily = MaterialTheme.typography.displayMedium.fontFamily
            )

            Spacer(Modifier.weight(1f))

            StartCreatingButton(onClick = onStartCreating)

            Spacer(Modifier.height(18.dp))

            SafetyNote()
        }
    }
}

@Composable
private fun HeroCard() {
    Surface(
        modifier = Modifier
            .fillMaxWidth(0.78f)
            .aspectRatio(1f)
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(36.dp),
                ambientColor = Color(0x33000000),
                spotColor = Color(0x33000000)
            ),
        color = Color.White,
        shape = RoundedCornerShape(36.dp)
    ) {
        Box(
            modifier = Modifier.padding(10.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(R.drawable.child_coloring),
                contentDescription = "A child happily drawing a coloring page",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(28.dp))
            )
        }
    }
}

@Composable
private fun StartCreatingButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .shadow(
                elevation = 12.dp,
                shape = RoundedCornerShape(50),
                ambientColor = MaterialTheme.colorScheme.primary,
                spotColor = MaterialTheme.colorScheme.primary
            ),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White
        )
    ) {
        Text(
            text = "Start Creating",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SafetyNote() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Filled.Shield,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = "Safe, parent-controlled AI coloring",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(name = "Onboarding – phone", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingPreviewPhone() {
    ColorMagicKidsTheme { OnboardingContent(onStartCreating = {}) }
}

@Preview(name = "Onboarding – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun OnboardingPreviewTablet() {
    ColorMagicKidsTheme { OnboardingContent(onStartCreating = {}) }
}
