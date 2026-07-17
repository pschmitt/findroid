package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.api.pvr.RadarrMovie
import dev.jdtech.jellyfin.api.pvr.RadarrQueueItem
import dev.jdtech.jellyfin.api.pvr.PvrImage
import dev.jdtech.jellyfin.api.pvr.SonarrQueueItem
import dev.jdtech.jellyfin.api.pvr.SonarrSeries
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.PvrQueueEntry
import dev.jdtech.jellyfin.models.PvrSource
import dev.jdtech.jellyfin.models.QueueItemStatus
import dev.jdtech.jellyfin.models.QueueStatus
import java.util.UUID

/**
 * Pure functions matching Sonarr/Radarr queue entries to Jellyfin items - no suspend, no I/O, so
 * they're directly unit-testable without Room/Hilt/Android in the loop. Every queue entry produces
 * a [PvrQueueEntry]; a lookup that fails along the way (unknown provider id, orphaned queue
 * reference, episode not yet synced into Jellyfin's library, a torrent added manually on the PVR
 * side, ...) yields an unmatched entry (`item = null`) titled from the PVR side's own metadata
 * instead of being dropped - this must never throw, since a single bad PVR-side reference
 * shouldn't take down the whole match.
 */

/** Sonarr's `series.tvdbId`/`movie.tmdbId` default to 0 when the field is absent from the DTO. */
private const val UNSET_PROVIDER_ID = 0

fun matchSonarr(
    series: List<SonarrSeries>,
    queue: List<SonarrQueueItem>,
    jellyfinShows: List<FindroidShow>,
    episodesByShowId: Map<UUID, List<FindroidEpisode>>,
): List<PvrQueueEntry> {
    val showByTvdbId: Map<String, FindroidShow> =
        jellyfinShows.mapNotNull { show -> show.tvdbId?.let { it to show } }.toMap()
    val seriesById: Map<Int, SonarrSeries> = series.associateBy { it.id }

    return queue.map { item ->
        val sonarrSeries = seriesById[item.seriesId]
        val episodeNumber = item.episode?.episodeNumber?.takeIf { it != UNSET_PROVIDER_ID }
        val show =
            sonarrSeries
                ?.tvdbId
                ?.takeIf { it != UNSET_PROVIDER_ID }
                ?.let { showByTvdbId[it.toString()] }
        val episode =
            if (show != null && episodeNumber != null) {
                episodesByShowId[show.id]?.firstOrNull {
                    it.parentIndexNumber == item.seasonNumber && it.indexNumber == episodeNumber
                }
            } else {
                null
            }
        PvrQueueEntry(
            item = episode,
            title = sonarrQueueTitle(sonarrSeries, item, episodeNumber),
            status = item.toQueueStatus(),
            tmdbId = sonarrSeries?.tmdbId?.takeIf { it != UNSET_PROVIDER_ID },
            sonarrEpisodeId = item.episodeId.takeIf { it != UNSET_PROVIDER_ID },
            seasonNumber = item.seasonNumber.takeIf { it != UNSET_PROVIDER_ID },
            episodeNumber = episodeNumber,
            posterUrl = sonarrSeries?.images?.posterUrl(),
            queueItemId = item.id,
        )
    }
}

fun matchRadarr(
    movies: List<RadarrMovie>,
    queue: List<RadarrQueueItem>,
    jellyfinMovies: List<FindroidMovie>,
): List<PvrQueueEntry> {
    val movieByTmdbId: Map<String, FindroidMovie> =
        jellyfinMovies.mapNotNull { movie -> movie.tmdbId?.let { it to movie } }.toMap()
    val radarrMovieById: Map<Int, RadarrMovie> = movies.associateBy { it.id }

    return queue.map { item ->
        val radarrMovie = radarrMovieById[item.movieId]
        val movie =
            radarrMovie
                ?.tmdbId
                ?.takeIf { it != UNSET_PROVIDER_ID }
                ?.let { movieByTmdbId[it.toString()] }
        PvrQueueEntry(
            item = movie,
            title = radarrMovie?.title?.takeIf { it.isNotBlank() } ?: item.title ?: UNKNOWN_TITLE,
            status = item.toQueueStatus(),
            tmdbId = radarrMovie?.tmdbId?.takeIf { it != UNSET_PROVIDER_ID },
            posterUrl = radarrMovie?.images?.posterUrl(),
            queueItemId = item.id,
        )
    }
}

/**
 * Collapses queue entries into the per-item status map used for badges. Unmatched entries have no
 * item id to key by and are left out. If two queue entries resolve to the same Jellyfin item
 * (e.g. a retried download that shows up as two queue rows before Sonarr/Radarr cleans up the old
 * one), the later entry wins - [toMap] keeps the last occurrence of a duplicate key.
 */
fun List<PvrQueueEntry>.toQueueStatusMap(): Map<UUID, QueueStatus> =
    mapNotNull { entry -> entry.item?.let { it.id to entry.status } }.toMap()

fun List<PvrQueueEntry>.toRadarrQueueStatusMap(): Map<Int, QueueStatus> =
    filter { it.status.source == PvrSource.RADARR }
        .mapNotNull { entry -> entry.tmdbId?.let { it to entry.status } }
        .toMap()

fun List<PvrQueueEntry>.toSonarrQueueStatusMap(): Map<Int, QueueStatus> =
    filter { it.status.source == PvrSource.SONARR }
        .mapNotNull { entry -> entry.sonarrEpisodeId?.let { it to entry.status } }
        .toMap()

