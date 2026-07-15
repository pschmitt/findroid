package dev.jdtech.jellyfin.presentation.film.components

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings

@Composable
fun DeleteDownloadDialog(
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
    path: String? = null,
    sizeBytes: Long? = null,
) {
    val context = LocalContext.current

    AlertDialog(
        title = { Text(text = stringResource(CoreR.string.delete_download)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                Text(text = stringResource(CoreR.string.delete_download_message))
                if (path != null) {
                    Text(
                        text = path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (sizeBytes != null) {
                    Text(
                        text = Formatter.formatFileSize(context, sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDelete) {
                Text(text = stringResource(CoreR.string.delete_download))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(CoreR.string.cancel)) }
        },
    )
}

@Composable
@Preview
private fun CancelDownloadDialogPreview() {
    FindroidTheme { DeleteDownloadDialog(onDelete = {}, onDismiss = {}) }
}
