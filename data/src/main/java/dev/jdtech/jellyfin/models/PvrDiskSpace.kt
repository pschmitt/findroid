package dev.jdtech.jellyfin.models

/**
 * Aggregated Sonarr/Radarr root-folder disk space, for the Downloads screen's storage summary.
 * Each side is `null` when that service isn't configured or the fetch failed - there's nothing
 * meaningful to show either way, so the UI just omits that row rather than showing an error for
 * what's a nice-to-have summary, not a critical status.
 */
data class PvrDiskSpaceResult(
    val sonarr: PvrServiceDiskSpace? = null,
    val radarr: PvrServiceDiskSpace? = null,
)

/**
 * [freeBytes]/[totalBytes] are summed across a service's root folders. Root folders can share the
 * same underlying volume, which would double-count that shared space - an accepted simplification
 * for a compact one-line-per-service summary (Sonarr/Radarr's own UI instead lists root folders
 * individually for this exact reason).
 */
data class PvrServiceDiskSpace(val freeBytes: Long, val totalBytes: Long)
