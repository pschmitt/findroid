package dev.jdtech.jellyfin.presentation.film.components

import android.app.DownloadManager
import android.os.Environment
import android.os.StatFs
import android.text.format.Formatter
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.jdtech.jellyfin.core.R as CoreR
import dev.jdtech.jellyfin.core.presentation.downloader.DownloadSelection
import dev.jdtech.jellyfin.core.presentation.downloader.DownloaderState
import dev.jdtech.jellyfin.core.presentation.dummy.dummyEpisode
import dev.jdtech.jellyfin.models.FindroidItem
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.models.FindroidSeason
import dev.jdtech.jellyfin.models.FindroidShow
import dev.jdtech.jellyfin.models.FindroidSourceType
import dev.jdtech.jellyfin.models.isDownloaded
import dev.jdtech.jellyfin.presentation.theme.FindroidTheme
import dev.jdtech.jellyfin.presentation.theme.spacings
import dev.jdtech.jellyfin.utils.displayNameWithContext
import dev.jdtech.jellyfin.utils.resolveDownloadStorageIndex

private data class OverflowAction(
    @DrawableRes val iconRes: Int,
    @StringRes val labelRes: Int,
    val onClick: () -> Unit,
)

@Composable
fun ItemButtonsBar(
    item: FindroidItem,
    onPlayClick: (startFromBeginning: Boolean) -> Unit,
    onMarkAsPlayedClick: () -> Unit,
    onMarkAsFavoriteClick: () -> Unit,
    onDownloadClick: (storageIndex: Int) -> Unit,
    onDownloadCancelClick: () -> Unit,
    onDownloadDeleteClick: () -> Unit,
    onDownloadForceClick: () -> Unit = {},
    onDownloadPauseClick: () -> Unit = {},
    onDownloadResumeClick: () -> Unit = {},
    onTrailerClick: (uri: String) -> Unit,
    modifier: Modifier = Modifier,
    downloaderState: DownloaderState? = null,
    downloadLocationPreference: String = "ask",
    enableDownloadDialog: Boolean = false,
    showEpisodeDownloadOption: Boolean = false,
    initialSelection: DownloadSelection = DownloadSelection(),
    initialAlsoFollowNew: Boolean = false,
    initialOnlyUnwatched: Boolean = false,
    getSeasons: (suspend () -> List<FindroidSeason>)? = null,
    hasActiveDownloadOrRule: Boolean = false,
    onDeleteDownloads: (() -> Unit)? = null,
    onBulkDownload:
        (selection: DownloadSelection, alsoFollowNew: Boolean, onlyUnwatched: Boolean) -> Unit =
        { _, _, _ ->
        },
    downloadIconTint: Color? = null,
    onInfoClick: (() -> Unit)? = null,
    trailingContent: @Composable FlowRowScope.() -> Unit = {},
) {
    val context = LocalContext.current

    val trailerUri =
        when (item) {
            is FindroidMovie -> {
                item.trailer
            }
            is FindroidShow -> {
                item.trailer
            }
            else -> null
        }

    val downloadedSource =
        if (item.isDownloaded()) {
            item.sources.firstOrNull { it.type == FindroidSourceType.LOCAL }
        } else {
            null
        }

    var storageSelectionDialogOpen by remember { mutableStateOf(false) }
    var cancelDownloadDialogOpen by remember { mutableStateOf(false) }
    var deleteDownloadDialogOpen by remember { mutableStateOf(false) }
    var downloadScopeDialogOpen by remember { mutableStateOf(false) }

    var selectedStorageIndex by remember { mutableIntStateOf(0) }
    var storageLocations = remember { context.getExternalFilesDirs(null) }

    val startDownload: () -> Unit = {
        storageLocations = context.getExternalFilesDirs(null)
        val preferredIndex = resolveDownloadStorageIndex(context, downloadLocationPreference)
        when {
            preferredIndex >= 0 -> {
                selectedStorageIndex = preferredIndex
                onDownloadClick(selectedStorageIndex)
            }
            storageLocations.size > 1 -> {
                storageSelectionDialogOpen = true
            }
            else -> {
                selectedStorageIndex = 0
                onDownloadClick(selectedStorageIndex)
            }
        }
    }

    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides 0.dp) {
        Column(
            modifier = modifier,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
        ) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
                verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacings.small),
            ) {
                FilledTonalButton(onClick = onMarkAsPlayedClick) {
                    Icon(
                        painter = painterResource(CoreR.drawable.ic_check),
                        contentDescription = null,
                        tint = if (item.played) Color.Red else LocalContentColor.current,
                    )
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    Text(
                        text =
                            stringResource(
                                if (item.played) CoreR.string.unmark_as_played
                                else CoreR.string.mark_as_played
                            )
                    )
                }
                FilledTonalButton(onClick = onMarkAsFavoriteClick) {
                    when (item.favorite) {
                        true -> {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart_filled),
                                contentDescription = null,
                                tint = Color.Red,
                            )
                        }
                        false -> {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_heart),
                                contentDescription = null,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                    Text(
                        text =
                            stringResource(
                                if (item.favorite) CoreR.string.remove_from_favorites
                                else CoreR.string.add_to_favorites
                            )
                    )
                }
                val canRestart = item.playbackPositionTicks.div(600000000) > 0
                val overflowActions =
                    buildList {
                        if (canRestart) {
                            add(
                                OverflowAction(
                                    iconRes = CoreR.drawable.ic_rotate_ccw,
                                    labelRes = CoreR.string.restart_from_beginning,
                                    onClick = { onPlayClick(true) },
                                )
                            )
                        }
                        trailerUri?.let { uri ->
                            add(
                                OverflowAction(
                                    iconRes = CoreR.drawable.ic_film,
                                    labelRes = CoreR.string.watch_trailer,
                                    onClick = { onTrailerClick(uri) },
                                )
                            )
                        }
                        onInfoClick?.let { infoClick ->
                            add(
                                OverflowAction(
                                    iconRes = CoreR.drawable.ic_info,
                                    labelRes = CoreR.string.info,
                                    onClick = infoClick,
                                )
                            )
                        }
                    }
                if (overflowActions.size == 1) {
                    val action = overflowActions.first()
                    FilledTonalButton(onClick = action.onClick) {
                        Icon(painter = painterResource(action.iconRes), contentDescription = null)
                        Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                        Text(text = stringResource(action.labelRes))
                    }
                } else if (overflowActions.size > 1) {
                    var overflowMenuExpanded by remember { mutableStateOf(false) }
                    FilledTonalIconButton(onClick = { overflowMenuExpanded = true }) {
                        Icon(
                            painter = painterResource(CoreR.drawable.ic_more_vertical),
                            contentDescription = stringResource(CoreR.string.more_options),
                        )
                    }
                    DropdownMenu(
                        expanded = overflowMenuExpanded,
                        onDismissRequest = { overflowMenuExpanded = false },
                    ) {
                        overflowActions.forEach { action ->
                            DropdownMenuItem(
                                text = { Text(text = stringResource(action.labelRes)) },
                                leadingIcon = {
                                    Icon(
                                        painter = painterResource(action.iconRes),
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    overflowMenuExpanded = false
                                    action.onClick()
                                },
                            )
                        }
                    }
                }
                trailingContent()
                if (downloaderState != null && !downloaderState.isDownloading) {
                    if (item.isDownloaded()) {
                        FilledTonalButton(onClick = { deleteDownloadDialogOpen = true }) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_trash),
                                contentDescription = null,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                            Text(
                                text =
                                    downloadedSource?.size?.let { size ->
                                        stringResource(
                                            CoreR.string.delete_download_with_size,
                                            Formatter.formatFileSize(context, size),
                                        )
                                    } ?: stringResource(CoreR.string.delete_download)
                            )
                        }
                    } else if (item.canDownload || item is FindroidShow || item is FindroidSeason) {
                        FilledTonalButton(
                            onClick = {
                                if (enableDownloadDialog) {
                                    downloadScopeDialogOpen = true
                                } else {
                                    startDownload()
                                }
                            }
                        ) {
                            Icon(
                                painter = painterResource(CoreR.drawable.ic_download),
                                contentDescription = null,
                                tint = downloadIconTint ?: LocalContentColor.current,
                            )
                            Spacer(modifier = Modifier.width(MaterialTheme.spacings.small))
                            Text(text = stringResource(CoreR.string.download))
                        }
                    }
                }
            }
            if (downloaderState != null) {
                AnimatedVisibility(downloaderState.isDownloading) {
                    Column {
                        DownloaderCard(
                            state = downloaderState,
                            onCancelClick = { cancelDownloadDialogOpen = true },
                            onRetryClick = { onDownloadClick(selectedStorageIndex) },
                            onForceClick = onDownloadForceClick,
                            onPauseClick = onDownloadPauseClick,
                            onResumeClick = onDownloadResumeClick,
                        )
                        Spacer(Modifier.height(MaterialTheme.spacings.small))
                    }
                }
            }
        }
        if (storageSelectionDialogOpen) {
            val locations = remember {
                storageLocations.map { dir ->
                    val locationStringRes =
                        if (Environment.isExternalStorageRemovable(dir)) CoreR.string.external
                        else CoreR.string.internal
                    val locationString = context.getString(locationStringRes)

                    val stat = StatFs(dir.path)
                    val availableMegaBytes = stat.availableBytes.div(1000000)
                    context.getString(CoreR.string.storage_name, locationString, availableMegaBytes)
                }
            }
            StorageSelectionDialog(
                storageLocations = locations,
                onSelect = { storageIndex ->
                    selectedStorageIndex = storageIndex
                    onDownloadClick(selectedStorageIndex)
                    storageSelectionDialogOpen = false
                },
                onDismiss = { storageSelectionDialogOpen = false },
            )
        }
        if (cancelDownloadDialogOpen) {
            CancelDownloadDialog(
                onCancel = {
                    onDownloadCancelClick()
                    cancelDownloadDialogOpen = false
                },
                onDismiss = { cancelDownloadDialogOpen = false },
            )
        }
        if (deleteDownloadDialogOpen) {
            DeleteDownloadDialog(
                onDelete = {
                    onDownloadDeleteClick()
                    deleteDownloadDialogOpen = false
                },
                onDismiss = { deleteDownloadDialogOpen = false },
                name = item.displayNameWithContext(),
                path = downloadedSource?.path,
                sizeBytes = downloadedSource?.size,
            )
        }
        if (downloadScopeDialogOpen) {
            var seasons by remember { mutableStateOf<List<FindroidSeason>?>(null) }
            LaunchedEffect(Unit) { seasons = getSeasons?.invoke() ?: emptyList() }
            DownloadScopeDialog(
                seasons = seasons,
                showEpisodeOption = showEpisodeDownloadOption,
                initialSelection = initialSelection,
                initialAlsoFollowNew = initialAlsoFollowNew,
                initialOnlyUnwatched = initialOnlyUnwatched,
                canDelete = hasActiveDownloadOrRule,
                onDelete =
                    onDeleteDownloads?.let {
                        {
                            downloadScopeDialogOpen = false
                            it()
                        }
                    },
                onConfirm = { selection, alsoFollowNew, onlyUnwatched ->
                    downloadScopeDialogOpen = false
                    if (selection.thisEpisodeOnly) {
                        startDownload()
                    } else {
                        onBulkDownload(selection, alsoFollowNew, onlyUnwatched)
                    }
                },
                onDismiss = { downloadScopeDialogOpen = false },
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
            onDownloadClick = {},
            onDownloadCancelClick = {},
            onDownloadDeleteClick = {},
            onTrailerClick = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun ItemButtonsBarDownloadingPreview() {
    FindroidTheme {
        ItemButtonsBar(
            item = dummyEpisode,
            downloaderState =
                DownloaderState(status = DownloadManager.STATUS_RUNNING, progress = 0.3f),
            onPlayClick = {},
            onMarkAsPlayedClick = {},
            onMarkAsFavoriteClick = {},
            onDownloadClick = {},
            onDownloadCancelClick = {},
            onDownloadDeleteClick = {},
            onTrailerClick = {},
        )
    }
}
