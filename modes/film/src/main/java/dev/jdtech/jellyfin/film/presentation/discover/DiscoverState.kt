package dev.jdtech.jellyfin.film.presentation.discover

import dev.jdtech.jellyfin.models.SeerrRequestItem
import dev.jdtech.jellyfin.models.SeerrSearchItem

data class DiscoverState(
    val query: String = "",
    val isSearching: Boolean = false,
    val results: List<SeerrSearchItem> = emptyList(),
    val recentRequests: List<SeerrRequestItem> = emptyList(),
    // TMDB ids the user requested during this screen's lifetime - overlays the (now stale)
    // status carried by the search results, so a just-requested item immediately reads as
    // "Requested" instead of still offering the button.
    val requestedTmdbIds: Set<Int> = emptySet(),
    // Search or requests-list failure, already user-presentable (names Seerr).
    val error: String? = null,
)

/** One-shot feedback for a request action, shown as a toast. */
sealed interface DiscoverEvent {
    data class Requested(val title: String) : DiscoverEvent

    data class Failed(val message: String?) : DiscoverEvent
}
