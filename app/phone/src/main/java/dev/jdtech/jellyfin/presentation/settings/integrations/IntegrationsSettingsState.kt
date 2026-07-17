package dev.jdtech.jellyfin.presentation.settings.integrations

import dev.jdtech.jellyfin.models.ServerWithAddresses
import dev.jdtech.jellyfin.models.UiText
import dev.jdtech.jellyfin.models.User

data class IntegrationsSettingsState(
    val jellyfinServers: List<ServerWithAddresses> = emptyList(),
    val jellyfinUsers: List<User> = emptyList(),
    val currentServerId: String? = null,
    val currentUserId: String? = null,
    val jellyfinOperationInProgress: Boolean = false,
    val jellyfinError: UiText? = null,
    val sonarrEnabled: Boolean = false,
    val sonarrBaseUrl: String = "",
    val sonarrApiKey: String = "",
    val sonarrHttpHeaders: String = "",
    val sonarrBasicAuthUsername: String = "",
    val sonarrBasicAuthPassword: String = "",
    val sonarrTestState: PvrTestState = PvrTestState.Idle,
    val radarrEnabled: Boolean = false,
    val radarrBaseUrl: String = "",
    val radarrApiKey: String = "",
    val radarrHttpHeaders: String = "",
    val radarrBasicAuthUsername: String = "",
    val radarrBasicAuthPassword: String = "",
    val radarrTestState: PvrTestState = PvrTestState.Idle,
    val seerrEnabled: Boolean = false,
    val seerrBaseUrl: String = "",
    val seerrApiKey: String = "",
    val seerrHttpHeaders: String = "",
    val seerrBasicAuthUsername: String = "",
    val seerrBasicAuthPassword: String = "",
    val seerrTestState: PvrTestState = PvrTestState.Idle,
    val pvrPollIntervalMinutes: Int = 15,
    val pvrReleaseCacheMinutes: Int = 15,
)

/** Result of a one-shot "Test connection" call - surfaced as inline status text, not a Snackbar. */
sealed interface PvrTestState {
    data object Idle : PvrTestState

    data object Testing : PvrTestState

    data class Success(val itemCount: Int) : PvrTestState

    data class Error(val message: String) : PvrTestState
}
