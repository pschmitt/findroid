package dev.jdtech.jellyfin.presentation.film.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.presentation.theme.spacings

/**
 * Everything that used to live behind the "Display Extra Info" setting (size/codec/subtitle
 * details) plus the downloaded file path, now surfaced on demand via an explicit Info button
 * instead of always-on chips or a persistent toggle.
 */
@Composable
fun InfoDialog(videoMetadata: VideoMetadata, downloadedFilePath: String?, onDismiss: () -> Unit) {
    AlertDialog(
        title = { Text(text = stringResource(CoreR.string.info)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small)) {
                ExtraInfoText(videoMetadata = videoMetadata)
                if (downloadedFilePath != null) {
                    Text(
                        text = "${stringResource(CoreR.string.download_file_path)}: $downloadedFilePath",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(CoreR.string.close)) }
        },
    )
}
