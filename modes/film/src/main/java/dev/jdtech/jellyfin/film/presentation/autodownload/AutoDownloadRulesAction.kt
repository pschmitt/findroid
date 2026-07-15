package dev.jdtech.jellyfin.film.presentation.autodownload

import java.util.UUID

sealed interface AutoDownloadRulesAction {
    data class ToggleShowRule(val seriesId: UUID, val enabled: Boolean) : AutoDownloadRulesAction

    data class UpdateShowRule(
        val seriesId: UUID,
        val seasonIds: Set<UUID>,
        val alsoFutureSeasons: Boolean,
        val onlyNewEpisodes: Boolean,
        val onlyUnwatched: Boolean,
    ) : AutoDownloadRulesAction

    data class DeleteShowRule(val seriesId: UUID, val alsoDeleteDownloads: Boolean) :
        AutoDownloadRulesAction

    data object OnBackClick : AutoDownloadRulesAction
}
