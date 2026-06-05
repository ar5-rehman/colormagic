package com.colormagic.kids.presentation.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

/**
 * Developer-editable external links. Replace these placeholders with your real
 * URLs / address before publishing. The Privacy Policy URL here MUST match the
 * one you set in the Google Play Console listing.
 */
object AppLinks {
    const val SUPPORT_EMAIL = "support@colormagic.app"
    const val TERMS_URL = "https://colormagic.app/terms"
    const val PRIVACY_URL = "https://colormagic.app/privacy"
}

/** Opens a web URL in the browser. Shows a toast if nothing can handle it. */
fun Context.openUrl(url: String) {
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Couldn't open the link.", Toast.LENGTH_SHORT).show()
    }
}

/** Opens the device email composer pre-filled to our support address. */
fun Context.openSupportEmail(subject: String, body: String = "") {
    val uri = Uri.parse(
        "mailto:${AppLinks.SUPPORT_EMAIL}" +
            "?subject=${Uri.encode(subject)}&body=${Uri.encode(body)}"
    )
    try {
        startActivity(
            Intent(Intent.ACTION_SENDTO, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "No email app found.", Toast.LENGTH_SHORT).show()
    }
}

/** Opens this app's Play Store listing (used for the "Rate us" action). */
fun Context.openPlayStoreListing() {
    val market = "market://details?id=$packageName"
    val web = "https://play.google.com/store/apps/details?id=$packageName"
    try {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(market))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    } catch (e: ActivityNotFoundException) {
        openUrl(web)
    }
}
