package dev.jdtech.jellyfin.repository

import dev.jdtech.jellyfin.models.PvrQueueSnapshot
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

    /** Forces an immediate fetch+match cycle, independent of the in-process/background polling. */
    suspend fun refreshNow()
}
