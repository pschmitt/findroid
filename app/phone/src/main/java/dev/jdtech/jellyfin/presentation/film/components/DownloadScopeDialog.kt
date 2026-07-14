package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloadScope
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

private fun DownloadScope.labelRes(): Int =
    when (this) {
        DownloadScope.EPISODE -> CoreR.string.download_scope_episode
        DownloadScope.SEASON -> CoreR.string.download_scope_season
        DownloadScope.LATEST_SEASON -> CoreR.string.download_scope_latest_season
        DownloadScope.SHOW -> CoreR.string.download_scope_show
    }

@Composable
fun DownloadScopeDialog(
    scopes: List<DownloadScope>,
    onConfirm: (scope: DownloadScope, alsoFollowNew: Boolean, onlyUnwatched: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedScope by remember { mutableStateOf(scopes.first()) }
    var alsoFollowNew by remember { mutableStateOf(false) }
    var onlyUnwatched by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(text = stringResource(CoreR.string.download_scope_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                Column(modifier = Modifier.selectableGroup()) {
                    scopes.forEach { scope ->
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { selectedScope = scope }
                                    .padding(vertical = MaterialTheme.spacings.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedScope == scope,
                                onClick = { selectedScope = scope },
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                            Text(
                                text = stringResource(scope.labelRes()),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                }
                HorizontalDivider()
                Column {
                    if (selectedScope != DownloadScope.EPISODE) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .clickable { onlyUnwatched = !onlyUnwatched }
                                    .padding(vertical = MaterialTheme.spacings.small),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Checkbox(
                                checked = onlyUnwatched,
                                onCheckedChange = { onlyUnwatched = it },
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                            Text(
                                text = stringResource(CoreR.string.download_scope_only_unwatched),
                                style = MaterialTheme.typography.bodyLarge,
                            )
                        }
                    }
                    Row(
                        modifier =
                            Modifier.fillMaxWidth()
                                .clickable { alsoFollowNew = !alsoFollowNew }
                                .padding(vertical = MaterialTheme.spacings.small),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = alsoFollowNew,
                            onCheckedChange = { alsoFollowNew = it },
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                        Text(
                            text = stringResource(CoreR.string.download_scope_also_new),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = { onConfirm(selectedScope, alsoFollowNew, onlyUnwatched) }
            ) {
                Text(text = stringResource(CoreR.string.download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(CoreR.string.cancel)) }
        },
    )
}

@Composable
@Preview
private fun DownloadScopeDialogPreview() {
    FindroidTheme {
        DownloadScopeDialog(
            scopes = listOf(DownloadScope.EPISODE, DownloadScope.SEASON, DownloadScope.SHOW),
            onConfirm = { _, _, _ -> },
            onDismiss = {},
        )
    }
}
