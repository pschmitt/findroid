package dev.jdtech.jellyfin.film.presentation.search

import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.SeerrSearchItem

sealed interface SearchAction {
    data class Search(val query: String) : SearchAction

    data class OnItemClick(val item: FindroidItem) : SearchAction

    data class OnSeerrItemClick(val item: SeerrSearchItem) : SearchAction
}
