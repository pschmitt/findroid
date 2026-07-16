package dev.jdtech.jellyfin.models

import java.time.LocalDate
import java.util.UUID

/**
 * A single upcoming Sonarr/Radarr release, already date-resolved and (best-effort) matched
 * against a real Jellyfin item id by [dev.jdtech.jellyfin.repository.CalendarRepository] - see
 * `matchSonarrCalendar`/`matchRadarrCalendar` in `CalendarMatching.kt`.
 *
 * Unlike [QueueStatus], an entry Findroid couldn't match to a local Jellyfin item is still
 * included here (with [itemId] `null`) rather than dropped - the point of the calendar is to show
 * "something is coming" even before the show/movie has been added to the Jellyfin library.
 */
data class CalendarEntry(
    val date: LocalDate,
    val source: PvrSource,
    val title: String,
    val subtitle: String?,
    val itemId: UUID?,
    val hasFile: Boolean,
    val monitored: Boolean,
    // Populated by CalendarRepositoryImpl in a follow-up per-item fetch once itemId is resolved -
    // null for unmatched entries, or if that fetch fails (poster art is non-critical, not worth
    // failing the whole entry over).
    val images: FindroidImages? = null,
)
