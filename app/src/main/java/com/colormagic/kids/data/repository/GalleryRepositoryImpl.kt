package com.colormagic.kids.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.colormagic.kids.data.gallery.ArtworkMediaSaver
import com.colormagic.kids.data.gallery.GalleryStore
import com.colormagic.kids.data.telemetry.AppTelemetry
import com.colormagic.kids.domain.model.GalleryArtwork
import com.colormagic.kids.domain.repository.GalleryRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: GalleryStore,
    private val telemetry: AppTelemetry
) : GalleryRepository {

    // The stored dateLabel used to be a frozen "Just now" string. We now derive
    // a real "MMM d, yyyy · h:mm a" label from the saved timestamp at display
    // time, so each card shows when it was actually made.
    override val artworks: Flow<List<GalleryArtwork>> =
        store.artworks.map { list ->
            list.map { a ->
                if (a.createdAtMillis > 0L) a.copy(dateLabel = formatTimestamp(a.createdAtMillis))
                else a
            }
        }

    override suspend fun save(
        bitmap: Bitmap,
        prompt: String,
        category: String?
    ): GalleryArtwork? {
        val id = UUID.randomUUID().toString()
        val displayName = "colormagic_${System.currentTimeMillis()}"
        val uri = runCatching {
            ArtworkMediaSaver.saveToPictures(context, bitmap, displayName)
        }.onFailure(telemetry::recordNonFatal).getOrNull() ?: return null

        val now = System.currentTimeMillis()
        val artwork = GalleryArtwork(
            id = id,
            title = titleFromPrompt(prompt),
            dateLabel = formatTimestamp(now),
            placeholderTint = 0xFFEDE7F6,
            category = category,
            localUri = uri.toString(),
            createdAtMillis = now
        )
        store.add(artwork)
        return artwork
    }

    override suspend fun delete(id: String) {
        store.remove(id)
        // The MediaStore PNG is deliberately left in place — once it's in
        // the kid's phone gallery they own it; we only remove the in-app
        // metadata entry.
    }

    override suspend fun deleteAll() {
        store.clear()
    }

    /** Formats a save timestamp as an absolute, readable date + time. */
    private fun formatTimestamp(millis: Long): String =
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault()).format(Date(millis))

    /** Shorten a kid's prompt into a card title. Keeps the first four words. */
    private fun titleFromPrompt(prompt: String): String {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return "My Artwork"
        val words = trimmed.split(Regex("\\s+"))
        return words.take(4).joinToString(" ").replaceFirstChar { it.titlecase() }
    }
}
