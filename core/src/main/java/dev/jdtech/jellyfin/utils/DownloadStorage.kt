package dev.jdtech.jellyfin.utils

import android.content.Context
import android.os.Environment

/**
 * Resolves the [Context.getExternalFilesDirs] index matching the "internal"/"external"
 * download-location preference. "internal" means the non-removable app-private external
 * storage (the built-in flash storage), "external" means removable storage (SD card/USB) -
 * neither is [Context.getFilesDir]. Returns -1 for "ask" or when no matching volume is mounted.
 */
fun resolveDownloadStorageIndex(context: Context, locationPreference: String): Int {
    val storageLocations = context.getExternalFilesDirs(null)
    return when (locationPreference) {
        "internal" ->
            storageLocations.indexOfFirst { it != null && !Environment.isExternalStorageRemovable(it) }
        "external" ->
            storageLocations.indexOfFirst { it != null && Environment.isExternalStorageRemovable(it) }
        else -> -1
    }
}
