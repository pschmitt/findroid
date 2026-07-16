package dev.jdtech.jellyfin.film.presentation.season

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.QueueStatus
import dev.jdtech.jellyfin.models.UpcomingEpisode
import dev.jdtech.jellyfin.repository.ExistingAutoDownloadScope
import dev.jdtech.jellyfin.utils.DownloadProgress
import java.util.UUID

data class SeasonState(
    val season: FindroidSeason? = null,
    val episodes: List<FindroidEpisode> = emptyList(),
    // Sonarr-known episodes of this season not yet in the Jellyfin library - always empty unless
    // Sonarr is configured and the show is matched (see SeasonEpisodesRepository). Rendered as
    // greyed-out placeholder rows after the real episodes, see SeasonScreen.
    val upcomingEpisodes: List<UpcomingEpisode> = emptyList(),
    val autoDownloadEnabled: Boolean = false,
    val existingScope: ExistingAutoDownloadScope = ExistingAutoDownloadScope(),
    val hasDownloads: Boolean = false,
    val downloadsSizeBytes: Long = 0L,
    val downloadProgress: Map<UUID, DownloadProgress> = emptyMap(),
    val queueStatus: Map<UUID, QueueStatus> = emptyMap(),
    val error: Exception? = null,
)
