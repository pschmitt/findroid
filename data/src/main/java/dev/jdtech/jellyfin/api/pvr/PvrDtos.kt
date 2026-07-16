package dev.jdtech.jellyfin.api.pvr

import kotlinx.serialization.Serializable

/**
 * Response DTOs for the Sonarr/Radarr v3 REST APIs. Only the fields Findroid actually needs are
 * modeled - both APIs return many more fields per record, so every DTO here relies on the caller
 * decoding with `ignoreUnknownKeys = true` (see [PvrJson]).
 */

// region Sonarr - GET /api/v3/series

@Serializable
data class SonarrSeries(val id: Int, val tvdbId: Int = 0, val title: String = "")

// endregion

// region Radarr - GET /api/v3/movie

@Serializable
data class RadarrMovie(val id: Int, val tmdbId: Int = 0, val title: String = "")

// endregion

// region Sonarr - GET /api/v3/queue
// Sonarr's queue entries are per-episode: seriesId + episodeId + seasonNumber identify what's
// being grabbed.

@Serializable
data class SonarrQueueItem(
    val id: Int,
    val seriesId: Int = 0,
    val episodeId: Int = 0,
    val seasonNumber: Int = 0,
    val title: String? = null,
    val status: String? = null,
    val trackedDownloadStatus: String? = null,
    val trackedDownloadState: String? = null,
    val size: Long = 0L,
    val sizeleft: Long = 0L,
    val timeleft: String? = null,
    val estimatedCompletionTime: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class SonarrQueueResponse(
    val page: Int = 1,
    val pageSize: Int = 0,
    val totalRecords: Int = 0,
    val records: List<SonarrQueueItem> = emptyList(),
)

// endregion

// region Radarr - GET /api/v3/queue
// Radarr's queue entries are per-movie: movieId identifies what's being grabbed (no
// season/episode concept).

@Serializable
data class RadarrQueueItem(
    val id: Int,
    val movieId: Int = 0,
    val title: String? = null,
    val status: String? = null,
    val trackedDownloadStatus: String? = null,
    val trackedDownloadState: String? = null,
    val size: Long = 0L,
    val sizeleft: Long = 0L,
    val timeleft: String? = null,
    val estimatedCompletionTime: String? = null,
    val errorMessage: String? = null,
)

@Serializable
data class RadarrQueueResponse(
    val page: Int = 1,
    val pageSize: Int = 0,
    val totalRecords: Int = 0,
    val records: List<RadarrQueueItem> = emptyList(),
)

// endregion
