package com.wwwescape.deviceinfo.util

import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Locale

/** Formats the raw ISO security-patch string (e.g. "2023-10-05") as a human date (e.g.
 * "October 5, 2023"), falling back to the raw string if it doesn't parse as expected. */
fun formatSecurityPatchDate(raw: String): String {
    val parsed = runCatching { SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(raw) }.getOrNull()
        ?: return raw
    return DateFormat.getDateInstance(DateFormat.LONG, Locale.getDefault()).format(parsed)
}
