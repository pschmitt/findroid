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
)
