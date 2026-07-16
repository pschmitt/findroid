package dev.jdtech.jellyfin.presentation.settings.integrations

data class IntegrationsSettingsState(
    val sonarrEnabled: Boolean = false,
    val sonarrBaseUrl: String = "",
    val sonarrApiKey: String = "",
    val sonarrTestState: PvrTestState = PvrTestState.Idle,
    val radarrEnabled: Boolean = false,
    val radarrBaseUrl: String = "",
    val radarrApiKey: String = "",
    val radarrTestState: PvrTestState = PvrTestState.Idle,
    val jellyseerrEnabled: Boolean = false,
    val jellyseerrBaseUrl: String = "",
    val jellyseerrApiKey: String = "",
    val jellyseerrTestState: PvrTestState = PvrTestState.Idle,
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
