package dev.jdtech.jellyfin.film.presentation.discover

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.models.SeerrSearchItem
import dev.jdtech.jellyfin.repository.JellyseerrRepository
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DiscoverViewModel
@Inject
constructor(private val jellyseerrRepository: JellyseerrRepository) : ViewModel() {
    private val _state = MutableStateFlow(DiscoverState())
    val state = _state.asStateFlow()

    private val eventsChannel = Channel<DiscoverEvent>()
    val events = eventsChannel.receiveAsFlow()

    private var searchJob: Job? = null

    init {
        loadRecentRequests()
    }

    fun onQueryChanged(query: String) {
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _state.value = _state.value.copy(results = emptyList(), isSearching = false, error = null)
            return
        }
        // Debounced so a fast typist doesn't fire one TMDB search per keystroke.
        searchJob =
            viewModelScope.launch {
                delay(SEARCH_DEBOUNCE_MS)
                _state.value = _state.value.copy(isSearching = true, error = null)
                val result = jellyseerrRepository.search(query)
                result.fold(
                    onSuccess = { items ->
                        _state.value = _state.value.copy(isSearching = false, results = items)
                    },
                    onFailure = { e ->
                        _state.value =
                            _state.value.copy(
                                isSearching = false,
                                results = emptyList(),
                                error = e.message,
                            )
                    },
                )
            }
    }

    fun request(item: SeerrSearchItem) {
        viewModelScope.launch {
            jellyseerrRepository
                .request(item)
                .fold(
                    onSuccess = {
                        _state.value =
                            _state.value.copy(
                                requestedTmdbIds = _state.value.requestedTmdbIds + item.tmdbId
                            )
                        eventsChannel.send(DiscoverEvent.Requested(item.title))
                        loadRecentRequests()
                    },
                    onFailure = { e -> eventsChannel.send(DiscoverEvent.Failed(e.message)) },
                )
        }
    }

    fun refreshRequests() {
        loadRecentRequests()
    }

    private fun loadRecentRequests() {
        viewModelScope.launch {
            jellyseerrRepository
                .getRecentRequests()
                .fold(
                    onSuccess = { requests ->
                        _state.value = _state.value.copy(recentRequests = requests)
                    },
                    onFailure = { e ->
                        // Only surface the failure when there's nothing else on screen - a stale
                        // requests list under fresh search results isn't worth an error banner.
                        if (_state.value.results.isEmpty() && _state.value.query.isBlank()) {
                            _state.value = _state.value.copy(error = e.message)
                        }
                    },
                )
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 400L
    }
}
