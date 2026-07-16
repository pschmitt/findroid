package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.UpcomingEpisode

/**
 * Exposes Sonarr-known episodes of a specific show/season that aren't in the Jellyfin library
 * yet - what the Season screen shows as "upcoming" placeholder rows alongside real episodes.
 * Unlike [CalendarRepository] (a global date-range view across every show), this is scoped to one
 * series - matching it to a Sonarr series happens internally via tvdbId, the same join
 * [dev.jdtech.jellyfin.repository.matchSonarrCalendar] uses.
 */
interface SeasonEpisodesRepository {
    /**
     * [seriesTvdbId] identifies the show (from the matching
     * [dev.jdtech.jellyfin.models.FindroidShow.tvdbId]), [seasonNumber] the season, and
     * [knownEpisodeNumbers] the episode numbers already present in the Jellyfin library for that
     * season - anything Sonarr knows about beyond that set is returned. Empty (not an error) when
     * Sonarr isn't configured, the show isn't tracked by Sonarr, or nothing is missing.
     */
    suspend fun getUpcomingEpisodes(
        seriesTvdbId: String,
        seasonNumber: Int,
        knownEpisodeNumbers: Set<Int>,
    ): List<UpcomingEpisode>
}
