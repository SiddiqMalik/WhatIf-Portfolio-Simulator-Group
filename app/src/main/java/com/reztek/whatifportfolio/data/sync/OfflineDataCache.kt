package com.reztek.whatifportfolio.data.sync

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.reztek.whatifportfolio.data.local.CachedAssetSearchEntity
import com.reztek.whatifportfolio.data.local.CachedSimulationDao
import com.reztek.whatifportfolio.data.local.CachedSimulationDetailEntity
import com.reztek.whatifportfolio.data.local.OfflineCacheDao
import com.reztek.whatifportfolio.data.local.SimulationDatabase
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto

/**
 * Local copy of the data the app reads from the REST API, so saved simulations
 * can be reopened and assets searched without a connection.
 *
 * - Simulation details: the full result (allocations, per-asset breakdown, time
 *   series) of each saved simulation. Pending local edits are layered on top.
 * - Asset search: every successful search is kept; offline, the exact query is
 *   answered from cache, otherwise earlier results are filtered by the typed text.
 *
 * PART3-ASH: local-first data management.
 */
class OfflineDataCache(
    private val cacheDao: OfflineCacheDao,
    private val simulationDao: CachedSimulationDao,
    private val gson: Gson = Gson(),
    private val clock: () -> Long = { System.currentTimeMillis() }
) {

    // ---- simulation details ------------------------------------------------

    suspend fun saveDetail(simulation: SimulationDto) {
        cacheDao.upsertDetail(
            CachedSimulationDetailEntity(
                id = simulation.id,
                json = gson.toJson(simulation),
                updatedAt = simulation.updatedAt,
                cachedAt = clock()
            )
        )
    }

    /**
     * Returns the cached detail with the user's pending rename applied, or null
     * when nothing is cached or the user has deleted it locally.
     */
    suspend fun loadDetail(id: String): SimulationDto? {
        val row = cacheDao.getDetail(id) ?: return null
        val cached = gson.fromJson(row.json, SimulationDto::class.java) ?: return null
        val local = simulationDao.getById(id) ?: return cached
        return when (local.syncStatus) {
            SyncStatus.PENDING_DELETE -> null
            SyncStatus.PENDING_UPDATE, SyncStatus.FAILED_UPDATE -> cached.copy(name = local.name)
            else -> cached
        }
    }

    suspend fun detailStamps(): Map<String, String> =
        cacheDao.detailStamps().associate { it.id to it.updatedAt }

    suspend fun removeDetail(id: String) = cacheDao.deleteDetail(id)

    suspend fun pruneOrphans() = cacheDao.deleteDetailsNotInList()

    // ---- asset search ------------------------------------------------------

    suspend fun saveSearch(query: String, type: String, results: List<AssetSearchResultDto>) {
        if (results.isEmpty()) return
        cacheDao.upsertSearch(
            CachedAssetSearchEntity(
                cacheKey = searchKey(query, type),
                json = gson.toJson(results),
                cachedAt = clock()
            )
        )
        cacheDao.pruneSearches()
    }

    /** Exact match first; otherwise earlier results whose symbol or name contain [query]. */
    suspend fun loadSearch(query: String, type: String): List<AssetSearchResultDto> {
        val exact = cacheDao.getSearch(searchKey(query, type))?.let { parseResults(it.json) }
        if (!exact.isNullOrEmpty()) return exact

        val needle = query.trim()
        if (needle.isEmpty()) return emptyList()
        return cacheDao.allSearches()
            .flatMap { parseResults(it.json) }
            .filter { type.equals("all", ignoreCase = true) || it.type.equals(type, ignoreCase = true) }
            .filter { it.symbol.contains(needle, ignoreCase = true) || it.name.contains(needle, ignoreCase = true) }
            .distinctBy { it.symbol.lowercase() + "|" + it.type.lowercase() }
            .take(MAX_FALLBACK_RESULTS)
    }

    private fun parseResults(json: String): List<AssetSearchResultDto> {
        val listType = object : TypeToken<List<AssetSearchResultDto>>() {}.type
        return gson.fromJson<List<AssetSearchResultDto>>(json, listType).orEmpty()
    }

    private fun searchKey(query: String, type: String) =
        type.trim().lowercase() + "|" + query.trim().lowercase()

    companion object {
        private const val MAX_FALLBACK_RESULTS = 25

        /** Set once from [com.reztek.whatifportfolio.WhatIfApplication]; null in unit tests. */
        @Volatile
        var instance: OfflineDataCache? = null

        fun init(context: Context) {
            val db = SimulationDatabase.getDatabase(context)
            instance = OfflineDataCache(db.offlineCacheDao(), db.cachedSimulationDao())
        }
    }
}
