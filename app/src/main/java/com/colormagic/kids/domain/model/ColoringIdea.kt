package com.colormagic.kids.domain.model

// Shape of a single "Need ideas?" suggestion. Backend will provide
// the title and previewImageUrl; previewTint is a fallback color used
// while the image loads or when the backend doesn't supply an image.
data class ColoringIdea(
    val id: String,
    val title: String,
    /** Big emoji shown as the card's "illustration" while there are no images. */
    val emoji: String = "✨",
    val previewImageUrl: String? = null,
    val previewTint: Long = 0xFFEDE7F6
)
