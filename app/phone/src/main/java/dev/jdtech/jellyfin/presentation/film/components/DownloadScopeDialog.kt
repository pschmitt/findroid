package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        DownloadScope.SHOW -> CoreR.string.download_scope_show
    }

@Composable
fun DownloadScopeDialog(
    scopes: List<DownloadScope>,
    onConfirm: (scope: DownloadScope, alsoFollowNew: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var selectedScope by remember { mutableStateOf(scopes.first()) }
    var alsoFollowNew by remember { mutableStateOf(false) }

    AlertDialog(
        title = { Text(text = stringResource(CoreR.string.download_scope_title)) },
        text = {
            Column {
                scopes.forEach { scope ->
                    Row(
                        modifier = Modifier.clickable { selectedScope = scope },
                    ) {
                        RadioButton(
                            selected = selectedScope == scope,
                            onClick = { selectedScope = scope },
                        )
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                        Text(
                            text = stringResource(scope.labelRes()),
                            modifier = Modifier,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacings.medium))
                Row(modifier = Modifier.clickable { alsoFollowNew = !alsoFollowNew }) {
                    Checkbox(checked = alsoFollowNew, onCheckedChange = { alsoFollowNew = it })
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    Text(text = stringResource(CoreR.string.download_scope_also_new))
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedScope, alsoFollowNew) }) {
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
            onConfirm = { _, _ -> },
            onDismiss = {},
        )
    }
}
