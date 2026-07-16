package dev.jdtech.jellyfin.models

import java.time.LocalDate

/**
 * A Sonarr-known episode of a specific season that isn't in the Jellyfin library yet - shown as a
 * placeholder row on the Season screen so users can see what's still coming, not just what's
 * already there. See [dev.jdtech.jellyfin.repository.SeasonEpisodesRepository].
 */
data class UpcomingEpisode(
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String?,
    val airDate: LocalDate?,
    val hasFile: Boolean,
    val monitored: Boolean,
)
