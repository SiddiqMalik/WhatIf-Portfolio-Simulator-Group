package com.reztek.whatifportfolio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/** PART3-ASH: offline mode with sync. */
@Dao
interface CachedSimulationDao {

    /** Everything the user should see; rows queued for deletion are hidden immediately. */
    @Query("SELECT * FROM cached_simulations WHERE syncStatus != 'PENDING_DELETE' ORDER BY createdAt DESC")
    fun observeVisible(): Flow<List<CachedSimulationEntity>>

    /** Rows that differ from the server (pending or failed). */
    @Query("SELECT COUNT(*) FROM cached_simulations WHERE syncStatus != 'SYNCED'")
    fun observeUnsyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM cached_simulations")
    suspend fun count(): Int

    @Query("SELECT * FROM cached_simulations WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): CachedSimulationEntity?

    /** Oldest edit first, so changes reach the server in the order the user made them. */
    @Query(
        "SELECT * FROM cached_simulations " +
            "WHERE syncStatus IN ('PENDING_UPDATE', 'PENDING_DELETE') ORDER BY localEditedAt ASC"
    )
    suspend fun getPending(): List<CachedSimulationEntity>

    @Query("SELECT id FROM cached_simulations WHERE syncStatus != 'SYNCED'")
    suspend fun getUnsyncedIds(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CachedSimulationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(entities: List<CachedSimulationEntity>)

    @Query("DELETE FROM cached_simulations WHERE id = :id")
    suspend fun deleteById(id: String)

    /** Drops server-confirmed rows that no longer exist on the server (deleted elsewhere). */
    @Query("DELETE FROM cached_simulations WHERE syncStatus = 'SYNCED' AND id NOT IN (:keepIds)")
    suspend fun deleteSyncedNotIn(keepIds: List<String>)

    @Query(
        "UPDATE cached_simulations SET syncStatus = 'PENDING_UPDATE', retryCount = 0, lastError = NULL " +
            "WHERE syncStatus = 'FAILED_UPDATE'"
    )
    suspend fun requeueFailedUpdates()

    @Query(
        "UPDATE cached_simulations SET syncStatus = 'PENDING_DELETE', retryCount = 0, lastError = NULL " +
            "WHERE syncStatus = 'FAILED_DELETE'"
    )
    suspend fun requeueFailedDeletes()
}
