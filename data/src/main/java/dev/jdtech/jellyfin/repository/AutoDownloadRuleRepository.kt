package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.AutoDownloadRuleDto
import java.util.UUID

interface AutoDownloadRuleRepository {
    suspend fun setShowRuleEnabled(
        serverId: String,
        userId: UUID,
        seriesId: UUID,
        enabled: Boolean,
    ): AutoDownloadRuleDto

    suspend fun setSeasonRuleEnabled(
        serverId: String,
        userId: UUID,
        seriesId: UUID,
        seasonId: UUID,
        enabled: Boolean,
    ): AutoDownloadRuleDto

    suspend fun isShowRuleEnabled(serverId: String, userId: UUID, seriesId: UUID): Boolean

    suspend fun isSeasonRuleEnabled(
        serverId: String,
        userId: UUID,
        seriesId: UUID,
        seasonId: UUID,
    ): Boolean

    suspend fun getRules(serverId: String, userId: UUID): List<AutoDownloadRuleDto>

    suspend fun getEnabledRules(serverId: String, userId: UUID): List<AutoDownloadRuleDto>

    suspend fun setRuleEnabled(id: Long, enabled: Boolean)

    suspend fun setRuleOnlyNewEpisodes(id: Long, onlyNewEpisodes: Boolean)

    suspend fun setRuleOnlyUnwatched(id: Long, onlyUnwatched: Boolean)

    /**
     * Replaces every existing rule for [seriesId] with exactly the rules implied by
     * [entireShow]/[seasonIds]: a single show-level rule if [entireShow], otherwise one
     * season-level rule per id in [seasonIds] (any other existing rule for the show, including a
     * stale show-level one, is dropped). [onlyNewEpisodes]/[onlyUnwatched] are applied to every
     * resulting rule.
     */
    suspend fun reconcileRules(
        serverId: String,
        userId: UUID,
        seriesId: UUID,
        entireShow: Boolean,
        seasonIds: Set<UUID>,
        onlyNewEpisodes: Boolean,
        onlyUnwatched: Boolean,
    ): List<AutoDownloadRuleDto>

    suspend fun deleteRule(id: Long)

    suspend fun deleteRulesForShow(serverId: String, userId: UUID, seriesId: UUID)

    suspend fun deleteSeasonRule(serverId: String, userId: UUID, seriesId: UUID, seasonId: UUID)

    suspend fun deleteAllRules(serverId: String, userId: UUID)
}
