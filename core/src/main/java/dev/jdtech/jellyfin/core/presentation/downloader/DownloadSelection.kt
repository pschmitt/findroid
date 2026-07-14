package dev.jdtech.jellyfin.core.presentation.downloader

import java.util.UUID

/**
 * What to download, chosen from [dev.jdtech.jellyfin.presentation.film.components.DownloadScopeDialog].
 * [thisEpisodeOnly] is exclusive with [entireShow]/[seasonIds] - it triggers the normal
 * single-item download flow instead of a bulk queue.
 */
data class DownloadSelection(
    val thisEpisodeOnly: Boolean = false,
    val entireShow: Boolean = false,
    val seasonIds: Set<UUID> = emptySet(),
    // Whole-show scope, but never backfills episodes that existed when the rule was created -
    // only seasons/episodes that show up afterwards. Implies persisting a rule; a one-off
    // "future seasons" download is a no-op by definition.
    val futureSeasonsOnly: Boolean = false,
)
