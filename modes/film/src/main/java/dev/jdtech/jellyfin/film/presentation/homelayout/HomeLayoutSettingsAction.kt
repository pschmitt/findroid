package dev.jdtech.jellyfin.film.presentation.homelayout

sealed interface HomeLayoutSettingsAction {
    data class OnMoveUp(val index: Int) : HomeLayoutSettingsAction

    data class OnMoveDown(val index: Int) : HomeLayoutSettingsAction
}
