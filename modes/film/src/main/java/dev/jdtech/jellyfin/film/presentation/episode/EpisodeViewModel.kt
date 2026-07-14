package dev.jdtech.jellyfin.film.presentation.episode

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.core.presentation.downloader.DownloadScope
import dev.jdtech.jellyfin.database.ServerDatabaseDao
import dev.jdtech.jellyfin.film.domain.VideoMetadataParser
import dev.jdtech.jellyfin.models.AutoDownloadRuleDto
import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.repository.AutoDownloadRuleRepository
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import dev.jdtech.jellyfin.utils.AutoDownloadRuleEvaluator
import dev.jdtech.jellyfin.utils.Downloader
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jellyfin.sdk.model.api.PersonKind

@HiltViewModel
class EpisodeViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val appPreferences: AppPreferences,
    private val videoMetadataParser: VideoMetadataParser,
    private val database: ServerDatabaseDao,
    private val downloader: Downloader,
    private val autoDownloadRuleRepository: AutoDownloadRuleRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(EpisodeState())
    val state = _state.asStateFlow()

    private val evaluator = AutoDownloadRuleEvaluator()

    lateinit var episodeId: UUID

    fun loadEpisode(episodeId: UUID) {
        this.episodeId = episodeId
        viewModelScope.launch {
            try {
                val episode = repository.getEpisode(episodeId)
                val videoMetadata = videoMetadataParser.parse(episode.sources.first())
                val actors = getActors(episode)
                val displayExtraInfo = appPreferences.getValue(appPreferences.displayExtraInfo)
                _state.emit(
                    _state.value.copy(
                        episode = episode,
                        videoMetadata = videoMetadata,
                        actors = actors,
                        displayExtraInfo = displayExtraInfo,
                    )
                )
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
        }
    }

    private suspend fun getActors(item: FindroidEpisode): List<FindroidItemPerson> {
        return withContext(Dispatchers.Default) {
            item.people.filter { it.type == PersonKind.ACTOR }
        }
    }

    private fun downloadWithScope(scope: DownloadScope, alsoFollowNew: Boolean) {
        val episode = _state.value.episode ?: return
        viewModelScope.launch {
            val serverId = appPreferences.getValue(appPreferences.currentServer) ?: return@launch
            val userId = repository.getUserId()
            val ruleSeasonId = if (scope == DownloadScope.SEASON) episode.seasonId else null
            val transientRule =
                AutoDownloadRuleDto(
                    serverId = serverId,
                    userId = userId,
                    seriesId = episode.seriesId,
                    seasonId = ruleSeasonId,
                    enabled = true,
                    createdAt = System.currentTimeMillis(),
                )
            evaluator.evaluate(transientRule, database, repository, downloader)
            if (alsoFollowNew) {
                if (scope == DownloadScope.SEASON) {
                    autoDownloadRuleRepository.setSeasonRuleEnabled(
                        serverId,
                        userId,
                        episode.seriesId,
                        episode.seasonId,
                        true,
                    )
                } else {
                    autoDownloadRuleRepository.setShowRuleEnabled(
                        serverId,
                        userId,
                        episode.seriesId,
                        true,
                    )
                }
            }
        }
    }

    fun onAction(action: EpisodeAction) {
        when (action) {
            is EpisodeAction.MarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsPlayed(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.UnmarkAsPlayed -> {
                viewModelScope.launch {
                    repository.markAsUnplayed(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.MarkAsFavorite -> {
                viewModelScope.launch {
                    repository.markAsFavorite(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.UnmarkAsFavorite -> {
                viewModelScope.launch {
                    repository.unmarkAsFavorite(episodeId)
                    loadEpisode(episodeId)
                }
            }
            is EpisodeAction.DownloadWithScope ->
                downloadWithScope(action.scope, action.alsoFollowNew)
            else -> Unit
        }
    }
}
