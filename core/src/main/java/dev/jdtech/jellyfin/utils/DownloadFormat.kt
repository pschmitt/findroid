package dev.jdtech.jellyfin.utils

import android.content.Context
import android.text.format.Formatter

/** Formats a transfer rate the same way across the download notification and the Downloads page. */
fun formatDownloadSpeed(context: Context, bytesPerSecond: Long): String {
    return "${Formatter.formatShortFileSize(context, bytesPerSecond.coerceAtLeast(0))}/s"
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
