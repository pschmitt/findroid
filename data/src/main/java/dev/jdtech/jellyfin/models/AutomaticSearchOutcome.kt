package dev.jdtech.jellyfin.models

/**
 * Result of waiting for a Sonarr automatic search (triggered via
 * [dev.jdtech.jellyfin.repository.SonarrSearchRepository.searchEpisode]) to finish, so the app can
 * notify the user instead of leaving them to guess whether/when it completed.
 */
data class AutomaticSearchOutcome(
    val succeeded: Boolean,
    val seriesTitle: String?,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String?,
)
