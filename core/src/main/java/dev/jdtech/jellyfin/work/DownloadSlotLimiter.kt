package dev.jdtech.jellyfin.work

import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Caps how many [VideoDownloadWorker]s transfer bytes at once. WorkManager has no built-in notion
 * of "run at most N of this worker type", so we gate the actual transfer here - extra workers
 * stay queued (visible to the user via a "Queued" notification) and wait their turn instead of
 * running unbounded or being rejected.
 */
internal object DownloadSlotLimiter {
    private val mutex = Mutex()
    private var active = 0

    suspend fun acquire(maxParallel: Int) {
        val limit = maxParallel.coerceAtLeast(1)
        while (true) {
            mutex.withLock {
                if (active < limit) {
                    active++
                    return
                }
            }
            delay(POLL_INTERVAL_MS)
        }
    }

    suspend fun release() {
        mutex.withLock { active = (active - 1).coerceAtLeast(0) }
    }

    private const val POLL_INTERVAL_MS = 1000L
}
