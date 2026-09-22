package com.reztek.whatifportfolio.data.remote

import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResponse
import com.reztek.whatifportfolio.data.remote.dto.CreateSimulationRequest
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationListResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("v1/assets/search")
    suspend fun searchAssets(
        @Query("q") query: String,
        @Query("type") type: String = "all"
    ): AssetSearchResponse

    @GET("v1/simulations")
    suspend fun listSimulations(
        @Query("status") status: String? = null
    ): SimulationListResponse

    @GET("v1/simulations/{id}")
    suspend fun getSimulation(@Path("id") id: String): SimulationDto

    @POST("v1/simulations")
    suspend fun createSimulation(@Body request: CreateSimulationRequest): SimulationDto

    @PUT("v1/simulations/{id}")
    suspend fun updateSimulation(
        @Path("id") id: String,
        @Body updates: Map<String, String>
    ): SimulationDto

    @DELETE("v1/simulations/{id}")
    suspend fun deleteSimulation(@Path("id") id: String): retrofit2.Response<Unit>
}
