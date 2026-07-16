package dev.jdtech.jellyfin.film.presentation.media

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.repository.JellyfinRepository
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MediaViewModel
@Inject
constructor(private val repository: JellyfinRepository, private val appPreferences: AppPreferences) :
    ViewModel() {
    private val _state = MutableStateFlow(MediaState())
    val state = _state.asStateFlow()

    fun loadData() {
        viewModelScope.launch {
            _state.emit(
                _state.value.copy(
                    isLoading = true,
                    error = null,
                    showCalendarTab = isPvrConfigured(),
                )
            )
            try {
                val libraries = repository.getLibraries()
                _state.emit(_state.value.copy(libraries = libraries))
            } catch (e: Exception) {
                _state.emit(_state.value.copy(error = e))
            }
            _state.emit(_state.value.copy(isLoading = false))
        }
    }

    private fun isPvrConfigured(): Boolean {
        val sonarrConfigured =
            appPreferences.getValue(appPreferences.sonarrEnabled) &&
                !appPreferences.getValue(appPreferences.sonarrBaseUrl).isNullOrBlank()
        val radarrConfigured =
            appPreferences.getValue(appPreferences.radarrEnabled) &&
                !appPreferences.getValue(appPreferences.radarrBaseUrl).isNullOrBlank()
        return sonarrConfigured || radarrConfigured
    }

    fun onAction(action: MediaAction) {
        when (action) {
            is MediaAction.OnRetryClick -> {
                loadData()
            }
            else -> Unit
        }
    }
}
