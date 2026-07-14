package com.wwwescape.deviceinfo.data.deviceos

import android.content.Context
import android.util.JsonReader
import androidx.core.content.pm.PackageInfoCompat

/**
 * Resolves a raw [android.os.Build.MODEL] (e.g. "AC2001") to its consumer marketing name (e.g.
 * "OnePlus Nord"), using Google Play's public supported-devices list bundled as a JSON asset
 * (see scripts/generate_device_names.py). Models absent from the list, or ambiguous across
 * multiple retail brandings, have no entry — callers should fall back to the raw model string.
 *
 * The asset is ~1.2 MB but only one entry is ever needed, so it's stream-scanned (stopping at the
 * match) rather than materialized as a map, and the answer is persisted per app version — the
 * dashboard and the widget's background refreshes then never touch the asset again.
 */
object DeviceNameResolver {
    private const val ASSET_PATH = "device_names.json"
    private const val PREFS_NAME = "device_name_cache"

    /** Stored in place of a name when the model has no entry, so a miss is cached too. */
    private const val NO_MATCH = ""

    @Volatile
    private var cached: Pair<String, String?>? = null

    fun marketingNameFor(context: Context, model: String): String? {
        cached?.let { (cachedModel, name) -> if (cachedModel == model) return name }
        val appContext = context.applicationContext
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val key = "${appVersionCode(appContext)}|$model"
        val name = prefs.getString(key, null)
            ?: (lookupInAsset(appContext, model) ?: NO_MATCH).also { resolved ->
                // Only one model/version pair is ever relevant, so drop stale entries.
                prefs.edit().clear().putString(key, resolved).apply()
            }
        return name.takeUnless { it == NO_MATCH }.also { cached = model to it }
    }

    private fun lookupInAsset(context: Context, model: String): String? = runCatching {
        context.assets.open(ASSET_PATH).bufferedReader(Charsets.UTF_8).use { stream ->
            JsonReader(stream).use { reader ->
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == model) return@runCatching reader.nextString()
                    reader.skipValue()
                }
                null
            }
        }
    }.getOrNull()

    private fun appVersionCode(context: Context): Long = runCatching {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    }.getOrDefault(0L)
}
