package com.colormagic.kids.domain.model

// Shape of a single "Need ideas?" suggestion. Backend will provide
// the title and previewImageUrl; previewTint is a fallback color used
// while the image loads or when the backend doesn't supply an image.
data class ColoringIdea(
    val id: String,
    val title: String,
    val previewImageUrl: String? = null,
    val previewTint: Long = 0xFFEDE7F6
)
