package dev.jdtech.jellyfin.core.presentation.downloader

import java.util.UUID

/**
 * What to download, chosen from [dev.jdtech.jellyfin.presentation.film.components.DownloadScopeDialog].
 * [thisEpisodeOnly] triggers the normal single-item download flow instead of a bulk queue, and is
 * exclusive with [seasonIds]/[alsoFutureSeasons]. [seasonIds] and [alsoFutureSeasons] are
 * otherwise independent: picking specific seasons and also watching for future seasons of the
 * same show can both be on at once.
 */
data class DownloadSelection(
    val thisEpisodeOnly: Boolean = false,
    val seasonIds: Set<UUID> = emptySet(),
    // Watch for and auto-download brand new seasons of this show as they're released. Implies
    // persisting a rule - a one-off "future seasons" download is a no-op by definition, since
    // there's nothing to download yet.
    val alsoFutureSeasons: Boolean = false,
)
