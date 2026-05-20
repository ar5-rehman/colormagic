package com.colormagic.kids.presentation.screens.gallery

import android.content.Context
import android.content.Intent
import com.colormagic.kids.domain.model.GalleryArtwork

// System-share entry point for a piece of gallery artwork.
//
// MVP behaviour: shares a kid-safe text blurb via the system share sheet —
// the colored PNG isn't yet persisted to disk (ColoringViewModel.onSave still
// has a TODO), so there's no file to attach. The moment that save lands and
// writes a PNG to the app's cache dir, this helper should:
//   1. switch the intent type to "image/png",
//   2. attach the file URI via FileProvider.getUriForFile(...),
//   3. addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION).
//
// Keeping the entry point here means the call site (Gallery card) doesn't
// have to change when that upgrade happens.
fun shareArtwork(context: Context, artwork: GalleryArtwork) {
    val text = buildString {
        append("Check out \"")
        append(artwork.title)
        append("\" — made with ColorMagic Kids!")
    }
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
        putExtra(Intent.EXTRA_SUBJECT, artwork.title)
    }
    val chooser = Intent.createChooser(send, "Share your artwork").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(chooser)
}
