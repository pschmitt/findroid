package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.models.FindroidEpisode
import dev.jdtech.jellyfin.models.FindroidItemPerson
import dev.jdtech.jellyfin.models.VideoMetadata
import dev.jdtech.jellyfin.repository.ExistingAutoDownloadScope

data class EpisodeState(
    val episode: FindroidEpisode? = null,
    val videoMetadata: VideoMetadata? = null,
    val actors: List<FindroidItemPerson> = emptyList(),
    val dateFormat: String = "system",
    val existingScope: ExistingAutoDownloadScope = ExistingAutoDownloadScope(),
    val error: Exception? = null,
)
