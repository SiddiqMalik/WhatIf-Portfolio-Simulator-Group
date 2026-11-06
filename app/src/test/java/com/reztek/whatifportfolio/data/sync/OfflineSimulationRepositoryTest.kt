package com.reztek.whatifportfolio.data.sync

import com.reztek.whatifportfolio.data.local.CachedSimulationDao
import com.reztek.whatifportfolio.data.local.CachedSimulationEntity
import com.reztek.whatifportfolio.data.local.SyncStatus
import com.reztek.whatifportfolio.data.remote.ApiService
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.remote.dto.SimulationListResponse
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/**
 * PART3-ASH: unit tests for the offline queue and sync rules (no Android runtime needed).
 */
class OfflineSimulationRepositoryTest {

    private val dao = mockk<CachedSimulationDao>(relaxed = true)
    private val api = mockk<ApiService>()
    private val repository = OfflineSimulationRepository(dao = dao, api = api, clock = { NOW })

    private fun row(
        id: String = "sim-1",
        name: String = "My run",
        status: SyncStatus = SyncStatus.SYNCED,
        editedAt: Long = 0L
    ) = CachedSimulationEntity(
        id = id,
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
        localEditedAt = editedAt
    )

    private fun dto(id: String, name: String) = SimulationDto(
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
        createdAt = "2024-01-01T00:00:00.000Z",
        updatedAt = "2024-06-02T00:00:00.000Z"
    )

    private fun httpError(code: Int) = HttpException(Response.error<Any>(code, "".toResponseBody(null)))

    @Test
    fun `rename is applied locally and queued`() = runTest {
        coEvery { dao.getById("sim-1") } returns row()

        val changed = repository.renameLocally("sim-1", "  New name  ")

        assertTrue(changed)
        coVerify {
            dao.upsert(
                match {
                    it.name == "New name" &&
                        it.syncStatus == SyncStatus.PENDING_UPDATE &&
                        it.localEditedAt == NOW
                }
            )
        }
    }

    @Test
    fun `blank rename is ignored`() = runTest {
        coEvery { dao.getById("sim-1") } returns row()

        assertFalse(repository.renameLocally("sim-1", "   "))
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `delete hides the row and queues the delete`() = runTest {
        coEvery { dao.getById("sim-1") } returns row()

        assertTrue(repository.deleteLocally("sim-1"))
        coVerify { dao.upsert(match { it.syncStatus == SyncStatus.PENDING_DELETE }) }
    }

    @Test
    fun `successful delete removes the local row`() = runTest {
        val pending = row(status = SyncStatus.PENDING_DELETE, editedAt = 5L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(pending), emptyList())
        coEvery { api.deleteSimulation("sim-1") } returns Response.success(Unit)

        assertEquals(SyncOutcome.DONE, repository.syncPending())
        coVerify { dao.deleteById("sim-1") }
    }

    @Test
    fun `delete of something already gone on the server counts as synced`() = runTest {
        val pending = row(status = SyncStatus.PENDING_DELETE, editedAt = 5L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(pending), emptyList())
        coEvery { api.deleteSimulation("sim-1") } returns Response.error<Unit>(404, "".toResponseBody(null))

        assertEquals(SyncOutcome.DONE, repository.syncPending())
        coVerify { dao.deleteById("sim-1") }
    }

    @Test
    fun `successful rename is marked synced with the server copy`() = runTest {
        val pending = row(name = "Renamed", status = SyncStatus.PENDING_UPDATE, editedAt = 5L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(pending), emptyList())
        coEvery { api.updateSimulation("sim-1", mapOf("name" to "Renamed")) } returns dto("sim-1", "Renamed")
        coEvery { dao.getById("sim-1") } returns pending

        assertEquals(SyncOutcome.DONE, repository.syncPending())
        coVerify { dao.upsert(match { it.syncStatus == SyncStatus.SYNCED && it.name == "Renamed" }) }
    }

    @Test
    fun `a newer edit made during the request stays queued`() = runTest {
        val sent = row(name = "First", status = SyncStatus.PENDING_UPDATE, editedAt = 5L)
        val newer = row(name = "Second", status = SyncStatus.PENDING_UPDATE, editedAt = 9L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(sent), listOf(newer))
        coEvery { api.updateSimulation("sim-1", mapOf("name" to "First")) } returns dto("sim-1", "First")
        coEvery { dao.getById("sim-1") } returns newer

        assertEquals(SyncOutcome.RETRY, repository.syncPending())
        coVerify(exactly = 0) { dao.upsert(match { it.syncStatus == SyncStatus.SYNCED }) }
    }

    @Test
    fun `no network keeps the change queued and asks for a retry`() = runTest {
        val pending = row(name = "Renamed", status = SyncStatus.PENDING_UPDATE, editedAt = 5L)
        coEvery { dao.getPending() } returns listOf(pending)
        coEvery { api.updateSimulation(any(), any()) } throws IOException("offline")

        assertEquals(SyncOutcome.RETRY, repository.syncPending())
        coVerify(exactly = 0) { dao.deleteById(any()) }
        coVerify(exactly = 0) { dao.upsert(any()) }
    }

    @Test
    fun `permanent server rejection marks the rename failed instead of losing it`() = runTest {
        val pending = row(name = "Renamed", status = SyncStatus.PENDING_UPDATE, editedAt = 5L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(pending), emptyList())
        coEvery { api.updateSimulation(any(), any()) } throws httpError(400)
        coEvery { dao.getById("sim-1") } returns pending

        assertEquals(SyncOutcome.DONE, repository.syncPending())
        coVerify { dao.upsert(match { it.syncStatus == SyncStatus.FAILED_UPDATE && it.name == "Renamed" }) }
    }

    @Test
    fun `rename of a simulation deleted on the server is dropped`() = runTest {
        val pending = row(name = "Renamed", status = SyncStatus.PENDING_UPDATE, editedAt = 5L)
        coEvery { dao.getPending() } returnsMany listOf(listOf(pending), emptyList())
        coEvery { api.updateSimulation(any(), any()) } throws httpError(404)
        coEvery { dao.getById("sim-1") } returns pending

        assertEquals(SyncOutcome.DONE, repository.syncPending())
        coVerify { dao.deleteById("sim-1") }
    }

    @Test
    fun `refresh never overwrites rows with unsynced local edits`() = runTest {
        coEvery { api.listSimulations(null) } returns SimulationListResponse(
            listOf(dto("a", "Server A"), dto("b", "Server B"))
        )
        coEvery { dao.getUnsyncedIds() } returns listOf("a")

        repository.refreshFromServer()

        coVerify { dao.upsertAll(match { rows -> rows.map { it.id } == listOf("b") }) }
        coVerify { dao.deleteSyncedNotIn(listOf("a", "b")) }
    }

    private companion object {
        const val NOW = 1_000L
    }
}
