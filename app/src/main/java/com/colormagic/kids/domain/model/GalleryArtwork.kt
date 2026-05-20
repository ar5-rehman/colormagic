package com.colormagic.kids.domain.model

// A single entry in the kid's Gallery — backed by either a sketch they
// coloured (sourceSketchId) or a one-off illustration. Date is pre-formatted
// at the repository / VM layer as a kid-friendly relative label.
data class GalleryArtwork(
    val id: String,
    val title: String,
    val dateLabel: String,
    val thumbnailUrl: String? = null,
    val placeholderTint: Long = 0xFFEDE7F6,
    /** One of [CategoryIdeas] keys (or null for older / uncategorised art).
     *  Drives the Gallery's category filter chip row. */
    val category: String? = null
)
