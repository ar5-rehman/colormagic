package com.colormagic.kids.presentation.screens.gallery

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import com.colormagic.kids.domain.model.GalleryArtwork

// System-share entry point for a piece of gallery artwork.
//
// When the artwork has a [GalleryArtwork.localUri] (a MediaStore content://
// URI, set during the save flow), we share the actual PNG image so the
// receiving app — WhatsApp, Photos, Gmail — can attach the picture. If the
// URI is missing (older entries or save failures), we fall back to sharing
// just a kid-safe text blurb so the share sheet still does something useful.
//
// MediaStore URIs are publicly read-permitted via the system, so no
// FileProvider configuration is needed.
fun shareArtwork(context: Context, artwork: GalleryArtwork) {
    val text = "Check out \"${artwork.title}\" — made with ColorMagic Kids!"
    val send = Intent(Intent.ACTION_SEND).apply {
        putExtra(Intent.EXTRA_SUBJECT, artwork.title)
        putExtra(Intent.EXTRA_TEXT, text)
        val uriString = artwork.localUri
        if (uriString != null) {
            val uri = uriString.toUri()
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            // Grant the receiving app temporary read access to the image.
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } else {
            type = "text/plain"
        }
    }
    val chooser = Intent.createChooser(send, "Share your artwork").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
