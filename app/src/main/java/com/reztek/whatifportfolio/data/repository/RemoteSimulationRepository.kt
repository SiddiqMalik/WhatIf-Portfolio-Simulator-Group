package com.reztek.whatifportfolio.data.repository

import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.NetworkModule
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto

/**
 * Wraps the Retrofit ApiService so ViewModels depend on a small, mockable
 * interface rather than Retrofit types directly.
 */
class RemoteSimulationRepository(
    private val api: ApiService = NetworkModule.apiService
) {
    suspend fun listSimulations(): List<SimulationDto> = api.listSimulations().data
}
