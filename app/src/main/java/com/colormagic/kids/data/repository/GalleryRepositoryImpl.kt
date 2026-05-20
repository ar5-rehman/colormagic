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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GalleryRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val store: GalleryStore,
    private val telemetry: AppTelemetry
) : GalleryRepository {

    override val artworks: Flow<List<GalleryArtwork>> = store.artworks

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

        val artwork = GalleryArtwork(
            id = id,
            title = titleFromPrompt(prompt),
            dateLabel = "Just now",
            placeholderTint = 0xFFEDE7F6,
            category = category,
            localUri = uri.toString(),
            createdAtMillis = System.currentTimeMillis()
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

    /** Shorten a kid's prompt into a card title. Keeps the first four words. */
    private fun titleFromPrompt(prompt: String): String {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) return "My Artwork"
        val words = trimmed.split(Regex("\\s+"))
        return words.take(4).joinToString(" ").replaceFirstChar { it.titlecase() }
    }
}
