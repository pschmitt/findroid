package dev.jdtech.jellyfin.models

/** UI-facing models for the Jellyseerr/Seerr integration - see `JellyseerrRepository`. */

enum class SeerrMediaType {
    MOVIE,
    TV,
}

/** Jellyseerr's media availability lifecycle. */
enum class SeerrMediaStatus {
    /** Not tracked by Jellyseerr at all - requestable. */
    NOT_REQUESTED,
    /** Requested, waiting for approval. */
    PENDING,
    /** Approved and handed to Sonarr/Radarr, not yet downloaded. */
    PROCESSING,
    PARTIALLY_AVAILABLE,
    AVAILABLE;

    companion object {
        /** Jellyseerr's numeric codes: 1=unknown, 2=pending, 3=processing, 4=partial, 5=available. */
        fun fromCode(code: Int?): SeerrMediaStatus =
            when (code) {
                2 -> PENDING
                3 -> PROCESSING
                4 -> PARTIALLY_AVAILABLE
                5 -> AVAILABLE
                else -> NOT_REQUESTED
            }
    }
}

data class SeerrSearchItem(
    val tmdbId: Int,
    val mediaType: SeerrMediaType,
    val title: String,
    val year: Int?,
    val overview: String?,
    val posterUrl: String?,
    val status: SeerrMediaStatus,
)

data class SeerrRequestItem(
    val id: Int,
    val tmdbId: Int,
    val mediaType: SeerrMediaType,
    val title: String,
    val posterUrl: String?,
    val mediaStatus: SeerrMediaStatus,
)
