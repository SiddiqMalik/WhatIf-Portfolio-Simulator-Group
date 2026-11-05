package com.reztek.whatifportfolio.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto

/**
 * Local copy of a cloud simulation (the summary fields the Saved list needs),
 * plus the bookkeeping required to sync offline edits back to the REST API.
 *
 * This is deliberately separate from [SavedSimulationEntity], which holds the
 * on-device forward projections.
 *
 * PART3-ASH: offline mode with sync.
 */
@Entity(tableName = "cached_simulations")
data class CachedSimulationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val initialInvestment: Double,
    val recurringContribution: Double,
    val frequency: String,
    val status: String,
    val totalContributed: Double,
    val finalValue: Double,
    val profitLoss: Double,
    val percentReturn: Double,
    val realValue: Double?,
    val realProfitLoss: Double?,
    val realReturnPct: Double?,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus = SyncStatus.SYNCED,
    val retryCount: Int = 0,
    val lastError: String? = null,
    /** Epoch millis of the last local edit; used to detect edits made while a sync request was in flight. */
    val localEditedAt: Long = 0L
)

/** Server copy -> cache row. */
fun SimulationDto.toCachedEntity(syncStatus: SyncStatus = SyncStatus.SYNCED): CachedSimulationEntity =
    CachedSimulationEntity(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        initialInvestment = initialInvestment,
        recurringContribution = recurringContribution,
        frequency = frequency,
        status = status,
        totalContributed = totalContributed,
        finalValue = finalValue,
        profitLoss = profitLoss,
        percentReturn = percentReturn,
        realValue = realValue,
        realProfitLoss = realProfitLoss,
        realReturnPct = realReturnPct,
        createdAt = createdAt,
        updatedAt = updatedAt,
        syncStatus = syncStatus
    )

/** Cache row -> the DTO the existing Saved list UI already understands (no charts/time series). */
fun CachedSimulationEntity.toDto(): SimulationDto =
    SimulationDto(
        id = id,
        name = name,
        startDate = startDate,
        endDate = endDate,
        initialInvestment = initialInvestment,
        recurringContribution = recurringContribution,
        frequency = frequency,
        status = status,
        totalContributed = totalContributed,
        finalValue = finalValue,
        profitLoss = profitLoss,
        percentReturn = percentReturn,
        realValue = realValue,
        realProfitLoss = realProfitLoss,
        realReturnPct = realReturnPct,
        createdAt = createdAt,
        updatedAt = updatedAt
    )
