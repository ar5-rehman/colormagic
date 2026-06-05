package com.colormagic.kids.presentation.screens.support

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MailOutline
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.colormagic.kids.domain.repository.FeedbackType
import com.colormagic.kids.presentation.components.BrandHeading
import com.colormagic.kids.presentation.components.BrandPrimaryButton
import com.colormagic.kids.presentation.components.BrandTokens
import com.colormagic.kids.presentation.components.ParentBrandHeader
import com.colormagic.kids.presentation.util.AppLinks
import com.colormagic.kids.presentation.util.openPlayStoreListing
import com.colormagic.kids.presentation.util.openSupportEmail
import com.colormagic.kids.presentation.util.openUrl
import com.colormagic.kids.ui.theme.ColorMagicKidsTheme

private const val APP_VERSION_LABEL = "Color Magic v2.4.1"
private val CONTENT_MAX_WIDTH = 600.dp

@Composable
fun SupportScreen(
    onBack: () -> Unit,
    viewModel: SupportViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SupportContent(
        state = state,
        onBack = onBack,
        onTypeSelected = viewModel::onTypeSelected,
        onMessageChanged = viewModel::onMessageChanged,
        onEmailChanged = viewModel::onEmailChanged,
        onSubmit = viewModel::onSubmit,
        onSendAnother = viewModel::onSendAnother,
        onEmailDirectly = {
            context.openSupportEmail(subject = "Color Magic — Support")
        },
        onRateApp = { context.openPlayStoreListing() },
        onTerms = { context.openUrl(AppLinks.TERMS_URL) },
        onPrivacy = { context.openUrl(AppLinks.PRIVACY_URL) }
    )
}

@Composable
private fun SupportContent(
    state: SupportUiState,
    onBack: () -> Unit,
    onTypeSelected: (FeedbackType) -> Unit,
    onMessageChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit,
    onSendAnother: () -> Unit,
    onEmailDirectly: () -> Unit,
    onRateApp: () -> Unit,
    onTerms: () -> Unit,
    onPrivacy: () -> Unit
) {
    val safeTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(top = safeTop)) {
            ParentBrandHeader(onBack = onBack, showProfile = false)

            // Centered, width-capped column → looks great on phone AND tablet.
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .widthIn(max = CONTENT_MAX_WIDTH)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(4.dp))
                    HeroBadge()
                    Spacer(Modifier.height(14.dp))
                    BrandHeading(
                        text = "How can we help?",
                        fontSize = 30.sp,
                        lineHeight = 36.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Send the team a suggestion, report a bug, or ask a " +
                            "question. A real person reads every message.",
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        color = BrandTokens.MutedInk,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))

                    if (state.isSubmitted) {
                        SuccessCard(onSendAnother = onSendAnother, onDone = onBack)
                    } else {
                        FeedbackFormCard(
                            state = state,
                            onTypeSelected = onTypeSelected,
                            onMessageChanged = onMessageChanged,
                            onEmailChanged = onEmailChanged,
                            onSubmit = onSubmit
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    QuickActionRow(
                        icon = Icons.Filled.MailOutline,
                        title = "Email us directly",
                        subtitle = AppLinks.SUPPORT_EMAIL,
                        onClick = onEmailDirectly
                    )
                    Spacer(Modifier.height(10.dp))
                    QuickActionRow(
                        icon = Icons.Filled.StarOutline,
                        title = "Rate Color Magic",
                        subtitle = "Enjoying the app? Leave a review.",
                        onClick = onRateApp
                    )

                    Spacer(Modifier.height(22.dp))
                    LegalFooter(onTerms = onTerms, onPrivacy = onPrivacy)
                }
            }
        }
    }
}

@Composable
private fun HeroBadge() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Filled.SupportAgent,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(34.dp)
        )
    }
}

// ─── Form ──────────────────────────────────────────────────────────

@Composable
private fun FeedbackFormCard(
    state: SupportUiState,
    onTypeSelected: (FeedbackType) -> Unit,
    onMessageChanged: (String) -> Unit,
    onEmailChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    CardSurface {
        FieldLabel("What's this about?")
        Spacer(Modifier.height(10.dp))
        TypeSelector(selected = state.type, onSelect = onTypeSelected)

        Spacer(Modifier.height(18.dp))
        FieldLabel(
            when (state.type) {
                FeedbackType.Suggestion -> "Your suggestion"
                FeedbackType.Bug -> "What went wrong?"
                FeedbackType.Question -> "Your question"
            }
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.message,
            onValueChange = onMessageChanged,
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            placeholder = {
                Text(
                    when (state.type) {
                        FeedbackType.Suggestion -> "Tell us your idea to make Color Magic better…"
                        FeedbackType.Bug -> "Describe what happened, and what you expected…"
                        FeedbackType.Question -> "Ask us anything about Color Magic…"
                    },
                    color = BrandTokens.MutedInk
                )
            },
            shape = RoundedCornerShape(16.dp),
            colors = brandFieldColors()
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "${state.messageCharCount}/4000",
            fontSize = 11.sp,
            color = BrandTokens.MutedInk,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End
        )

        Spacer(Modifier.height(12.dp))
        FieldLabel("Email (optional)")
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = state.email,
            onValueChange = onEmailChanged,
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("So we can reply to you", color = BrandTokens.MutedInk) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            shape = RoundedCornerShape(16.dp),
            colors = brandFieldColors()
        )

        if (state.errorMessage != null) {
            Spacer(Modifier.height(12.dp))
            Text(
                text = state.errorMessage,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = Color(0xFFC62828)
            )
        }

        Spacer(Modifier.height(18.dp))
        BrandPrimaryButton(
            label = if (state.isSubmitting) "Sending…" else "Send message",
            onClick = onSubmit,
            enabled = state.canSubmit,
            height = 56.dp,
            edgeThickness = 6.dp
        )
    }
}

