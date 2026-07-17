package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.SeerrRequestItem
import dev.jdtech.jellyfin.models.SeerrSearchItem

/**
 * Discover/request flows backed by a Seerr/Seerr instance - the "add new content" side of
 * the PVR integration. Seerr owns the Sonarr/Radarr routing (quality profiles, root
 * folders), so Findroid only ever searches and files requests. All calls are user-initiated, so
 * failures are surfaced via [Result] rather than swallowed, same as the search repositories.
 */
interface SeerrRepository {
    /** TMDB-backed movie/series search, with each result's current availability in Seerr. */
    suspend fun search(query: String): Result<List<SeerrSearchItem>>

    /** Requests the item; series requests cover all seasons. */
    suspend fun request(item: SeerrSearchItem): Result<Unit>

    /** Most recent requests first, titles/posters already resolved. */
    suspend fun getRecentRequests(limit: Int = 20): Result<List<SeerrRequestItem>>
}
