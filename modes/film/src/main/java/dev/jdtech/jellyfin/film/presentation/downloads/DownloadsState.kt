package dev.jdtech.jellyfin.film.presentation.downloads

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidMovie
import dev.jdtech.jellyfin.utils.DeleteProgress
import dev.jdtech.jellyfin.utils.DownloadProgress
import java.util.UUID

data class DownloadsState(
    val isLoading: Boolean = false,
    val error: Exception? = null,
    val movies: List<FindroidMovie> = emptyList(),
    val showGroups: List<DownloadShowGroup> = emptyList(),
    val selectedIds: Set<UUID> = emptySet(),
    val downloadProgress: Map<UUID, DownloadProgress> = emptyMap(),
    val deleteProgress: DeleteProgress? = null,
) {
    val isEmpty: Boolean
        get() = movies.isEmpty() && showGroups.isEmpty()
}

data class DownloadShowGroup(
    val seriesId: UUID,
    val seriesName: String,
    val episodes: List<FindroidEpisode>,
)
