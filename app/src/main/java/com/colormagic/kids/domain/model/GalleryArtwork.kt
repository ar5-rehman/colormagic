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
    val category: String? = null,
    /** content:// URI of the saved colored PNG (MediaStore). Null for the
     *  legacy / placeholder entries that pre-date the save flow. Used for
     *  both in-app rendering (AsyncImage) and system sharing. */
    val localUri: String? = null,
    /** Epoch millis the artwork was saved — used for sort + the date label. */
    val createdAtMillis: Long = 0L
)