@Composable
private fun TypeSelector(
    selected: FeedbackType,
    onSelect: (FeedbackType) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        TypeChip(
            label = "Suggestion",
            icon = Icons.Filled.Lightbulb,
            selected = selected == FeedbackType.Suggestion,
            onClick = { onSelect(FeedbackType.Suggestion) },
            modifier = Modifier.weight(1f)
        )
        TypeChip(
            label = "Bug",
            icon = Icons.Filled.BugReport,
            selected = selected == FeedbackType.Bug,
            onClick = { onSelect(FeedbackType.Bug) },
            modifier = Modifier.weight(1f)
        )
        TypeChip(
            label = "Question",
            icon = Icons.AutoMirrored.Filled.HelpOutline,
            selected = selected == FeedbackType.Question,
            onClick = { onSelect(FeedbackType.Question) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun TypeChip(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (selected) MaterialTheme.colorScheme.primary else BrandTokens.SubtleOutline
    val bg = if (selected) MaterialTheme.colorScheme.primaryContainer else Color.White
    val ink = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else BrandTokens.HeadingInk
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = bg,
        border = BorderStroke(if (selected) 1.5.dp else 1.dp, border),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) MaterialTheme.colorScheme.primary else BrandTokens.MutedInk,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = ink,
                maxLines = 1
            )
        }
    }
}

// ─── Success ───────────────────────────────────────────────────────

@Composable
private fun SuccessCard(onSendAnother: () -> Unit, onDone: () -> Unit) {
    CardSurface {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFDDF3E0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(32.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(
                text = "Thanks for reaching out!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTokens.HeadingInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "We've received your message and will take a look. " +
                    "If you left an email, we'll get back to you there.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = BrandTokens.MutedInk,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(18.dp))
            BrandPrimaryButton(
                label = "Done",
                onClick = onDone,
                height = 54.dp,
                edgeThickness = 6.dp
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                onClick = onSendAnother,
                shape = RoundedCornerShape(50),
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth().height(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Send another message",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

// ─── Quick actions + legal ─────────────────────────────────────────

@Composable
private fun QuickActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(BrandTokens.SubtleSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk
                )
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = BrandTokens.MutedInk,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun LegalFooter(onTerms: () -> Unit, onPrivacy: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            LinkText(text = "Terms of Service", onClick = onTerms)
            Text(
                text = "  •  ",
                fontSize = 13.sp,
                color = BrandTokens.MutedInk
            )
            LinkText(text = "Privacy Policy", onClick = onPrivacy)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = APP_VERSION_LABEL,
            fontSize = 12.sp,
            color = BrandTokens.MutedInk
        )
    }
}

@Composable
private fun LinkText(text: String, onClick: () -> Unit) {
    Surface(onClick = onClick, color = Color.Transparent) {
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            modifier = Modifier.padding(vertical = 4.dp, horizontal = 2.dp)
        )
    }
}

// ─── Shared bits ───────────────────────────────────────────────────

@Composable
private fun CardSurface(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = Color.White,
        border = BorderStroke(1.dp, BrandTokens.SubtleOutline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(20.dp)) { content() }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = BrandTokens.HeadingInk
    )
}

@Composable
private fun brandFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.White,
    unfocusedContainerColor = Color.White,
    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
    unfocusedIndicatorColor = BrandTokens.SubtleOutline
)

@Preview(name = "Support – phone", showBackground = true, widthDp = 360, heightDp = 1200)
@Composable
private fun SupportPreviewPhone() {
    ColorMagicKidsTheme {
        SupportContent(
            state = SupportUiState(),
            onBack = {}, onTypeSelected = {}, onMessageChanged = {}, onEmailChanged = {},
            onSubmit = {}, onSendAnother = {}, onEmailDirectly = {}, onRateApp = {},
            onTerms = {}, onPrivacy = {}
        )
    }
}

@Preview(name = "Support – tablet", showBackground = true, widthDp = 1000, heightDp = 800)
@Composable
private fun SupportPreviewTablet() {
    ColorMagicKidsTheme {
        SupportContent(
            state = SupportUiState(type = FeedbackType.Bug, message = "It crashed."),
            onBack = {}, onTypeSelected = {}, onMessageChanged = {}, onEmailChanged = {},
            onSubmit = {}, onSendAnother = {}, onEmailDirectly = {}, onRateApp = {},
            onTerms = {}, onPrivacy = {}
        )
    }
}
