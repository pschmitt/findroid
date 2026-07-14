package dev.jdtech.jellyfin.film.presentation.autodownload

import dev.jdtech.jellyfin.models.UiText
import java.util.UUID

data class AutoDownloadShowRuleUiModel(
    val seriesId: UUID,
    // all rule ids currently covering this show (normally exactly one; can be more if rules
    // predate the "one active rule per show" invariant, until the group is next edited)
    val ruleIds: List<Long>,
    val showName: String,
    val enabled: Boolean,
    val entireShow: Boolean,
    val seasonIds: Set<UUID>,
    val scopeLabel: UiText,
    val onlyNewEpisodes: Boolean,
    val onlyUnwatched: Boolean,
)

data class AutoDownloadRulesState(
    val shows: List<AutoDownloadShowRuleUiModel> = emptyList(),
    val isLoading: Boolean = false,
    val error: Exception? = null,
)
