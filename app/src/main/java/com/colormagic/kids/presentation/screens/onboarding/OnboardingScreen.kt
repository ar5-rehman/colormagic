package com.colormagic.kids.presentation.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.R
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme
import kotlinx.coroutines.launch

// ═════════════════════════════════════════════════════════════════════
//  Onboarding — 5 swipeable pages that teach kids + parents how the
//  app works, including a clear explanation of the credit system.
// ═════════════════════════════════════════════════════════════════════

private const val PAGE_COUNT = 5

@Composable
fun OnboardingScreen(onStartCreating: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == PAGE_COUNT - 1

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { index ->
            when (index) {
                0 -> WelcomePage()
                1 -> HowItWorksPage()
                2 -> ToolsPage()
                3 -> CreditsPage()
                4 -> SafetyPage()
            }
        }

        // ── Bottom controls ──────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color.White.copy(alpha = 0.95f), Color.White)
                    )
                )
                .padding(horizontal = 28.dp)
                .padding(top = 24.dp, bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            PageDots(count = PAGE_COUNT, current = pagerState.currentPage)
            Spacer(Modifier.height(24.dp))

            if (isLastPage) {
                BigButton(
                    text = "Let's Start Creating! 🎨",
                    color = MaterialTheme.colorScheme.primary,
                    onClick = onStartCreating
                )
            } else {
                BigButton(
                    text = "Next",
                    color = MaterialTheme.colorScheme.primary,
                    trailingIcon = Icons.AutoMirrored.Filled.ArrowForward,
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )
            }
            Spacer(Modifier.height(10.dp))
            AnimatedVisibility(visible = !isLastPage, enter = fadeIn(), exit = fadeOut()) {
                Surface(
                    onClick = onStartCreating,
                    color = Color.Transparent,
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFAAAAAA),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Page 1 — Welcome
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun WelcomePage() {
    PageShell(gradient = listOf(Color(0xFFF3EEFF), Color.White)) {
        Spacer(Modifier.height(12.dp))

        // Logo
        Image(
            painter = painterResource(R.drawable.color_magic_kids_logo),
            contentDescription = "Color Magic Kids logo",
            modifier = Modifier
                .size(80.dp)
                .clip(RoundedCornerShape(18.dp))
        )
        Spacer(Modifier.height(16.dp))

        // Hero image
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.72f)
                .aspectRatio(1f)
                .shadow(16.dp, RoundedCornerShape(32.dp)),
            color = Color.White,
            shape = RoundedCornerShape(32.dp)
        ) {
            Box(Modifier.padding(8.dp), contentAlignment = Alignment.Center) {
                Image(
                    painter = painterResource(R.drawable.child_coloring),
                    contentDescription = "A child coloring",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(26.dp))
                )
            }
        }
        Spacer(Modifier.height(32.dp))

        Text(
            text = "Welcome to",
            fontSize = 18.sp,
            color = Color(0xFF9E9E9E),
            fontWeight = FontWeight.Medium
        )
        Text(
            text = "Color Magic Kids",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.primary,
            fontFamily = MaterialTheme.typography.displayMedium.fontFamily
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = "Turn any idea into a magical coloring page!\nJust say what you imagine — and start coloring.",
            fontSize = 16.sp,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF6F6E76),
            modifier = Modifier.fillMaxWidth(0.88f)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Page 2 — How It Works (3-step visual)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun HowItWorksPage() {
    PageShell(gradient = listOf(Color(0xFFFFF8F0), Color.White)) {
        EmojiCluster("💡", "✏️", "🖼️")
        Spacer(Modifier.height(20.dp))
        PageTitle("How It Works")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Three easy steps to create art!",
            fontSize = 16.sp,
            color = Color(0xFF9E9E9E)
        )
        Spacer(Modifier.height(28.dp))

        StepCard(
            number = 1,
            emoji = "💬",
            title = "Describe your idea",
            body = "Type anything — \"a pirate riding a unicorn\" or pick a fun category like Animals or Space.",
            accent = Color(0xFF7E57C2),
            bg = Color(0xFFF3EEFF)
        )
        Spacer(Modifier.height(14.dp))
        StepCard(
            number = 2,
            emoji = "✨",
            title = "AI creates the page",
            body = "Our magic AI turns your words into a beautiful black-and-white coloring page in seconds!",
            accent = Color(0xFFE65100),
            bg = Color(0xFFFFF3E0)
        )
        Spacer(Modifier.height(14.dp))
        StepCard(
            number = 3,
            emoji = "🎨",
            title = "Color & save!",
            body = "Use crayons, markers, glitter and more to bring it to life. Save it or print it!",
            accent = Color(0xFF2E7D32),
            bg = Color(0xFFE8F5E9)
        )
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Page 3 — Magic Tools
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun ToolsPage() {
    PageShell(gradient = listOf(Color(0xFFF0F6FF), Color.White)) {
        EmojiCluster("🖍️", "🎨", "✨")
        Spacer(Modifier.height(20.dp))
        PageTitle("Magic Art Tools")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Pick your favorite and start coloring!",
            fontSize = 16.sp,
            color = Color(0xFF9E9E9E)
        )
        Spacer(Modifier.height(24.dp))

        // Tool grid — 2 columns of tool chips
        val tools = listOf(
            "🖍️" to "Crayon",
            "🖊️" to "Marker",
            "✏️" to "Pencil",
            "💧" to "Watercolor",
            "🌟" to "Highlighter",
            "🦄" to "Magic Rainbow",
            "✨" to "Glitter",
            "🪣" to "Fill Bucket"
        )
        tools.chunked(2).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { (emoji, name) ->
                    ToolChip(emoji = emoji, name = name, modifier = Modifier.weight(1f))
                }
                // If odd row, fill the remaining space
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
        }

        Spacer(Modifier.height(12.dp))
        FeaturePill("Undo & Redo", "↩️")
        Spacer(Modifier.height(8.dp))
        FeaturePill("Save to Gallery & Print", "🖨️")
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Page 4 — Credits Explained (THE KEY PAGE)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun CreditsPage() {
    PageShell(gradient = listOf(Color(0xFFFFF9F0), Color.White)) {
        EmojiCluster("⭐", "🎟️", "💎")
        Spacer(Modifier.height(20.dp))
        PageTitle("How Credits Work")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Each coloring page costs 1 credit. Here's how you get them:",
            fontSize = 16.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFF6F6E76),
            modifier = Modifier.fillMaxWidth(0.9f)
        )
        Spacer(Modifier.height(24.dp))

        // Credit source 1 — Daily free
        CreditSourceCard(
            emoji = "🌅",
            title = "Free Daily Credit",
            body = "You get 1 free credit every day — just for opening the app! Resets at midnight.",
            accent = Color(0xFFFF8F00),
            bg = Color(0xFFFFF8E1),
            border = Color(0xFFFFCC80)
        )
        Spacer(Modifier.height(12.dp))

        // Credit source 2 — Watch ads
        CreditSourceCard(
            emoji = "🎬",
            title = "Watch a Short Video",
            body = "Watch a quick video to earn 3 bonus credits. You can watch up to 5 per day = 15 credits!",
            accent = Color(0xFF1565C0),
            bg = Color(0xFFE3F2FD),
            border = Color(0xFF90CAF9)
        )
        Spacer(Modifier.height(12.dp))

        // Credit source 3 — Buy packs
        CreditSourceCard(
            emoji = "💎",
            title = "Buy Credit Packs",
            body = "Need more? Parents can buy a pack of 20 credits anytime. These never expire!",
            accent = Color(0xFF7E57C2),
            bg = Color(0xFFF3EEFF),
            border = Color(0xFFD1C4E9)
        )
        Spacer(Modifier.height(12.dp))

        // Credit source 4 — Go Pro
        CreditSourceCard(
            emoji = "👑",
            title = "Go Pro — Unlimited Fun",
            body = "Pro members get 30 credits every day + 50 bonus monthly credits. No ads needed!",
            accent = Color(0xFFC62828),
            bg = Color(0xFFFFEBEE),
            border = Color(0xFFEF9A9A)
        )

        Spacer(Modifier.height(20.dp))

        // Quick math callout
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFF5F5F5),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📊", fontSize = 22.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Free users can earn up to 16 credits daily",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF424242),
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "1 daily + 15 from videos = 16 coloring pages!",
                    fontSize = 13.sp,
                    color = Color(0xFF757575),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Page 5 — Safe & Fun (parents focus)
