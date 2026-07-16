package dev.jdtech.jellyfin.presentation.settings.integrations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.jdtech.jellyfin.api.pvr.JellyseerrApi
import dev.jdtech.jellyfin.api.pvr.PvrCredentialKeys
import dev.jdtech.jellyfin.api.pvr.RadarrApi
import dev.jdtech.jellyfin.api.pvr.SonarrApi
import dev.jdtech.jellyfin.security.SecureCredentialStore
import dev.jdtech.jellyfin.settings.domain.AppPreferences
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class IntegrationsSettingsViewModel
@Inject
constructor(
    private val appPreferences: AppPreferences,
    private val secureCredentialStore: SecureCredentialStore,
) : ViewModel() {
    private val _state = MutableStateFlow(IntegrationsSettingsState())
    val state = _state.asStateFlow()

    // API keys are debounced instead of persisted per keystroke - every putString on
    // EncryptedSharedPreferences runs keystore crypto, which is pointless churn while the user is
    // still typing/pasting. Dirty flags let onCleared flush a pending write when the screen is
    // closed before the debounce fires.
    private val apiKeyPersistJobs = mutableMapOf<String, Job>()
    private val dirtyApiKeys = mutableSetOf<String>()

    fun load() {
        _state.value =
            IntegrationsSettingsState(
                sonarrEnabled = appPreferences.getValue(appPreferences.sonarrEnabled),
                sonarrBaseUrl = appPreferences.getValue(appPreferences.sonarrBaseUrl).orEmpty(),
                sonarrApiKey = secureCredentialStore.getString(PvrCredentialKeys.SONARR_API_KEY).orEmpty(),
                radarrEnabled = appPreferences.getValue(appPreferences.radarrEnabled),
                radarrBaseUrl = appPreferences.getValue(appPreferences.radarrBaseUrl).orEmpty(),
                radarrApiKey = secureCredentialStore.getString(PvrCredentialKeys.RADARR_API_KEY).orEmpty(),
                jellyseerrEnabled = appPreferences.getValue(appPreferences.jellyseerrEnabled),
                jellyseerrBaseUrl = appPreferences.getValue(appPreferences.jellyseerrBaseUrl).orEmpty(),
                jellyseerrApiKey =
                    secureCredentialStore.getString(PvrCredentialKeys.JELLYSEERR_API_KEY).orEmpty(),
                pvrPollIntervalMinutes =
                    appPreferences.getValue(appPreferences.pvrPollIntervalMinutes),
                pvrReleaseCacheMinutes =
                    appPreferences.getValue(appPreferences.pvrReleaseCacheMinutes),
            )
    }

    fun onAction(action: IntegrationsSettingsAction) {
        when (action) {
            is IntegrationsSettingsAction.OnBackClick -> Unit
            is IntegrationsSettingsAction.OnSonarrEnabledChanged -> {
                appPreferences.setValue(appPreferences.sonarrEnabled, action.enabled)
                _state.value = _state.value.copy(sonarrEnabled = action.enabled)
            }
            is IntegrationsSettingsAction.OnSonarrBaseUrlChanged -> {
                appPreferences.setValue(
                    appPreferences.sonarrBaseUrl,
                    action.baseUrl.ifBlank { null },
                )
                _state.value =
                    _state.value.copy(
                        sonarrBaseUrl = action.baseUrl,
                        sonarrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnSonarrApiKeyChanged -> {
                persistApiKeyDebounced(PvrCredentialKeys.SONARR_API_KEY, action.apiKey)
                _state.value =
                    _state.value.copy(
                        sonarrApiKey = action.apiKey,
                        sonarrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnTestSonarrConnection -> testSonarrConnection()
            is IntegrationsSettingsAction.OnRadarrEnabledChanged -> {
                appPreferences.setValue(appPreferences.radarrEnabled, action.enabled)
                _state.value = _state.value.copy(radarrEnabled = action.enabled)
            }
            is IntegrationsSettingsAction.OnRadarrBaseUrlChanged -> {
                appPreferences.setValue(
                    appPreferences.radarrBaseUrl,
                    action.baseUrl.ifBlank { null },
                )
                _state.value =
                    _state.value.copy(
                        radarrBaseUrl = action.baseUrl,
                        radarrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnRadarrApiKeyChanged -> {
                persistApiKeyDebounced(PvrCredentialKeys.RADARR_API_KEY, action.apiKey)
                _state.value =
                    _state.value.copy(
                        radarrApiKey = action.apiKey,
                        radarrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnTestRadarrConnection -> testRadarrConnection()
            is IntegrationsSettingsAction.OnJellyseerrEnabledChanged -> {
                appPreferences.setValue(appPreferences.jellyseerrEnabled, action.enabled)
                _state.value = _state.value.copy(jellyseerrEnabled = action.enabled)
            }
            is IntegrationsSettingsAction.OnJellyseerrBaseUrlChanged -> {
                appPreferences.setValue(
                    appPreferences.jellyseerrBaseUrl,
                    action.baseUrl.ifBlank { null },
                )
                _state.value =
                    _state.value.copy(
                        jellyseerrBaseUrl = action.baseUrl,
                        jellyseerrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnJellyseerrApiKeyChanged -> {
                persistApiKeyDebounced(PvrCredentialKeys.JELLYSEERR_API_KEY, action.apiKey)
                _state.value =
                    _state.value.copy(
                        jellyseerrApiKey = action.apiKey,
                        jellyseerrTestState = PvrTestState.Idle,
                    )
            }
            is IntegrationsSettingsAction.OnTestJellyseerrConnection -> testJellyseerrConnection()
            is IntegrationsSettingsAction.OnPollIntervalChanged -> {
                appPreferences.setValue(appPreferences.pvrPollIntervalMinutes, action.minutes)
                _state.value = _state.value.copy(pvrPollIntervalMinutes = action.minutes)
            }
            is IntegrationsSettingsAction.OnReleaseCacheChanged -> {
                appPreferences.setValue(appPreferences.pvrReleaseCacheMinutes, action.minutes)
                _state.value = _state.value.copy(pvrReleaseCacheMinutes = action.minutes)
            }
        }
    }

    private fun persistApiKeyDebounced(credentialKey: String, value: String) {
        dirtyApiKeys.add(credentialKey)
        apiKeyPersistJobs[credentialKey]?.cancel()
        apiKeyPersistJobs[credentialKey] =
            viewModelScope.launch {
                delay(API_KEY_PERSIST_DEBOUNCE_MS)
                secureCredentialStore.putString(credentialKey, value.ifBlank { null })
                dirtyApiKeys.remove(credentialKey)
            }
    }

    override fun onCleared() {
        super.onCleared()
        apiKeyPersistJobs.values.forEach { it.cancel() }
        // Flush anything still pending so closing the screen right after typing doesn't lose it.
        if (PvrCredentialKeys.SONARR_API_KEY in dirtyApiKeys) {
            secureCredentialStore.putString(
                PvrCredentialKeys.SONARR_API_KEY,
                _state.value.sonarrApiKey.ifBlank { null },
            )
        }
        if (PvrCredentialKeys.RADARR_API_KEY in dirtyApiKeys) {
            secureCredentialStore.putString(
                PvrCredentialKeys.RADARR_API_KEY,
                _state.value.radarrApiKey.ifBlank { null },
            )
        }
        if (PvrCredentialKeys.JELLYSEERR_API_KEY in dirtyApiKeys) {
            secureCredentialStore.putString(
                PvrCredentialKeys.JELLYSEERR_API_KEY,
                _state.value.jellyseerrApiKey.ifBlank { null },
            )
        }
    }

    private fun testSonarrConnection() {
        val baseUrl = _state.value.sonarrBaseUrl
        val apiKey = _state.value.sonarrApiKey
        _state.value = _state.value.copy(sonarrTestState = PvrTestState.Testing)
        viewModelScope.launch {
            val result =
                try {
                    val series = SonarrApi(baseUrl, apiKey).getSeries()
                    PvrTestState.Success(series.size)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PvrTestState.Error(e.message ?: e.toString())
                }
            _state.value = _state.value.copy(sonarrTestState = result)
        }
    }

    private fun testRadarrConnection() {
        val baseUrl = _state.value.radarrBaseUrl
        val apiKey = _state.value.radarrApiKey
        _state.value = _state.value.copy(radarrTestState = PvrTestState.Testing)
        viewModelScope.launch {
            val result =
                try {
                    val movies = RadarrApi(baseUrl, apiKey).getMovie()
                    PvrTestState.Success(movies.size)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PvrTestState.Error(e.message ?: e.toString())
                }
            _state.value = _state.value.copy(radarrTestState = result)
        }
    }

    private fun testJellyseerrConnection() {
        val baseUrl = _state.value.jellyseerrBaseUrl
        val apiKey = _state.value.jellyseerrApiKey
        _state.value = _state.value.copy(jellyseerrTestState = PvrTestState.Testing)
        viewModelScope.launch {
            val result =
                try {
                    val api = JellyseerrApi(baseUrl, apiKey)
                    // auth/me validates the key; the request count doubles as the "N items" the
                    // shared success message expects.
                    api.getCurrentUser()
                    PvrTestState.Success(api.getRequests(take = 1).pageInfo.results)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    PvrTestState.Error(e.message ?: e.toString())
                }
            _state.value = _state.value.copy(jellyseerrTestState = result)
        }
    }

    private companion object {
        const val API_KEY_PERSIST_DEBOUNCE_MS = 750L
    }
}
