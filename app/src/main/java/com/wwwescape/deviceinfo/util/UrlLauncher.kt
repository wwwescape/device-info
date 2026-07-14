package com.wwwescape.deviceinfo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

/** Opens a web link in the user's browser. Only https URLs are allowed, and a device with no
 * browser (or with it disabled) is a silent no-op rather than a crash. */
fun openUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (!uri.scheme.equals("https", ignoreCase = true)) return
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        // Nothing can handle the link.
    }
}