// ═════════════════════════════════════════════════════════════════════

@Composable
private fun SafetyPage() {
    PageShell(gradient = listOf(Color(0xFFF0FAF0), Color.White)) {
        EmojiCluster("🛡️", "👨‍👩‍👧", "❤️")
        Spacer(Modifier.height(20.dp))
        PageTitle("Safe & Fun for Kids")
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Parents are always in control.",
            fontSize = 16.sp,
            color = Color(0xFF9E9E9E)
        )
        Spacer(Modifier.height(24.dp))

        SafetyItem(
            emoji = "⏰",
            title = "Screen-Time Limits",
            body = "Set a timer so your child gets a gentle break reminder. Only a parent can extend it."
        )
        Spacer(Modifier.height(12.dp))
        SafetyItem(
            emoji = "🔢",
            title = "Daily Sketch Limits",
            body = "Choose how many coloring pages your child can create each day (1, 3, 5, 7, 10 or custom)."
        )
        Spacer(Modifier.height(12.dp))
        SafetyItem(
            emoji = "🔒",
            title = "Privacy First",
            body = "All artwork stays on your device. We never share your child's creations or personal data."
        )
        Spacer(Modifier.height(12.dp))
        SafetyItem(
            emoji = "🔥",
            title = "Streaks & Motivation",
            body = "Kids build a daily coloring streak! Sign in with Google to keep progress across devices."
        )

        Spacer(Modifier.height(24.dp))

        // Google sign-in nudge
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFFFF3E0),
            border = BorderStroke(1.dp, Color(0xFFFFCC80)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(text = "💡", fontSize = 20.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Tip for parents",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE65100)
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "Sign in with Google after setup to save credits, " +
                            "settings and streaks — so nothing is lost if " +
                            "your child switches devices.",
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = Color(0xFFBF360C)
                    )
                }
            }
        }
    }
}

