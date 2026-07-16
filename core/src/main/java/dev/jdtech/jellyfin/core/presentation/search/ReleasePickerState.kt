package dev.jdtech.jellyfin.core.presentation.search

import dev.jdtech.jellyfin.api.pvr.SonarrRelease

/** Null in the owning screen's state when the release picker sheet isn't open. */
data class ReleasePickerState(val isLoading: Boolean = true, val releases: List<SonarrRelease> = emptyList())
