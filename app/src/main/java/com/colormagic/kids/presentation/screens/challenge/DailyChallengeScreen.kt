package com.colormagic.kids.presentation.screens.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.data.local.preferences.ChallengePreferences
import com.colormagic.kids.domain.model.DailyChallengeProvider
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.BrandTopBar
import com.colormagic.kids.presentation.components.TactileSurface

@Composable
fun DailyChallengeScreen(
    onBack: () -> Unit,
    onStartChallenge: (String) -> Unit,
    challengePrefs: ChallengePreferences
) {
    val challenge = remember { DailyChallengeProvider.today() }
    var completedToday by remember { mutableStateOf(false) }
    var bestScore by remember { mutableStateOf(0) }
    var totalChallenges by remember { mutableStateOf(0) }
    var totalStars by remember { mutableStateOf(0) }
    var lastScore by remember { mutableStateOf(0) }
    var lastStars by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        completedToday = challengePrefs.hasCompletedToday()
        challengePrefs.bestScore.collect { bestScore = it }
    }
    LaunchedEffect(Unit) { challengePrefs.totalChallenges.collect { totalChallenges = it } }
    LaunchedEffect(Unit) { challengePrefs.totalStars.collect { totalStars = it } }
    LaunchedEffect(Unit) { challengePrefs.lastScore.collect { lastScore = it } }
    LaunchedEffect(Unit) { challengePrefs.lastStars.collect { lastStars = it } }

    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxSize().padding(top = safeTop, bottom = safeBottom)) {
            BrandTopBar(
                onBack = onBack,
                centered = true,
                title = {
                    Text(
                        text = "Daily Challenge",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        fontFamily = MaterialTheme.typography.headlineLarge.fontFamily
                    )
                }
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(20.dp))

                // Challenge card
                Surface(
                    shape = RoundedCornerShape(28.dp),
                    shadowElevation = 12.dp,
                    color = Color.Transparent,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .background(Brush.linearGradient(listOf(Color(0xFF7C4DFF), Color(0xFFAB47BC))))
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(challenge.emoji, fontSize = 52.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            challenge.theme,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            challenge.prompt,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(20.dp))

                        if (completedToday) {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color(0x33FFFFFF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("Today's Score", fontSize = 13.sp, color = Color.White.copy(alpha = 0.8f))
                                    Text("$lastScore/100", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                                    Row {
                                        repeat(5) { i ->
                                            Icon(
                                                Icons.Filled.Star, null,
                                                tint = if (i < lastStars) Color(0xFFFFD600) else Color(0x55FFFFFF),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        } else {
                            TactileSurface(
                                onClick = { onStartChallenge(challenge.prompt) },
                                fill = Color.White,
                                edge = Color(0xFFDDDDDD),
                                shape = RoundedCornerShape(50),
                                height = 52.dp,
                                edgeThickness = 5.dp,
                                contentColor = Color(0xFF7C4DFF),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFFFF6D3F), modifier = Modifier.size(22.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Challenge!", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF7C4DFF))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Stats
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(label = "Best Score", value = "$bestScore", emoji = "🏆", modifier = Modifier.weight(1f))
                    StatCard(label = "Challenges", value = "$totalChallenges", emoji = "🎨", modifier = Modifier.weight(1f))
                    StatCard(label = "Total Stars", value = "$totalStars", emoji = "⭐", modifier = Modifier.weight(1f))
                }

                Spacer(Modifier.height(20.dp))

                // How scoring works
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFFF5F0FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text("How Scoring Works", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A347E))
                        Spacer(Modifier.height(10.dp))
                        ScoreExplainer("🎯", "Coverage", "Color as much of the picture as you can!")
                        Spacer(Modifier.height(6.dp))
                        ScoreExplainer("✏️", "Stay in Lines", "Try to keep your colors inside the lines")
                        Spacer(Modifier.height(6.dp))
                        ScoreExplainer("🌈", "Color Variety", "Use lots of different colors!")
                    }
                }

                Spacer(Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, emoji: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = BrandTokens.SubtleSurface,
        modifier = modifier
    ) {
        Column(Modifier.padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(emoji, fontSize = 22.sp)
            Spacer(Modifier.height(4.dp))
            Text(value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BrandTokens.HeadingInk)
            Text(label, fontSize = 11.sp, color = BrandTokens.MutedInk, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ScoreExplainer(emoji: String, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(emoji, fontSize = 18.sp)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4A347E))
            Text(desc, fontSize = 12.sp, color = Color(0xFF7E6BA8))
        }
    }
}
