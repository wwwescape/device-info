package com.wwwescape.deviceinfo.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.wwwescape.deviceinfo.R

/** Opens a web link in the user's browser. Only https URLs are allowed. Many TVs ship without a
 * browser, so when nothing can open the link the address is shown instead, to visit elsewhere. */
fun openUrl(context: Context, url: String) {
    val uri = Uri.parse(url)
    if (!uri.scheme.equals("https", ignoreCase = true)) return
    val intent = Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
    try {
        context.startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(context, context.getString(R.string.link_no_browser, url), Toast.LENGTH_LONG).show()
    }
}
