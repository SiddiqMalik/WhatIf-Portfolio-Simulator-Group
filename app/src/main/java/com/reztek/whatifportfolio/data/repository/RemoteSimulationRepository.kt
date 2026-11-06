package com.reztek.whatifportfolio.data.repository

import android.util.Log
import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.NetworkModule
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.remote.dto.CreateSimulationRequest
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.sync.OfflineDataCache
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

private const val TAG = "RemoteSimRepo"

/** A simulation plus whether it came from the on-device copy (offline) or the server. */
data class SimulationLookup(val simulation: SimulationDto, val fromCache: Boolean)

/**
 * Wraps the Retrofit ApiService so ViewModels depend on a small, mockable
 * interface rather than Retrofit types directly.
 *
 * PART3-ASH: reads of a single simulation and of asset search are cached on the
 * device (when [cache] is available). If the network is down they are answered
 * from that copy, so saved simulations can be reopened and assets searched offline.
 */
class RemoteSimulationRepository(
    private val api: ApiService = NetworkModule.apiService,
    private val cache: OfflineDataCache? = OfflineDataCache.instance
) {
    suspend fun listSimulations(status: String? = null): List<SimulationDto> =
        api.listSimulations(status).data

    suspend fun getSimulation(id: String): SimulationDto = getSimulationWithSource(id).simulation

    /** Like [getSimulation], but also says whether the result is the offline copy. */
    suspend fun getSimulationWithSource(id: String): SimulationLookup {
        try {
            val fresh = api.getSimulation(id)
            quietly { cache?.saveDetail(fresh) }
            return SimulationLookup(fresh, fromCache = false)
        } catch (e: Exception) {
            if (!isConnectivityProblem(e)) throw e
            val saved = quietly { cache?.loadDetail(id) }
            if (saved != null) {
                Log.i(TAG, "Offline: showing the on-device copy of $id")
                return SimulationLookup(saved, fromCache = true)
            }
            throw e
        }
    }

    suspend fun createSimulation(request: CreateSimulationRequest): SimulationDto =
        api.createSimulation(request)

    suspend fun updateSimulation(id: String, updates: Map<String, String>): SimulationDto =
        api.updateSimulation(id, updates)

    suspend fun deleteSimulation(id: String) {
        val response = api.deleteSimulation(id)
        if (!response.isSuccessful) {
            error("Delete failed (${response.code()})")
        }
        quietly { cache?.removeDetail(id) }
    }

    suspend fun searchAssets(query: String, type: String = "all"): List<AssetSearchResultDto> {
        try {
            val results = api.searchAssets(query, type).results
            quietly { cache?.saveSearch(query, type, results) }
            return results
        } catch (e: Exception) {
            if (!isConnectivityProblem(e)) throw e
            val saved = quietly { cache?.loadSearch(query, type) }
            if (!saved.isNullOrEmpty()) {
                Log.i(TAG, "Offline: ${saved.size} asset(s) from earlier searches for \"$query\"")
                return saved
            }
            throw e
        }
    }

    /** No connection, timeout, or a server outage: the cases where a local copy is better than an error. */
    private fun isConnectivityProblem(e: Exception): Boolean =
        e is IOException || (e is HttpException && e.code() >= 500)

    /** Cache problems must never break a screen that otherwise works. */
    private suspend fun <T> quietly(block: suspend () -> T?): T? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Offline cache unavailable: ${e.message}")
            null
        }
}
