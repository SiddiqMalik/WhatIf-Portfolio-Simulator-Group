package com.reztek.whatifportfolio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/*
 * PART3-ASH: local-first data management.
 * Full simulation results and asset-search results are stored as JSON so the
 * app can reopen saved simulations and search assets with no connection.
 */

/** Full `GET /v1/simulations/:id` response (allocations, per-asset breakdown, time series). */
@Entity(tableName = "cached_simulation_details")
data class CachedSimulationDetailEntity(
    @PrimaryKey val id: String,
    val json: String,
    val updatedAt: String,
    val cachedAt: Long
)

/** One `GET /v1/assets/search` response, keyed by "<type>|<lower-case query>". */
@Entity(tableName = "cached_asset_searches")
data class CachedAssetSearchEntity(
    @PrimaryKey val cacheKey: String,
    val json: String,
    val cachedAt: Long
)

/** Lightweight (id, updatedAt) pair used to decide which details are stale. */
data class DetailStamp(
    val id: String,
    val updatedAt: String
)
