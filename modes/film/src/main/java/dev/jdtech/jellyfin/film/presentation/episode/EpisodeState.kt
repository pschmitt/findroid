package dev.jdtech.jellyfin.film.presentation.episode

import dev.jdtech.jellyfin.core.presentation.search.ReleasePickerState
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
    val seriesTvdbId: String? = null,
    val releasePicker: ReleasePickerState? = null,
    val error: Exception? = null,
)
