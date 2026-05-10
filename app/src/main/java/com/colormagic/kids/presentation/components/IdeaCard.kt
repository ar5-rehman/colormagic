package com.colormagic.kids.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.colormagic.kids.domain.model.ColoringIdea

// Card that previews a backend-supplied coloring idea. The illustration
// area is a tinted Box for now — swap in AsyncImage(idea.previewImageUrl)
// when network image loading is wired up.
@Composable
fun IdeaCard(
    idea: ColoringIdea,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardShape = RoundedCornerShape(22.dp)
    Surface(
        onClick = onClick,
        shape = cardShape,
        color = MaterialTheme.colorScheme.surface,
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = cardShape,
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000)
            )
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
                    .background(Color(idea.previewTint))
            )
            Text(
                text = idea.title,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTokens.HeadingInk,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp)
            )
        }
    }
}
