package com.reztek.whatifportfolio.ui.simulation

import android.app.Application
import com.reztek.whatifportfolio.MainDispatcherRule
import com.reztek.whatifportfolio.data.engine.SimulationEngine
import com.reztek.whatifportfolio.data.local.SavedSimulationEntity
import com.reztek.whatifportfolio.data.local.SimulationRepository
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class CompareViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var remote: RemoteSimulationRepository
    private lateinit var local: SimulationRepository
    private lateinit var viewModel: CompareViewModel

    @Before
    fun setUp() {
        remote = mockk()
        local = mockk()
        viewModel = CompareViewModel(mockk<Application>(relaxed = true), remote, local)
    }

    private fun remoteDto(id: String, name: String, finalValue: Double = 7200.0) = SimulationDto(
        id = id,
        name = name,
        startDate = "2021-01-01",
        endDate = "2026-01-01",
        initialInvestment = 1000.0,
        recurringContribution = 100.0,
        status = "saved",
        totalContributed = 7000.0,
        finalValue = finalValue,
        profitLoss = finalValue - 7000.0,
        percentReturn = (finalValue - 7000.0) / 7000.0 * 100.0,
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z"
    )

    private fun localEntity(id: Long): SavedSimulationEntity {
        val result = SimulationEngine.calculateSimulation(1000.0, 100.0, 8.0, 4.0, 3)
        return SavedSimulationEntity(
            id = id,
            title = "Local $id",
            initialInvestment = 1000.0,
            monthlyContribution = 100.0,
            returnRate = 8.0,
            inflationRate = 4.0,
            years = 3,
            finalNominalValue = result.finalNominalValue,
            finalRealValue = result.finalRealValue,
            createdAt = 0L
        )
    }

    @Test
    fun comparesARemoteAndALocalSimulation() {
        coEvery { remote.getSimulation("r1") } returns remoteDto("r1", "Cloud run")
        coEvery { local.getSimulationById(5L) } returns localEntity(5L)

        viewModel.load("remote-r1", "local-5")

        val state = viewModel.uiState.value as CompareUiState.Ready
        assertEquals("Cloud run", state.first.title)
        assertEquals("Local 5", state.second.title)
        assertEquals(4, state.second.chartPoints.size)
    }

    @Test
    fun comparesTwoRemoteSimulationsInTheOrderSelected() {
        coEvery { remote.getSimulation("a") } returns remoteDto("a", "A", 8000.0)
        coEvery { remote.getSimulation("b") } returns remoteDto("b", "B", 6000.0)

        viewModel.load("remote-a", "remote-b")

        val state = viewModel.uiState.value as CompareUiState.Ready
        assertEquals("A", state.first.title)
        assertEquals("B", state.second.title)
        assertEquals(ComparisonWinner.FIRST, ComparisonHighlights.winner(state.first.finalValue, state.second.finalValue))
    }

    @Test
    fun reportsAMissingLocalSimulation() {
        coEvery { remote.getSimulation("r1") } returns remoteDto("r1", "Cloud run")
        coEvery { local.getSimulationById(9L) } returns null

        viewModel.load("remote-r1", "local-9")

        val state = viewModel.uiState.value as CompareUiState.Error
        assertTrue(state.message.contains("no longer exists"))
    }

    @Test
    fun reportsUnrecognisedKeysWithoutCallingTheRepositories() {
        coEvery { local.getSimulationById(any()) } returns null

        viewModel.load("garbage", "local-1")

        assertTrue(viewModel.uiState.value is CompareUiState.Error)
        coVerify(exactly = 0) { remote.getSimulation(any()) }
    }

    @Test
    fun rejectsComparingASimulationWithItself() {
        viewModel.load("local-1", "local-1")

        assertTrue(viewModel.uiState.value is CompareUiState.Error)
    }

    @Test
    fun surfacesNetworkFailuresAndRecoversOnRetry() {
        coEvery { remote.getSimulation("r1") } throws java.io.IOException("offline")
        coEvery { local.getSimulationById(5L) } returns localEntity(5L)

        viewModel.load("remote-r1", "local-5")
        assertEquals("offline", (viewModel.uiState.value as CompareUiState.Error).message)

        coEvery { remote.getSimulation("r1") } returns remoteDto("r1", "Cloud run")
        viewModel.retry()

        assertTrue(viewModel.uiState.value is CompareUiState.Ready)
    }

    @Test
    fun loadingTheSameKeysTwiceDoesNotRefetch() {
        coEvery { remote.getSimulation("r1") } returns remoteDto("r1", "Cloud run")
        coEvery { local.getSimulationById(5L) } returns localEntity(5L)

        viewModel.load("remote-r1", "local-5")
        viewModel.load("remote-r1", "local-5")

        coVerify(exactly = 1) { remote.getSimulation("r1") }
    }
}
