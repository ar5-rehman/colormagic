package com.colormagic.kids.domain.model

// A finished coloring saved by the kid. Backend / local DB will populate this.
data class SavedPicture(
    val id: String,
    val sourceSketchId: String,
    val imageUrl: String? = null,
    val placeholderTint: Long = 0xFFE8F5E9
)
