package com.colormagic.kids.presentation.screens.gallery

import android.content.Context
import androidx.core.net.toUri
import androidx.print.PrintHelper
import com.colormagic.kids.domain.model.GalleryArtwork

// Sends a piece of gallery artwork to the Android system print dialog, from
// which a parent can print to a real printer or "Save as PDF". Parents love
// printing colored pages, and PDF export doubles as a no-printer fallback.
//
// We print the saved colored PNG via its MediaStore content:// URI. Entries
// without a localUri (legacy / failed saves) can't be printed — the caller
// should hide the action in that case.
fun printArtwork(context: Context, artwork: GalleryArtwork) {
    val uriString = artwork.localUri ?: return
    runCatching {
        PrintHelper(context).apply {
            scaleMode = PrintHelper.SCALE_MODE_FIT       // whole page, no cropping
            colorMode = PrintHelper.COLOR_MODE_COLOR
            orientation = PrintHelper.ORIENTATION_PORTRAIT
        }.printBitmap("${artwork.title} — ColorMagic Kids", uriString.toUri())
    }
}
