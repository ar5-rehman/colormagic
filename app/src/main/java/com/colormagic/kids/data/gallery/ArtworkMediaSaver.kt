package com.colormagic.kids.data.gallery

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream

// Writes a finished artwork PNG into the device's Pictures collection so it
// shows up alongside camera photos in the kid's phone gallery — and so the
// returned content:// URI can be shared with any other app via Intent.
//
// API-level branches:
//   • Q (29) + → scoped storage. MediaStore.Images is the only legal path,
//                 RELATIVE_PATH determines folder, IS_PENDING gates visibility
//                 until the write commits.
//   • < Q       → legacy filesystem path under Pictures/ColorMagicKids/.
object ArtworkMediaSaver {

    private const val ALBUM = "ColorMagicKids"

    suspend fun saveToPictures(
        context: Context,
        bitmap: Bitmap,
        displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$displayName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Images.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_PICTURES}/$ALBUM"
                )
                // Hide from gallery scanners until the write actually finishes —
                // prevents partial / corrupt entries from being indexed.
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(collection, values) ?: return@withContext null
        runCatching {
            resolver.openOutputStream(uri)?.use { out: OutputStream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
        }.onFailure {
            resolver.delete(uri, null, null)
            return@withContext null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.Images.Media.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }
        uri
    }
}
