package com.colormagic.kids.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.GalleryArtwork

@Composable
fun ArtworkCard(
    artwork: GalleryArtwork,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
    onPrint: (() -> Unit)? = null,
    onAnimate: () -> Unit = {},
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(24.dp)
    Surface(
        onClick = onOpen,
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            ArtworkThumbnail(artwork = artwork)

            Spacer(Modifier.height(10.dp))

            // Title + date
            Column(modifier = Modifier.padding(horizontal = 6.dp)) {
                Text(
                    text = artwork.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTokens.HeadingInk
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = artwork.dateLabel,
                    fontSize = 13.sp,
                    color = BrandTokens.MutedInk
                )
            }

            Spacer(Modifier.height(10.dp))

            // Action buttons – two rows, equal-width
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LabeledAction(
                        icon = Icons.Filled.Edit,
                        label = "Edit",
                        backgroundColor = Color(0xFFE3F2FD),
                        tint = Color(0xFF1565C0),
                        onClick = onEdit,
                        modifier = Modifier.weight(1f)
                    )
                    LabeledAction(
                        icon = Icons.Filled.Share,
                        label = "Share",
                        backgroundColor = Color(0xFFD0EBFF),
                        tint = Color(0xFF01579B),
                        onClick = onShare,
                        modifier = Modifier.weight(1f)
                    )
                    if (onPrint != null) {
                        LabeledAction(
                            icon = Icons.Filled.Print,
                            label = "Print",
                            backgroundColor = Color(0xFFE3DDF6),
                            tint = Color(0xFF4A347E),
                            onClick = onPrint,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimateAction(
                        isActive = artwork.animationType != "None",
                        onClick = onAnimate,
                        modifier = Modifier.weight(1f)
                    )
                    LabeledAction(
                        icon = Icons.Filled.DeleteOutline,
                        label = "Delete",
                        backgroundColor = Color(0xFFFADADA),
                        tint = Color(0xFFC62828),
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtworkThumbnail(artwork: GalleryArtwork) {
    val animType = artwork.animationType
    val hasAnim = animType != "None"
    val infiniteTransition = rememberInfiniteTransition(label = "gallery_anim")
    val animProgress by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = when (animType) {
                    "Spin" -> 2000
                    "Heartbeat" -> 800
                    "Wiggle" -> 600
                    else -> 1200
                },
                easing = LinearEasing
            ),
            repeatMode = if (animType == "Spin") RepeatMode.Restart else RepeatMode.Reverse
        ),
        label = "gallery_anim_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(artwork.placeholderTint))
            .then(
                if (hasAnim) Modifier.graphicsLayer {
                    when (animType) {
                        "Bounce" -> translationY = -20f * animProgress
                        "Float" -> {
                            translationY = -14f * animProgress
                            scaleX = 1f + 0.02f * animProgress
                            scaleY = 1f + 0.02f * animProgress
                        }
                        "Wiggle" -> rotationZ = 3f * (animProgress * 2f - 1f)
                        "Spin" -> rotationY = 360f * animProgress
                        "Heartbeat" -> {
                            val s = 1f + 0.06f * animProgress
                            scaleX = s; scaleY = s
                        }
                        "Jelly" -> {
                            scaleX = 1f + 0.04f * animProgress
                            scaleY = 1f - 0.03f * animProgress
                        }
                    }
                } else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        val displayUri = artwork.localUri ?: artwork.thumbnailUrl
        if (displayUri != null) {
            coil.compose.AsyncImage(
                model = displayUri,
                contentDescription = artwork.title,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Text(text = "🎨", fontSize = 56.sp)
        }
    }
}

@Composable
private fun LabeledAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    backgroundColor: Color,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.height(2.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium, color = tint)
        }
    }
}

@Composable
private fun AnimateAction(
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (isActive) Color(0xFFE8F5E9) else Color(0xFFF0ECFF),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("✨", fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(
                if (isActive) "On" else "Animate",
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = if (isActive) Color(0xFF2E7D32) else Color(0xFF5E35B1)
            )
        }
    }
}