/**
 * "Series - S1E5" when the episode is identified, "Series - Season 1" for season-pack grabs
 * (no per-episode number), falling back to the release title Sonarr reports for the download.
 */
private fun sonarrQueueTitle(
    series: SonarrSeries?,
    item: SonarrQueueItem,
    episodeNumber: Int?,
): String {
    val seriesTitle = series?.title?.takeIf { it.isNotBlank() }
    return when {
        seriesTitle != null && episodeNumber != null ->
            "$seriesTitle - S${item.seasonNumber}E$episodeNumber"
        seriesTitle != null && item.seasonNumber != 0 -> "$seriesTitle - Season ${item.seasonNumber}"
        seriesTitle != null -> seriesTitle
        else -> item.title ?: UNKNOWN_TITLE
    }
}

private const val UNKNOWN_TITLE = "Unknown"

private fun List<PvrImage>.posterUrl(): String? =
    firstOrNull { it.coverType.equals("poster", ignoreCase = true) }?.let { it.remoteUrl ?: it.url }

private fun SonarrQueueItem.toQueueStatus(): QueueStatus =
    buildQueueStatus(
        source = PvrSource.SONARR,
        status = status,
        trackedDownloadStatus = trackedDownloadStatus,
        trackedDownloadState = trackedDownloadState,
        size = size,
        sizeleft = sizeleft,
        timeleft = timeleft,
        errorMessage = errorMessage,
    )

private fun RadarrQueueItem.toQueueStatus(): QueueStatus =
    buildQueueStatus(
        source = PvrSource.RADARR,
        status = status,
        trackedDownloadStatus = trackedDownloadStatus,
        trackedDownloadState = trackedDownloadState,
        size = size,
        sizeleft = sizeleft,
        timeleft = timeleft,
        errorMessage = errorMessage,
    )

private fun buildQueueStatus(
    source: PvrSource,
    status: String?,
    trackedDownloadStatus: String?,
    trackedDownloadState: String?,
    size: Long,
    sizeleft: Long,
    timeleft: String?,
    errorMessage: String?,
): QueueStatus {
    val etaSeconds = parseTimeleftSeconds(timeleft)
    val percent = if (size > 0) (((size - sizeleft) * 100) / size).toInt().coerceIn(0, 100) else -1
    val speedBytesPerSecond = if (etaSeconds > 0 && sizeleft > 0) sizeleft / etaSeconds else 0L
    return QueueStatus(
        source = source,
        status = mapQueueItemStatus(status, trackedDownloadStatus, trackedDownloadState),
        percent = percent,
        sizeBytes = size,
        remainingBytes = sizeleft,
        speedBytesPerSecond = speedBytesPerSecond,
        etaSeconds = etaSeconds,
        errorMessage = errorMessage,
    )
}

/**
 * Sonarr and Radarr share the same queue item status vocabulary:
 * - `status`: "queued" / "delay" / "paused" / "downloading" / "completed" / "failed" / "warning"
 * - `trackedDownloadStatus`: "ok" / "warning" / "error" - an overlay on top of `status` describing
 *   whether the *tracked* download (post-grab import tracking) is healthy.
 * - `trackedDownloadState`: "downloading" / "importPending" / "importing" / "imported" /
 *   "failedPending" / "failed"
 *
 * `trackedDownloadStatus` signals problems regardless of the coarser `status` field, so it's
 * checked first; otherwise `status`/`trackedDownloadState` together decide between
 * queued/downloading/importing.
 */
internal fun mapQueueItemStatus(
    status: String?,
    trackedDownloadStatus: String?,
    trackedDownloadState: String?,
): QueueItemStatus {
    val normalizedStatus = status?.lowercase()
    val normalizedTrackedStatus = trackedDownloadStatus?.lowercase()
    val normalizedTrackedState = trackedDownloadState?.lowercase()

    if (normalizedTrackedStatus == "error") return QueueItemStatus.FAILED
    if (normalizedTrackedStatus == "warning") return QueueItemStatus.WARNING

    return when {
        normalizedStatus == "failed" || normalizedTrackedState in FAILED_STATES ->
            QueueItemStatus.FAILED
        normalizedStatus == "warning" -> QueueItemStatus.WARNING
        normalizedStatus == "completed" || normalizedTrackedState in IMPORTING_STATES ->
            QueueItemStatus.IMPORTING
        normalizedStatus == "downloading" -> QueueItemStatus.DOWNLOADING
        else -> QueueItemStatus.QUEUED
    }
}

private val FAILED_STATES = setOf("failed", "failedpending")
private val IMPORTING_STATES = setOf("importpending", "importing", "imported")

/** Parses Sonarr/Radarr's `timeleft` duration string ("HH:MM:SS", sometimes "D.HH:MM:SS"). */
internal fun parseTimeleftSeconds(timeleft: String?): Long {
    if (timeleft.isNullOrBlank()) return -1L
    val dayAndRest = timeleft.split(".", limit = 2)
    val (days, clock) = if (dayAndRest.size == 2) dayAndRest[0] to dayAndRest[1] else "0" to dayAndRest[0]
    val parts = clock.split(":").mapNotNull { it.toLongOrNull() }
    if (parts.size != 3) return -1L
    val daysLong = days.toLongOrNull() ?: 0L
    val (hours, minutes, seconds) = parts
    return daysLong * 86_400L + hours * 3_600L + minutes * 60L + seconds
}
