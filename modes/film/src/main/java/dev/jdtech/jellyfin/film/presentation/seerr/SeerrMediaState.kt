package dev.jdtech.jellyfin.film.presentation.seerr

import dev.jdtech.jellyfin.models.SeerrMediaDetail
import dev.jdtech.jellyfin.models.QueueStatus
import dev.jdtech.jellyfin.core.presentation.search.ReleasePickerState
import java.util.UUID

data class SeerrMediaState(
    val detail: SeerrMediaDetail? = null,
    val isLoading: Boolean = false,
    // A request/cancel round-trip is in flight - disables the action button meanwhile.
    val isSubmitting: Boolean = false,
    val pvrSearchConfigured: Boolean = false,
    val manualPvrSearchAvailable: Boolean = false,
    val queueStatus: QueueStatus? = null,
    val jellyfinShowId: UUID? = null,
    val jellyfinSeasonId: UUID? = null,
    val releasePicker: ReleasePickerState? = null,
    val error: Exception? = null,
)
