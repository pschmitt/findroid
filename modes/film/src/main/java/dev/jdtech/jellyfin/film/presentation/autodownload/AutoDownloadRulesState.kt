package dev.jdtech.jellyfin.film.presentation.autodownload

import dev.jdtech.jellyfin.models.UiText
import java.util.UUID

data class AutoDownloadShowRuleUiModel(
    val seriesId: UUID,
    // every rule id covering this show - a season-specific row per selected season, plus an
    // optional show-level (seasonId == null) row when alsoFutureSeasons is on. These coexist by
    // design; they're not alternatives to each other.
    val ruleIds: List<Long>,
    val showName: String,
    val enabled: Boolean,
    val seasonIds: Set<UUID>,
    val alsoFutureSeasons: Boolean,
    val scopeLabel: UiText,
    val onlyNewEpisodes: Boolean,
    val onlyUnwatched: Boolean,
)

data class AutoDownloadRulesState(
    val shows: List<AutoDownloadShowRuleUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
