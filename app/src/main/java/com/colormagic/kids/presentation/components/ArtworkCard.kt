package com.colormagic.kids.presentation.components

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
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.GalleryArtwork

// Card for one entry in the kid's gallery list. Reusable for any list of
// saved artworks (Gallery, "Recent" rails, share-sheet picker, etc.).
//
// Image area is a tinted placeholder for now — swap in AsyncImage(thumbnailUrl)
// once an image loader is added.
@Composable
fun ArtworkCard(
    artwork: GalleryArtwork,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onShare: () -> Unit = {},
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
            Spacer(Modifier.height(12.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
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
                Spacer(Modifier.width(10.dp))
                CircleIconAction(
                    icon = Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = "Open ${artwork.title}",
                    backgroundColor = Color(0xFFB3E5FC),
                    tint = Color(0xFF01579B),
                    onClick = onOpen
                )
                Spacer(Modifier.width(8.dp))
                CircleIconAction(
                    icon = Icons.Filled.Share,
                    contentDescription = "Share ${artwork.title}",
                    backgroundColor = Color(0xFFD0EBFF),
                    tint = Color(0xFF01579B),
                    onClick = onShare
                )
                Spacer(Modifier.width(8.dp))
                CircleIconAction(
                    icon = Icons.Filled.DeleteOutline,
                    contentDescription = "Delete ${artwork.title}",
                    backgroundColor = Color(0xFFFADADA),
                    tint = Color(0xFFC62828),
                    onClick = onDelete
                )
            }
        }
    }
}

@Composable
private fun ArtworkThumbnail(artwork: GalleryArtwork) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(artwork.placeholderTint)),
        contentAlignment = Alignment.Center
    ) {
        // Backend will provide thumbnailUrl → swap for AsyncImage here.
        Text(text = "🎨", fontSize = 56.sp)
    }
}

@Composable
private fun CircleIconAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    backgroundColor: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        modifier = Modifier.size(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
