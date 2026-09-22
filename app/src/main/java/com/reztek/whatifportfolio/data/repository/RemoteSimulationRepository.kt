package com.reztek.whatifportfolio.data.repository

import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.NetworkModule
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.remote.dto.CreateSimulationRequest
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto

/**
 * Wraps the Retrofit ApiService so ViewModels depend on a small, mockable
 * interface rather than Retrofit types directly.
 */
class RemoteSimulationRepository(
    private val api: ApiService = NetworkModule.apiService
) {
    suspend fun listSimulations(status: String? = null): List<SimulationDto> =
        api.listSimulations(status).data

    suspend fun getSimulation(id: String): SimulationDto = api.getSimulation(id)

    suspend fun createSimulation(request: CreateSimulationRequest): SimulationDto =
        api.createSimulation(request)

    suspend fun updateSimulation(id: String, updates: Map<String, String>): SimulationDto =
        api.updateSimulation(id, updates)

    suspend fun deleteSimulation(id: String) {
        val response = api.deleteSimulation(id)
        if (!response.isSuccessful) {
            error("Delete failed (${response.code()})")
        }
    }

    suspend fun searchAssets(query: String, type: String = "all"): List<AssetSearchResultDto> =
        api.searchAssets(query, type).results
}
