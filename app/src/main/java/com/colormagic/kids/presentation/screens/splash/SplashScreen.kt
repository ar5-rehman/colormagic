package com.colormagic.kids.presentation.screens.splash

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.colormagic.kids.R
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

@Composable
fun SplashScreen(
    // Called with the route to go to next (Main for returning users, Onboarding
    // on first launch). The VM decides + also triggers anonymous sign-in.
    onReady: (route: String) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val nextRoute by viewModel.nextRoute.collectAsStateWithLifecycle()
    LaunchedEffect(nextRoute) {
        nextRoute?.let(onReady)
    }
    SplashContent()
}

@Composable
private fun SplashContent() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(140.dp)
                        .shadow(
                            elevation = 16.dp,
                            shape = RoundedCornerShape(32.dp),
                            ambientColor = MaterialTheme.colorScheme.primary,
                            spotColor = MaterialTheme.colorScheme.primary
                        ),
                    color = Color.White,
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Image(
                            painter = painterResource(R.drawable.color_magic_kids_logo),
                            contentDescription = "ColorMagic Kids logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(108.dp)
                                .clip(RoundedCornerShape(24.dp))
                        )
                    }
                }

                Spacer(Modifier.height(28.dp))

                Text(
                    text = "ColorMagic Kids",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(Modifier.height(10.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Unleash your imagination",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }

                Spacer(Modifier.height(40.dp))

                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50)),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Preparing your canvas...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(name = "Splash – phone", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun SplashPreviewPhone() {
    ColorMagicKidsTheme { SplashContent() }
}

@Preview(name = "Splash – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun SplashPreviewTablet() {
    ColorMagicKidsTheme { SplashContent() }
}
