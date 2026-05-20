package com.colormagic.kids.domain.repository

import android.graphics.Bitmap
import com.colormagic.kids.domain.model.GalleryArtwork
import kotlinx.coroutines.flow.Flow

// Persistent in-app gallery. Backed by:
//   • MediaStore (Pictures/ColorMagicKids/) — also makes the artwork show up
//                                              in the phone's stock gallery.
//   • A local JSON metadata file — survives reinstall-survival isn't a goal
//                                  yet; this is per-install for MVP.
interface GalleryRepository {

    /** Hot stream of every saved artwork, newest first. */
    val artworks: Flow<List<GalleryArtwork>>

    /**
     * Saves [bitmap] to disk + MediaStore and records its metadata. Returns
     * the persisted artwork (including the content:// URI used for share +
     * in-app display) on success, or null on any failure.
     */
    suspend fun save(
        bitmap: Bitmap,
        prompt: String,
        category: String?
    ): GalleryArtwork?

    /** Removes the metadata entry. We deliberately do NOT delete the
     *  MediaStore file — once it's in the kid's phone gallery they own it. */
    suspend fun delete(id: String)

    /** Clears every metadata entry. Same caveat as [delete] — the underlying
     *  MediaStore PNGs are left alone; only the in-app gallery is wiped. */
    suspend fun deleteAll()
}
