package com.reztek.whatifportfolio.data.sync

import com.reztek.whatifportfolio.data.local.CachedSimulationDao
import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResponse
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** PART3-ASH: reads fall back to the on-device copy when there is no connection. */
class RemoteSimulationRepositoryOfflineTest {

    private val api = mockk<ApiService>()
    private val simulationDao = mockk<CachedSimulationDao>().also {
        coEvery { it.getById(any()) } returns null
    }
    private val cacheDao = FakeOfflineCacheDao()
    private val cache = OfflineDataCache(cacheDao, simulationDao, clock = { 1_000L })
    private val repository = RemoteSimulationRepository(api, cache)

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun onlineReadIsFreshAndGetsStoredForLater() = runTest {
        coEvery { api.getSimulation("sim-1") } returns sampleSimulation()

        val lookup = repository.getSimulationWithSource("sim-1")

        assertFalse(lookup.fromCache)
        assertTrue(cacheDao.details.containsKey("sim-1"))
    }

    @Test
    fun offlineReadReturnsTheStoredCopy() = runTest {
        coEvery { api.getSimulation("sim-1") } returns sampleSimulation()
        repository.getSimulation("sim-1") // fills the cache while online

        coEvery { api.getSimulation("sim-1") } throws IOException("no network")
        val lookup = repository.getSimulationWithSource("sim-1")

        assertTrue(lookup.fromCache)
        assertEquals("My run", lookup.simulation.name)
        assertEquals(2, lookup.simulation.timeSeries!!.size)
    }

    @Test
    fun offlineWithNothingStoredStillFails() = runTest {
        coEvery { api.getSimulation("sim-9") } throws IOException("no network")

        try {
            repository.getSimulation("sim-9")
            fail("expected the network error")
        } catch (e: IOException) {
            assertEquals("no network", e.message)
        }
    }

    @Test
    fun serverOutageFallsBackToTheStoredCopy() = runTest {
        coEvery { api.getSimulation("sim-1") } returns sampleSimulation()
        repository.getSimulation("sim-1")

        coEvery { api.getSimulation("sim-1") } throws httpError(503)

        assertTrue(repository.getSimulationWithSource("sim-1").fromCache)
    }

    @Test
    fun notFoundIsNotHiddenByTheStoredCopy() = runTest {
        coEvery { api.getSimulation("sim-1") } returns sampleSimulation()
        repository.getSimulation("sim-1")

        coEvery { api.getSimulation("sim-1") } throws httpError(404)

        try {
            repository.getSimulation("sim-1")
            fail("expected HttpException")
        } catch (e: HttpException) {
            assertEquals(404, e.code())
        }
    }

    @Test
    fun onlineSearchIsStoredAndAnsweredOfflineLater() = runTest {
        val apple = AssetSearchResultDto("AAPL", "Apple Inc", "equity")
        coEvery { api.searchAssets("apple", "all") } returns AssetSearchResponse("apple", "all", listOf(apple))
        assertEquals(listOf(apple), repository.searchAssets("apple", "all"))

        coEvery { api.searchAssets("apple", "all") } throws IOException("no network")

        assertEquals(listOf(apple), repository.searchAssets("apple", "all"))
    }

    @Test
    fun offlineSearchWithNoMatchingHistoryFails() = runTest {
        coEvery { api.searchAssets("tesla", "all") } throws IOException("no network")

        try {
            repository.searchAssets("tesla", "all")
            fail("expected the network error")
        } catch (e: IOException) {
            assertEquals("no network", e.message)
        }
    }

    @Test
    fun deletingASimulationForgetsItsStoredCopy() = runTest {
        coEvery { api.getSimulation("sim-1") } returns sampleSimulation()
        repository.getSimulation("sim-1")
        coEvery { api.deleteSimulation("sim-1") } returns Response.success(Unit)

        repository.deleteSimulation("sim-1")

        assertFalse(cacheDao.details.containsKey("sim-1"))
    }
}
