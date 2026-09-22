package com.reztek.whatifportfolio.data.remote.dto

data class SimulationDto(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String,
    val initialInvestment: Double,
    val recurringContribution: Double,
    val frequency: String = "monthly",
    val status: String,
    val totalContributed: Double,
    val finalValue: Double,
    val profitLoss: Double,
    val percentReturn: Double,
    val realValue: Double? = null,
    val realProfitLoss: Double? = null,
    val realReturnPct: Double? = null,
    val allocations: List<AllocationDto>? = null,
    val perAsset: List<PerAssetResultDto>? = null,
    val timeSeries: List<TimeSeriesPointDto>? = null,
    val createdAt: String,
    val updatedAt: String
)

data class SimulationListResponse(val data: List<SimulationDto>)

data class AllocationDto(
    val symbol: String,
    val type: String,
    val percent: Double,
    val name: String? = null
)

data class PerAssetResultDto(
    val symbol: String,
    val type: String,
    val percent: Double,
    val units: Double,
    val contributed: Double
)

data class TimeSeriesPointDto(
    val date: String,
    val contributed: Double,
    val nominalValue: Double,
    val realValue: Double? = null
)

data class CreateSimulationRequest(
    val name: String,
    val startDate: String,
    val endDate: String? = null,
    val initialInvestment: Double,
    val recurringContribution: Double,
    val frequency: String = "monthly",
    val allocations: List<AllocationDto>
)

data class AssetSearchResultDto(
    val symbol: String,
    val name: String,
    val type: String,
    val currency: String? = null,
    val exchange: String? = null
)

data class AssetSearchResponse(
    val query: String,
    val type: String,
    val results: List<AssetSearchResultDto>
)
