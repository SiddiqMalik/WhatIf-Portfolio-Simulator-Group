package com.reztek.whatifportfolio.data.sync

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import com.reztek.whatifportfolio.data.local.CachedSimulationDao
import com.reztek.whatifportfolio.data.local.CachedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.data.local.toCachedEntity
import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.NetworkModule
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "OfflineSimRepo"

/** Result of one pass over the pending queue. */
enum class SyncOutcome { DONE, RETRY }

/**
 * Offline-first access to the signed-in user's cloud simulations.
 *
 * - The Saved list reads from Room, so it works with no connection.
 * - Rename and delete are applied to Room immediately and marked pending.
 * - [syncPending] pushes the queue to the REST API (PUT / DELETE, both idempotent).
 *
 * Conflict policy: the user's explicit action wins (last write wins). A rename
 * for a simulation that no longer exists on the server is dropped (404).
 *
 * PART3-ASH: offline mode with sync.
 */
class OfflineSimulationRepository(
    private val dao: CachedSimulationDao,
    private val api: ApiService = NetworkModule.apiService,
    private val inTransaction: suspend (suspend () -> Unit) -> Unit = { block -> block() },
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    val visibleSimulations: Flow<List<CachedSimulationEntity>> = dao.observeVisible()
    val unsyncedCount: Flow<Int> = dao.observeUnsyncedCount()

    suspend fun hasCache(): Boolean = dao.count() > 0

    /**
     * Pulls the latest list from the API into Room. Rows with unsynced local
     * edits are never overwritten, and server-confirmed rows that disappeared
     * from the server are removed. Throws on network/HTTP errors.
     */
    suspend fun refreshFromServer() {
        val remote = api.listSimulations(null).data
        inTransaction {
            val protectedIds = dao.getUnsyncedIds().toSet()
            dao.upsertAll(remote.filter { it.id !in protectedIds }.map { it.toCachedEntity() })
            dao.deleteSyncedNotIn(remote.map { it.id })
        }
        Log.i(TAG, "Refreshed cache from server: ${remote.size} simulation(s)")
    }

    /** Renames locally and queues the change. Returns false if nothing changed. */
    suspend fun renameLocally(id: String, newName: String): Boolean {
        val name = newName.trim()
        if (name.isEmpty()) return false
        val row = dao.getById(id) ?: return false
        if (row.syncStatus == SyncStatus.PENDING_DELETE || row.syncStatus == SyncStatus.FAILED_DELETE) return false
        if (row.name == name && row.syncStatus == SyncStatus.SYNCED) return false
        dao.upsert(
            row.copy(
                name = name,
                syncStatus = SyncStatus.PENDING_UPDATE,
                retryCount = 0,
                lastError = null,
                localEditedAt = clock()
            )
        )
        Log.i(TAG, "Queued rename for $id")
        return true
    }

    /** Hides the simulation immediately and queues the delete. Returns false if it is unknown. */
    suspend fun deleteLocally(id: String): Boolean {
        val row = dao.getById(id) ?: return false
        dao.upsert(
            row.copy(
                syncStatus = SyncStatus.PENDING_DELETE,
                retryCount = 0,
                lastError = null,
                localEditedAt = clock()
            )
        )
        Log.i(TAG, "Queued delete for $id")
        return true
    }

    /** Puts rows the server permanently rejected back in the queue (manual "Sync now"). */
    suspend fun requeueFailed() {
        dao.requeueFailedUpdates()
        dao.requeueFailedDeletes()
    }

    /**
     * Pushes every pending change to the API, oldest first. Only one pass runs
     * at a time (worker and manual sync share the lock), which prevents the
     * same change being submitted twice.
     */
    suspend fun syncPending(): SyncOutcome = syncMutex.withLock {
        val pending = dao.getPending()
        Log.i(TAG, "Sync pass starting: ${pending.size} pending change(s)")
        for (row in pending) {
            try {
                when (row.syncStatus) {
                    SyncStatus.PENDING_DELETE -> pushDelete(row)
                    SyncStatus.PENDING_UPDATE -> pushUpdate(row)
                    else -> Unit
                }
            } catch (e: HttpException) {
                recordHttpFailure(row, e.code())
            } catch (e: IOException) {
                // No connection: the rest of the queue would fail the same way.
                Log.w(TAG, "Sync paused, network unavailable: ${e.message}")
                break
            }
        }
        if (dao.getPending().isEmpty()) SyncOutcome.DONE else SyncOutcome.RETRY
    }

    private suspend fun pushDelete(row: CachedSimulationEntity) {
        val response = api.deleteSimulation(row.id)
        if (response.isSuccessful || response.code() == 404) {
            dao.deleteById(row.id)
            Log.i(TAG, "Delete synced for ${row.id}")
        } else {
            recordHttpFailure(row, response.code())
        }
    }

    private suspend fun pushUpdate(row: CachedSimulationEntity) {
        val updated = api.updateSimulation(row.id, mapOf("name" to row.name))
        val current = dao.getById(row.id) ?: return
        if (current.syncStatus == SyncStatus.PENDING_UPDATE && current.localEditedAt == row.localEditedAt) {
            dao.upsert(updated.toCachedEntity())
            Log.i(TAG, "Rename synced for ${row.id}")
        } else {
            // Edited again while the request was in flight: leave the newer edit queued.
            Log.i(TAG, "Newer local edit for ${row.id}; keeping it queued")
        }
    }

    private suspend fun recordHttpFailure(row: CachedSimulationEntity, code: Int) {
        val current = dao.getById(row.id) ?: return
        if (current.localEditedAt != row.localEditedAt) return // edited again meanwhile; leave it

        when {
            code == 404 -> {
                // Already gone on the server: nothing left to push.
                dao.deleteById(row.id)
                Log.w(TAG, "Server no longer has ${row.id}; dropped local change")
            }
            code == 401 || code == 403 -> {
                dao.upsert(current.copy(lastError = "Sign in again to sync (HTTP $code)"))
            }
            code == 408 || code == 429 || code >= 500 -> {
                val attempts = current.retryCount + 1
                if (attempts >= MAX_ATTEMPTS) {
                    markFailed(current, "Server unavailable (HTTP $code)", attempts)
                } else {
                    dao.upsert(current.copy(retryCount = attempts, lastError = "Server busy (HTTP $code), will retry"))
                }
            }
            else -> markFailed(current, "Server rejected the change (HTTP $code)", current.retryCount)
        }
    }

    private suspend fun markFailed(row: CachedSimulationEntity, reason: String, attempts: Int) {
        val failed = if (row.syncStatus == SyncStatus.PENDING_DELETE) {
            SyncStatus.FAILED_DELETE
        } else {
            SyncStatus.FAILED_UPDATE
        }
        Log.e(TAG, "Giving up on ${row.id}: $reason")
        dao.upsert(row.copy(syncStatus = failed, retryCount = attempts, lastError = reason))
    }

    companion object {
        const val MAX_ATTEMPTS = 5
        private val syncMutex = Mutex()

        fun create(context: Context): OfflineSimulationRepository {
            val db = SimulationDatabase.getDatabase(context)
            return OfflineSimulationRepository(
                dao = db.cachedSimulationDao(),
                inTransaction = { block -> db.withTransaction { block() } }
            )
        }
    }
}
