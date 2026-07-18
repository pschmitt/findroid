package dev.jdtech.jellyfin.utils

/**
 * Binary (IEC) units - e.g. "4.40 GiB" - used for every file/transfer size shown in the app,
 * matching how Sonarr/Radarr (and most similar dashboards) report space. Deliberately not
 * Android's own `Formatter.formatFileSize`/`formatShortFileSize` (decimal/1000-based) - mixing the
 * two made the same byte count look like two different sizes depending on which screen showed it.
 */
fun formatBinaryFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KiB", "MiB", "GiB", "TiB", "PiB")
    var value = bytes / 1024.0
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return "%.2f %s".format(value, units[unitIndex])
}

/** Formats a transfer rate the same way across the download notification and the Downloads page. */
fun formatDownloadSpeed(bytesPerSecond: Long): String {
    return "${formatBinaryFileSize(bytesPerSecond.coerceAtLeast(0))}/s"
}

/** Formats a remaining-time estimate as m:ss, or h:mm:ss once it crosses an hour. */
fun formatEta(etaSeconds: Long): String {
    if (etaSeconds < 0) return "--:--"
    val hours = etaSeconds / 3600
    val minutes = (etaSeconds % 3600) / 60
    val seconds = etaSeconds % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}