// ═════════════════════════════════════════════════════════════════════
//  Shared building blocks
// ═════════════════════════════════════════════════════════════════════

/** Scrollable page wrapper with gradient background and ambient wash. */
@Composable
private fun PageShell(
    gradient: List<Color>,
    content: @Composable () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(gradient))
    ) {
        // Ambient circle (top-left)
        Box(
            Modifier
                .size(260.dp)
                .offset((-110).dp, (-110).dp)
                .clip(CircleShape)
                .background(gradient.first().copy(alpha = 0.6f))
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = safeTop + 28.dp, bottom = 210.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            content()
        }
    }
}

/** Three overlapping emoji in a playful cluster. */
@Composable
private fun EmojiCluster(left: String, center: String, right: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = left,
            fontSize = 32.sp,
            modifier = Modifier.offset(x = (-36).dp, y = 6.dp)
        )
        Text(
            text = center,
            fontSize = 44.sp
        )
        Text(
            text = right,
            fontSize = 32.sp,
            modifier = Modifier.offset(x = 36.dp, y = 6.dp)
        )
    }
}

@Composable
private fun PageTitle(text: String) {
    Text(
        text = text,
        fontSize = 30.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF101012),
        textAlign = TextAlign.Center,
        fontFamily = MaterialTheme.typography.displayMedium.fontFamily
    )
}

/** Numbered step card for the "How It Works" page. */
@Composable
private fun StepCard(
    number: Int,
    emoji: String,
    title: String,
    body: String,
    accent: Color,
    bg: Color
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Number badge
            Surface(
                shape = CircleShape,
                color = accent,
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        text = "$number",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = emoji, fontSize = 20.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = body,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

/** Tool chip for the art tools page. */
@Composable
private fun ToolChip(emoji: String, name: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, Color(0xFFE8E4EE)),
        shadowElevation = 2.dp,
        modifier = modifier.height(56.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = emoji, fontSize = 22.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF424242)
            )
        }
    }
}

/** Small feature pill (undo, print, etc). */
@Composable
private fun FeaturePill(text: String, emoji: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = Color(0xFFF5F0FF),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 18.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF5E35B1)
            )
        }
    }
}

/** Credit source card for the credits explanation page. */
@Composable
private fun CreditSourceCard(
    emoji: String,
    title: String,
    body: String,
    accent: Color,
    bg: Color,
    border: Color
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Emoji badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(text = emoji, fontSize = 24.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = accent
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

/** Safety feature item for the parents page. */
@Composable
private fun SafetyItem(emoji: String, title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(text = emoji, fontSize = 28.sp)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF2E7D32)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = body,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Color(0xFF616161)
                )
            }
        }
    }
}

// ─── Page indicator dots ─────────────────────────────────────────────

@Composable
private fun PageDots(count: Int, current: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(count) { i ->
            val active = i == current
            val scale by animateFloatAsState(
                targetValue = if (active) 1f else 0.85f,
                animationSpec = spring(stiffness = Spring.StiffnessMedium),
                label = "dot"
            )
            Box(
                Modifier
                    .scale(scale)
                    .then(
                        if (active) Modifier
                            .width(26.dp)
                            .height(10.dp)
                        else Modifier.size(10.dp)
                    )
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else Color(0xFFD8D4DE)
                    )
            )
        }
    }
}

// ─── Big CTA button ──────────────────────────────────────────────────

@Composable
private fun BigButton(
    text: String,
    color: Color,
    trailingIcon: ImageVector? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(50),
        color = color,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
    ) {
        Row(
            Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(text, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            if (trailingIcon != null) {
                Spacer(Modifier.width(8.dp))
                Icon(trailingIcon, null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

// ─── Previews ────────────────────────────────────────────────────────

@Preview(name = "Onboarding – phone", showBackground = true, widthDp = 360, heightDp = 800)
@Composable
private fun OnboardingPreviewPhone() {
    ColorMagicKidsTheme { OnboardingScreen(onStartCreating = {}) }
}

@Preview(name = "Onboarding – tablet", showBackground = true, widthDp = 800, heightDp = 1200)
@Composable
private fun OnboardingPreviewTablet() {
    ColorMagicKidsTheme { OnboardingScreen(onStartCreating = {}) }
}
