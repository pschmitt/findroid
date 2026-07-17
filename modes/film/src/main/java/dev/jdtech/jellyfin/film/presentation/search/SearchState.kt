package dev.jdtech.jellyfin.film.presentation.search

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.QueueStatus
import dev.jdtech.jellyfin.models.SeerrSearchItem

data class SearchState(
    val items: List<FindroidItem> = emptyList(),
    val seerrResults: List<SeerrSearchItem> = emptyList(),
    val loading: Boolean = false,
    val seerrSearching: Boolean = false,
    val radarrQueueStatus: Map<Int, QueueStatus> = emptyMap(),
)
