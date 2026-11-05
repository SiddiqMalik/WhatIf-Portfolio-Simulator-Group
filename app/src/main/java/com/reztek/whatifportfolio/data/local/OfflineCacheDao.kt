package com.reztek.whatifportfolio.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/** PART3-ASH: local-first data management (simulation details + asset search cache). */
@Dao
interface OfflineCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDetail(entity: CachedSimulationDetailEntity)

    @Query("SELECT * FROM cached_simulation_details WHERE id = :id")
    suspend fun getDetail(id: String): CachedSimulationDetailEntity?

    @Query("SELECT id, updatedAt FROM cached_simulation_details")
    suspend fun detailStamps(): List<DetailStamp>

    @Query("DELETE FROM cached_simulation_details WHERE id = :id")
    suspend fun deleteDetail(id: String)

    /** Drops details for simulations that no longer exist in the local list. */
    @Query("DELETE FROM cached_simulation_details WHERE id NOT IN (SELECT id FROM cached_simulations)")
    suspend fun deleteDetailsNotInList()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSearch(entity: CachedAssetSearchEntity)

    @Query("SELECT * FROM cached_asset_searches WHERE cacheKey = :key")
    suspend fun getSearch(key: String): CachedAssetSearchEntity?

    @Query("SELECT * FROM cached_asset_searches ORDER BY cachedAt DESC")
    suspend fun allSearches(): List<CachedAssetSearchEntity>

    /** Keeps only the 100 most recent searches. */
    @Query(
        "DELETE FROM cached_asset_searches WHERE cacheKey NOT IN " +
            "(SELECT cacheKey FROM cached_asset_searches ORDER BY cachedAt DESC LIMIT 100)"
    )
    suspend fun pruneSearches()
}
