package com.colormagic.kids.presentation.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Button
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val DangerRed = Color(0xFFB0192C)

/** Confirmation shown before wiping every saved artwork — the delete is permanent. */
@Composable
fun DeleteAllArtworkDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Filled.DeleteOutline,
                contentDescription = null,
                tint = DangerRed
            )
        },
        title = {
            Text(
                text = "Delete all artwork?",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTokens.HeadingInk
            )
        },
        text = {
            Text(
                text = "This permanently removes every saved artwork from this device. " +
                    "This can't be undone.",
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = BrandTokens.MutedInk
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = DangerRed,
                    contentColor = Color.White
                )
            ) {
                Text("Delete", fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", fontWeight = FontWeight.SemiBold, color = BrandTokens.MutedInk)
            }
        }
    )
}
