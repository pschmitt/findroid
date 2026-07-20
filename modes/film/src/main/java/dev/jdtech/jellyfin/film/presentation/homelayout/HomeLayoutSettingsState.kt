package dev.jdtech.jellyfin.film.presentation.homelayout

import dev.jdtech.jellyfin.models.UiText

data class HomeLayoutRow(val key: String, val label: UiText)

data class HomeLayoutSettingsState(
    val rows: List<HomeLayoutRow> = emptyList(),
    val hiddenRows: List<HomeLayoutRow> = emptyList(),
    val isLoading: Boolean = false,
)
