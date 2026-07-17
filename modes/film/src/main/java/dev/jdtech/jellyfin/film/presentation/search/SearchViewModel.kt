package dev.jdtech.jellyfin.film.presentation.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.pvr.PvrConfiguration
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.repository.QueueStatusRepository
import dev.jdtech.jellyfin.repository.SeerrRepository
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
                    val seerrResults =
                        if (pvrConfiguration.isSeerrConfigured()) {
                            seerrRepository.search(query).getOrDefault(emptyList())
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
