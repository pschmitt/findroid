package dev.jdtech.jellyfin.film.presentation.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.pvr.SonarrRelease
import dev.jdtech.jellyfin.core.presentation.search.ReleasePickerState
import dev.jdtech.jellyfin.core.presentation.search.SearchEvent
import dev.jdtech.jellyfin.models.CalendarEntry
import dev.jdtech.jellyfin.repository.CalendarRepository
import dev.jdtech.jellyfin.repository.SonarrSearchRepository
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CalendarViewModel
@Inject
constructor(
    private val calendarRepository: CalendarRepository,
    private val sonarrSearchRepository: SonarrSearchRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(CalendarState())
    val state = _state.asStateFlow()

    private val searchEventsChannel = Channel<SearchEvent>()
    val searchEvents = searchEventsChannel.receiveAsFlow()

    init {
        load()
    }

    fun refresh() {
        load()
    }

    /** No-op when [entry] has no [CalendarEntry.episodeId] (Radarr entries, or an unmatched Sonarr
     * entry - see CalendarMatching.kt) - the UI only exposes this action for Sonarr entries. */
    fun searchEpisodeAutomatic(entry: CalendarEntry) {
        val episodeId = entry.episodeId ?: return
        viewModelScope.launch {
            val event =
                sonarrSearchRepository
                    .searchEpisode(episodeId)
                    .fold({ SearchEvent.SearchTriggered }, { SearchEvent.Failed(it.message) })
            searchEventsChannel.send(event)
        }
    }

    fun openReleasePicker(entry: CalendarEntry) {
        val episodeId = entry.episodeId ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(releasePicker = ReleasePickerState())
            val result = sonarrSearchRepository.getReleases(episodeId)
            _state.value =
                _state.value.copy(
                    releasePicker = result.getOrNull()?.let { ReleasePickerState(isLoading = false, releases = it) }
                )
            result.onFailure { searchEventsChannel.send(SearchEvent.Failed(it.message)) }
        }
    }

    fun grabRelease(release: SonarrRelease) {
        viewModelScope.launch {
            val result = sonarrSearchRepository.grabRelease(release)
            _state.value = _state.value.copy(releasePicker = null)
            searchEventsChannel.send(
                result.fold({ SearchEvent.ReleaseGrabbed }, { SearchEvent.Failed(it.message) })
            )
        }
    }

    fun dismissReleasePicker() {
        _state.value = _state.value.copy(releasePicker = null)
    }

    private fun load() {
        viewModelScope.launch {
            if (_state.value.isEmpty) {
                _state.value = _state.value.copy(isLoading = true, error = null)
            }
            try {
                val entries = calendarRepository.getUpcoming()
                _state.value =
                    _state.value.copy(
                        isLoading = false,
                        error = null,
                        groupedEntries = groupByDate(entries),
                    )
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e)
            }
        }
    }
}

/**
 * Groups an already date-sorted (ascending) list of entries into consecutive same-date runs,
 * preserving order - a free function so it's directly unit-testable without a ViewModel/Hilt in
 * the loop, same convention as `buildPvrQueueGroups` in `DownloadsViewModel.kt`.
 */
internal fun groupByDate(entries: List<CalendarEntry>): List<Pair<LocalDate, List<CalendarEntry>>> =
    entries.groupBy { it.date }.map { (date, entriesForDate) -> date to entriesForDate }
