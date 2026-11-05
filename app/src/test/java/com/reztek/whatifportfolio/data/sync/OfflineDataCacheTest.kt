package com.reztek.whatifportfolio.data.sync

import com.reztek.whatifportfolio.data.local.CachedAssetSearchEntity
import com.reztek.whatifportfolio.data.local.CachedSimulationDao
import com.reztek.whatifportfolio.data.local.CachedSimulationDetailEntity
import com.reztek.whatifportfolio.data.local.CachedSimulationEntity
import com.reztek.whatifportfolio.data.local.DetailStamp
import com.reztek.whatifportfolio.data.local.OfflineCacheDao
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.data.remote.dto.AllocationDto
import com.reztek.whatifportfolio.data.remote.dto.AssetSearchResultDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.TimeSeriesPointDto
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** In-memory stand-in for the Room DAO so the cache logic runs without Android. */
class FakeOfflineCacheDao : OfflineCacheDao {
    val details = mutableMapOf<String, CachedSimulationDetailEntity>()
    val searches = mutableMapOf<String, CachedAssetSearchEntity>()

    override suspend fun upsertDetail(entity: CachedSimulationDetailEntity) { details[entity.id] = entity }
    override suspend fun getDetail(id: String) = details[id]
    override suspend fun detailStamps() = details.values.map { DetailStamp(it.id, it.updatedAt) }
    override suspend fun deleteDetail(id: String) { details.remove(id) }
    override suspend fun deleteDetailsNotInList() = Unit
    override suspend fun upsertSearch(entity: CachedAssetSearchEntity) { searches[entity.cacheKey] = entity }
    override suspend fun getSearch(key: String) = searches[key]
    override suspend fun allSearches() = searches.values.sortedByDescending { it.cachedAt }
    override suspend fun pruneSearches() = Unit
}

fun sampleSimulation(id: String = "sim-1", name: String = "My run") = SimulationDto(
    id = id,
    name = name,
    startDate = "2024-01-01",
    endDate = "2025-01-01",
    initialInvestment = 1000.0,
    recurringContribution = 100.0,
    status = "saved",
    totalContributed = 2200.0,
    finalValue = 2500.0,
    profitLoss = 300.0,
    percentReturn = 13.6,
    realValue = 2400.0,
    allocations = listOf(AllocationDto("AAPL", "equity", 100.0, "Apple")),
    timeSeries = listOf(
        TimeSeriesPointDto("2024-01-01", 1000.0, 1000.0, 1000.0),
        TimeSeriesPointDto("2025-01-01", 2200.0, 2500.0, 2400.0)
    ),
    createdAt = "2024-01-01T00:00:00.000Z",
    updatedAt = "2024-06-01T00:00:00.000Z"
)

/** PART3-ASH: unit tests for the local copy of simulation results and asset searches. */
class OfflineDataCacheTest {

    private val cacheDao = FakeOfflineCacheDao()
    private val simulationDao = mockk<CachedSimulationDao>()
    private val cache = OfflineDataCache(cacheDao, simulationDao, clock = { 1_000L })

    private fun localRow(status: SyncStatus, name: String) = CachedSimulationEntity(
        id = "sim-1",
        name = name,
        startDate = "2024-01-01",
        endDate = "2025-01-01",
        initialInvestment = 1000.0,
        recurringContribution = 100.0,
        frequency = "monthly",
        status = "saved",
        totalContributed = 2200.0,
        finalValue = 2500.0,
        profitLoss = 300.0,
        percentReturn = 13.6,
        realValue = null,
        realProfitLoss = null,
        realReturnPct = null,
        createdAt = "2024-01-01T00:00:00.000Z",
        updatedAt = "2024-06-01T00:00:00.000Z",
        syncStatus = status,
        localEditedAt = 0L
    )

    @Test
    fun savedDetailComesBackWithItsChartData() = runTest {
        coEvery { simulationDao.getById("sim-1") } returns null
        cache.saveDetail(sampleSimulation())

        val loaded = cache.loadDetail("sim-1")

        assertNotNull(loaded)
        assertEquals("My run", loaded!!.name)
        assertEquals(2, loaded.timeSeries!!.size)
        assertEquals("AAPL", loaded.allocations!!.first().symbol)
    }

    @Test
    fun unknownDetailIsNull() = runTest {
        assertNull(cache.loadDetail("nope"))
    }

    @Test
    fun pendingRenameIsShownOnTheOfflineCopy() = runTest {
        cache.saveDetail(sampleSimulation(name = "Old name"))
        coEvery { simulationDao.getById("sim-1") } returns localRow(SyncStatus.PENDING_UPDATE, "New name")

        assertEquals("New name", cache.loadDetail("sim-1")!!.name)
    }

    @Test
    fun simulationDeletedOfflineIsNoLongerOpenable() = runTest {
        cache.saveDetail(sampleSimulation())
        coEvery { simulationDao.getById("sim-1") } returns localRow(SyncStatus.PENDING_DELETE, "My run")

        assertNull(cache.loadDetail("sim-1"))
    }

    @Test
    fun removeDetailForgetsIt() = runTest {
        coEvery { simulationDao.getById("sim-1") } returns null
        cache.saveDetail(sampleSimulation())
        cache.removeDetail("sim-1")

        assertNull(cache.loadDetail("sim-1"))
    }

    @Test
    fun stampsReportTheCachedUpdatedAt() = runTest {
        cache.saveDetail(sampleSimulation())

        assertEquals(mapOf("sim-1" to "2024-06-01T00:00:00.000Z"), cache.detailStamps())
    }

    private val apple = AssetSearchResultDto("AAPL", "Apple Inc", "equity")
    private val bitcoin = AssetSearchResultDto("BTC", "Bitcoin", "crypto")

    @Test
    fun exactSearchIsAnsweredFromCache() = runTest {
        cache.saveSearch("Apple", "all", listOf(apple))

        assertEquals(listOf(apple), cache.loadSearch("  apple ", "all"))
    }

    @Test
    fun differentQueryIsFilteredFromEarlierResults() = runTest {
        cache.saveSearch("a", "all", listOf(apple, bitcoin))

        val results = cache.loadSearch("bit", "all")

        assertEquals(listOf(bitcoin), results)
    }

    @Test
    fun typeFilterIsRespectedOffline() = runTest {
        cache.saveSearch("a", "all", listOf(apple, bitcoin))

        assertTrue(cache.loadSearch("apple", "crypto").isEmpty())
        assertEquals(listOf(apple), cache.loadSearch("apple", "equity"))
    }

    @Test
    fun emptySearchResultsAreNotStored() = runTest {
        cache.saveSearch("zzz", "all", emptyList())

        assertTrue(cacheDao.searches.isEmpty())
    }

    @Test
    fun blankQueryReturnsNothingOffline() = runTest {
        cache.saveSearch("a", "all", listOf(apple))

        assertTrue(cache.loadSearch("   ", "all").isEmpty())
    }
}
