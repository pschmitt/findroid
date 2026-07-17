package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.PvrQueueSnapshot
import dev.jdtech.jellyfin.models.PvrSource
import dev.jdtech.jellyfin.models.QueueStatus
import java.util.UUID
import kotlinx.coroutines.flow.Flow

/**
 * Exposes the current Sonarr/Radarr download queue. [getQueueSnapshotFlow] carries the full queue
 * - including entries that couldn't be matched to a Jellyfin library item (e.g. a torrent added
 * manually on the PVR side) and per-service fetch errors - see `matchSonarr`/`matchRadarr` in
 * `QueueStatusMatching.kt`. The map-shaped flows only cover matched entries, keyed by the
 * Jellyfin item id; items that aren't in either queue right now are simply absent.
 */
interface QueueStatusRepository {
    /** Every current queue entry (matched or not, Sonarr-then-Radarr order) plus fetch errors. */
    fun getQueueSnapshotFlow(): Flow<PvrQueueSnapshot>

    fun getQueueStatusFlow(): Flow<Map<UUID, QueueStatus>>

    fun getQueueStatusFlow(itemId: UUID): Flow<QueueStatus?>

    /** Radarr queue status keyed by TMDB id, including movies not imported into Jellyfin yet. */
    fun getRadarrQueueStatusFlow(): Flow<Map<Int, QueueStatus>>

    /** Sonarr queue status keyed by Sonarr episode id, including episodes not in Jellyfin yet. */
    fun getSonarrQueueStatusFlow(): Flow<Map<Int, QueueStatus>>

    /** Forces an immediate fetch+match cycle, independent of the in-process/background polling. */
    suspend fun refreshNow()

    /**
     * Removes a queue entry from Sonarr/Radarr (there is no API-side "pause" - that lives in the
     * download client). [removeFromClient] also deletes the download in the download client;
     * [blocklist] prevents the same release from being grabbed again. Refreshes the snapshot on
     * success, so the flows above update immediately.
     */
    suspend fun removeQueueItem(
        source: PvrSource,
        queueItemId: Int,
        removeFromClient: Boolean,
        blocklist: Boolean,
    ): Result<Unit>
}
