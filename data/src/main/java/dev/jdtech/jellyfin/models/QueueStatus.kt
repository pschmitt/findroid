package dev.jdtech.jellyfin.models

/**
 * One Sonarr/Radarr queue entry as surfaced by
 * [dev.jdtech.jellyfin.repository.QueueStatusRepository]. [item] is the Jellyfin library item the
 * entry was matched to (see `matchSonarr`/`matchRadarr` in `QueueStatusMatching.kt`) - null when
 * the download couldn't be resolved to anything in the library, e.g. a torrent added manually on
 * the Sonarr/Radarr side for a series/movie Jellyfin hasn't imported yet. Unmatched entries still
 * carry a human-readable [title] built from the PVR side's own metadata, so a queue view can list
 * every download rather than silently dropping the unmatched ones.
 */
data class PvrQueueEntry(
    val item: FindroidItem?,
    val title: String,
    val status: QueueStatus,
)

/**
 * The download-progress payload of a single Sonarr/Radarr queue entry (see [PvrQueueEntry] for
 * the item association).
 */
data class QueueStatus(
    val source: PvrSource,
    val status: QueueItemStatus,
    val percent: Int = -1,
    val sizeBytes: Long = 0L,
    val remainingBytes: Long = 0L,
    val speedBytesPerSecond: Long = 0L,
    val etaSeconds: Long = -1L,
    val errorMessage: String? = null,
)

enum class PvrSource { SONARR, RADARR }

enum class QueueItemStatus { QUEUED, DOWNLOADING, IMPORTING, WARNING, FAILED }
