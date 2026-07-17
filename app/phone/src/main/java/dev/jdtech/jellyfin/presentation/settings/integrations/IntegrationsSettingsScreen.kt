package dev.jdtech.jellyfin.presentation.settings.integrations

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.settings.R as SettingsR

@Composable
fun IntegrationsSettingsScreen(
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: IntegrationsSettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(true) { viewModel.load() }

    IntegrationsSettingsScreenLayout(
        state = state,
        navigateToServers = navigateToServers,
        navigateToUsers = navigateToUsers,
        onAction = { action ->
            when (action) {
                is IntegrationsSettingsAction.OnBackClick -> navigateBack()
                else -> viewModel.onAction(action)
            }
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IntegrationsSettingsScreenLayout(
    state: IntegrationsSettingsState,
    navigateToServers: () -> Unit,
    navigateToUsers: () -> Unit,
    onAction: (IntegrationsSettingsAction) -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(SettingsR.string.settings_category_connections)) },
                navigationIcon = {
                    IconButton(onClick = { onAction(IntegrationsSettingsAction.OnBackClick) }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_arrow_left),
                            contentDescription = null,
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier =
                Modifier.fillMaxWidth()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = navigateToServers, modifier = Modifier.weight(1f)) {
                    Text(stringResource(SettingsR.string.settings_category_servers))
                }
                OutlinedButton(onClick = navigateToUsers, modifier = Modifier.weight(1f)) {
                    Text(stringResource(SettingsR.string.users))
                }
            }

            HorizontalDivider()

            PvrServiceSection(
                nameRes = CoreR.string.integrations_sonarr,
                logoRes = CoreR.drawable.ic_sonarr,
                apiKeySettingsPath = "/settings/general",
                enabled = state.sonarrEnabled,
                baseUrl = state.sonarrBaseUrl,
                apiKey = state.sonarrApiKey,
                testState = state.sonarrTestState,
                onEnabledChanged = {
                    onAction(IntegrationsSettingsAction.OnSonarrEnabledChanged(it))
                },
                onBaseUrlChanged = {
                    onAction(IntegrationsSettingsAction.OnSonarrBaseUrlChanged(it))
                },
                onApiKeyChanged = {
                    onAction(IntegrationsSettingsAction.OnSonarrApiKeyChanged(it))
                },
                onTestConnectionClick = {
                    onAction(IntegrationsSettingsAction.OnTestSonarrConnection)
                },
            )

            HorizontalDivider()

            PvrServiceSection(
                nameRes = CoreR.string.integrations_radarr,
                logoRes = CoreR.drawable.ic_radarr,
                apiKeySettingsPath = "/settings/general",
                enabled = state.radarrEnabled,
                baseUrl = state.radarrBaseUrl,
                apiKey = state.radarrApiKey,
                testState = state.radarrTestState,
                onEnabledChanged = {
                    onAction(IntegrationsSettingsAction.OnRadarrEnabledChanged(it))
                },
                onBaseUrlChanged = {
                    onAction(IntegrationsSettingsAction.OnRadarrBaseUrlChanged(it))
                },
                onApiKeyChanged = {
                    onAction(IntegrationsSettingsAction.OnRadarrApiKeyChanged(it))
                },
                onTestConnectionClick = {
                    onAction(IntegrationsSettingsAction.OnTestRadarrConnection)
                },
            )

            HorizontalDivider()

            PvrServiceSection(
                nameRes = CoreR.string.integrations_seerr,
                logoRes = CoreR.drawable.ic_seerr,
                apiKeySettingsPath = "/settings",
                enabled = state.seerrEnabled,
                baseUrl = state.seerrBaseUrl,
                apiKey = state.seerrApiKey,
                testState = state.seerrTestState,
                onEnabledChanged = {
                    onAction(IntegrationsSettingsAction.OnSeerrEnabledChanged(it))
                },
                onBaseUrlChanged = {
                    onAction(IntegrationsSettingsAction.OnSeerrBaseUrlChanged(it))
                },
                onApiKeyChanged = {
                    onAction(IntegrationsSettingsAction.OnSeerrApiKeyChanged(it))
                },
                onTestConnectionClick = {
                    onAction(IntegrationsSettingsAction.OnTestSeerrConnection)
                },
            )

            HorizontalDivider()

            Column {
                Text(
                    text = stringResource(CoreR.string.integrations_poll_interval),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(CoreR.string.integrations_poll_interval_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.pvrPollIntervalMinutes.toString(),
                    onValueChange = { value ->
                        val minutes = value.toIntOrNull()
                        if (minutes != null) {
                            onAction(IntegrationsSettingsAction.OnPollIntervalChanged(minutes))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Column {
                Text(
                    text = stringResource(CoreR.string.integrations_release_cache),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Text(
                    text = stringResource(CoreR.string.integrations_release_cache_summary),
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = state.pvrReleaseCacheMinutes.toString(),
                    onValueChange = { value ->
                        val minutes = value.toIntOrNull()
                        if (minutes != null) {
                            onAction(IntegrationsSettingsAction.OnReleaseCacheChanged(minutes))
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun PvrServiceSection(
    nameRes: Int,
    @DrawableRes logoRes: Int,
    // Path under the service's base URL where its web UI shows the API key, e.g.
    // "/settings/general" for Sonarr/Radarr - linked from the section for easier setup.
    apiKeySettingsPath: String,
    enabled: Boolean,
    baseUrl: String,
    apiKey: String,
    testState: PvrTestState,
    onEnabledChanged: (Boolean) -> Unit,
    onBaseUrlChanged: (String) -> Unit,
    onApiKeyChanged: (String) -> Unit,
    onTestConnectionClick: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // Single header line: logo, name, and the enable toggle right-aligned.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(logoRes),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = stringResource(nameRes),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f),
            )
            Switch(checked = enabled, onCheckedChange = onEnabledChanged)
        }

        if (enabled) {
            OutlinedTextField(
                value = baseUrl,
                onValueChange = onBaseUrlChanged,
                label = { Text(text = stringResource(CoreR.string.integrations_server_url)) },
                placeholder = {
                    Text(text = stringResource(CoreR.string.integrations_server_url_hint))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = apiKey,
                onValueChange = onApiKeyChanged,
                label = { Text(text = stringResource(CoreR.string.integrations_api_key)) },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(
                        onClick = {
                            clipboardManager.getText()?.text?.let { pasted ->
                                onApiKeyChanged(pasted.trim())
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_clipboard_paste),
                            contentDescription =
                                stringResource(CoreR.string.integrations_paste_api_key),
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Button(
                    onClick = onTestConnectionClick,
                    enabled =
                        testState !is PvrTestState.Testing &&
                            baseUrl.isNotBlank() &&
                            apiKey.isNotBlank(),
                ) {
                    Text(text = stringResource(CoreR.string.integrations_test_connection))
                }
                TextButton(
                    onClick = {
                        val url = baseUrl.trim().trimEnd('/')
                        uriHandler.openUri(url + apiKeySettingsPath)
                    },
                    enabled = baseUrl.isNotBlank(),
                ) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_globe),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(CoreR.string.integrations_get_api_key))
                }
            }

            when (testState) {
                is PvrTestState.Success -> {
                    Text(
                        text =
                            stringResource(
                                CoreR.string.integrations_test_connection_success,
                                testState.itemCount,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is PvrTestState.Error -> {
                    Text(
                        text =
                            stringResource(
                                CoreR.string.integrations_test_connection_error,
                                testState.message,
                            ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                else -> Unit
            }
        }
    }
}
