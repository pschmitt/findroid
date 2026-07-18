package dev.jdtech.jellyfin.film.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.pvr.PvrConfiguration
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.QueueStatusRepository
import dev.jdtech.jellyfin.repository.SeerrRepository
import dev.jdtech.jellyfin.models.tmdbIdOrNull
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class SearchViewModel
@Inject
constructor(
    private val repository: JellyfinRepository,
    private val seerrRepository: SeerrRepository,
    private val pvrConfiguration: PvrConfiguration,
    private val queueStatusRepository: QueueStatusRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(SearchState())
    val state = _state.asStateFlow()

    var currentJob: Job? = null

    init {
        viewModelScope.launch {
            queueStatusRepository.getRadarrQueueStatusFlow().collect { statuses ->
                _state.value = _state.value.copy(radarrQueueStatus = statuses)
            }
        }
    }

    private fun search(query: String) {
        currentJob?.cancel()
        currentJob =
            viewModelScope.launch {
                try {
                    if (query.isBlank()) {
                        _state.emit(SearchState(radarrQueueStatus = _state.value.radarrQueueStatus))
                        return@launch
                    }

                    _state.emit(
                        SearchState(
                            loading = true,
                            seerrSearching = pvrConfiguration.isSeerrConfigured(),
                            radarrQueueStatus = _state.value.radarrQueueStatus,
                        )
                    )
                    val items = repository.getSearchItems(query)
                    // Hide Seerr results already in the Jellyfin library right above them - a
                    // Seerr result is only useful here as "not on your server yet, want to
                    // request it?", so one that's already a library hit is a plain duplicate.
                    val libraryTmdbIds = items.mapNotNull { it.tmdbIdOrNull() }.toSet()
                    val seerrResults =
                        if (pvrConfiguration.isSeerrConfigured()) {
                            seerrRepository
                                .search(query)
                                .getOrDefault(emptyList())
                                .filter { it.tmdbId !in libraryTmdbIds }
                        } else {
                            emptyList()
                        }

                    _state.emit(
                        SearchState(
                            items = items,
                            seerrResults = seerrResults,
                            loading = false,
                            radarrQueueStatus = _state.value.radarrQueueStatus,
                        )
                    )
                } catch (_: CancellationException) {} catch (e: Exception) {
                    Timber.e(e)
                    _state.emit(_state.value.copy(loading = false))
                }
            }
    }

    fun onAction(action: SearchAction) {
        when (action) {
            is SearchAction.Search -> {
                search(query = action.query)
            }
            else -> Unit
        }
    }
}
