package com.reztek.whatifportfolio.ui.dashboard

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.reztek.whatifportfolio.MainDispatcherRule
import com.reztek.whatifportfolio.data.remote.dto.SimulationDto
import com.reztek.whatifportfolio.data.repository.RemoteSimulationRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

// Tests for DashboardViewModel. It loads data in its init block, so mocks
// must be set up before the ViewModel is constructed. Data now comes from
// RemoteSimulationRepository (our REST API), not Firestore directly.
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var simulationRepository: RemoteSimulationRepository

    private companion object {
        const val TEST_UID = "uid-12345"
    }

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        simulationRepository = mockk()

        val user = mockk<FirebaseUser>(relaxed = true)
        every { user.uid } returns TEST_UID
        every { user.displayName } returns "David Nkosi"
        every { auth.currentUser } returns user
    }

    /** Builds a DTO exactly as the API would return it, with sensible defaults. */
    private fun sampleDto(
        id: String,
        name: String = "Sample simulation",
        finalValue: Double = 10_000.0,
        percentReturn: Double = 12.5,
        updatedAt: String = "2024-01-01T00:00:00Z"
    ): SimulationDto = SimulationDto(
        id = id,
        name = name,
        startDate = "2023-01-01T00:00:00Z",
        endDate = "2024-01-01T00:00:00Z",
        initialInvestment = 1_000.0,
        recurringContribution = 100.0,
        frequency = "monthly",
        status = "active",
        totalContributed = 2_200.0,
        finalValue = finalValue,
        profitLoss = finalValue - 2_200.0,
        percentReturn = percentReturn,
        realValue = finalValue,
        realProfitLoss = finalValue - 2_200.0,
        realReturnPct = percentReturn,
        createdAt = "2023-01-01T00:00:00Z",
        updatedAt = updatedAt
    )

    @Test
    fun loadDashboard_whenNoAuthenticatedUser_emitsSignedOutError() {
        every { auth.currentUser } returns null

        val viewModel = DashboardViewModel(simulationRepository, auth)

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertEquals("You've been signed out.", (state as DashboardUiState.Error).message)
    }

    @Test
    fun loadDashboard_whenNoAuthenticatedUser_doesNotCallRepository() {
        every { auth.currentUser } returns null

        DashboardViewModel(simulationRepository, auth)

        coVerify(exactly = 0) { simulationRepository.listSimulations() }
    }

    @Test
    fun loadDashboard_withValidSimulations_mapsThemToSimulationSummaries() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } returns listOf(
                sampleDto(
                    id = "sim-1",
                    name = "Tech heavy 10yr",
                    finalValue = 152_340.75,
                    percentReturn = 52.34,
                    updatedAt = "2023-11-14T22:13:20Z"
                )
            )

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val state = viewModel.uiState.value
            assertTrue(state is DashboardUiState.Loaded)
            val summary = (state as DashboardUiState.Loaded).recentSimulations.single()
            assertEquals("sim-1", summary.id)
            assertEquals("Tech heavy 10yr", summary.name)
            assertEquals(152_340.75, summary.finalValue, 0.001)
            assertEquals(52.34, summary.percentReturn, 0.001)
            assertEquals(1_700_000_000_000L, summary.updatedAtMillis)
        }

    @Test
    fun loadDashboard_sortsByUpdatedAtDescendingAndTakesTop3() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } returns listOf(
                sampleDto(id = "sim-oldest", updatedAt = "2021-01-01T00:00:00Z"),
                sampleDto(id = "sim-newest", updatedAt = "2024-01-01T00:00:00Z"),
                sampleDto(id = "sim-middle", updatedAt = "2022-01-01T00:00:00Z"),
                sampleDto(id = "sim-second-newest", updatedAt = "2023-01-01T00:00:00Z")
            )

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val loaded = viewModel.uiState.value as DashboardUiState.Loaded
            assertEquals(3, loaded.recentSimulations.size)
            assertEquals(
                listOf("sim-newest", "sim-second-newest", "sim-middle"),
                loaded.recentSimulations.map { it.id }
            )
        }

    @Test
    fun loadDashboard_withUnparsableUpdatedAt_defaultsMillisToZero() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } returns listOf(
                sampleDto(id = "sim-bad-date", updatedAt = "not-a-real-date")
            )

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val summary = (viewModel.uiState.value as DashboardUiState.Loaded)
                .recentSimulations.single()
            assertEquals(0L, summary.updatedAtMillis)
        }

    @Test
    fun loadDashboard_withNegativeReturn_preservesTheSign() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Negative returns should come through as-is, not get dropped.
            coEvery { simulationRepository.listSimulations() } returns listOf(
                sampleDto(id = "sim-loss", name = "2008 crash scenario", finalValue = 4_820.10, percentReturn = -51.8)
            )

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val summary = (viewModel.uiState.value as DashboardUiState.Loaded)
                .recentSimulations.single()
            assertEquals(-51.8, summary.percentReturn, 0.001)
        }

    @Test
    fun loadDashboard_withNoSavedSimulations_emitsLoadedWithEmptyList() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } returns emptyList()

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val state = viewModel.uiState.value
            assertTrue(state is DashboardUiState.Loaded)
            assertTrue((state as DashboardUiState.Loaded).recentSimulations.isEmpty())
        }

    @Test
    fun loadDashboard_greetsUserByFirstNameOnly() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } returns emptyList()

            val viewModel = DashboardViewModel(simulationRepository, auth)

            assertEquals(
                "David",
                (viewModel.uiState.value as DashboardUiState.Loaded).displayName
            )
        }

    @Test
    fun loadDashboard_whenDisplayNameIsNull_fallsBackToGenericGreeting() =
        runTest(mainDispatcherRule.testDispatcher) {
            val user = mockk<FirebaseUser>(relaxed = true)
            every { user.uid } returns TEST_UID
            every { user.displayName } returns null
            every { auth.currentUser } returns user
            coEvery { simulationRepository.listSimulations() } returns emptyList()

            val viewModel = DashboardViewModel(simulationRepository, auth)

            assertEquals(
                "there",
                (viewModel.uiState.value as DashboardUiState.Loaded).displayName
            )
        }

    @Test
    fun loadDashboard_whenRepositoryThrows_emitsRecoverableError() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } throws
                RuntimeException("UNAVAILABLE: network error")

            val viewModel = DashboardViewModel(simulationRepository, auth)

            val state = viewModel.uiState.value
            assertTrue(state is DashboardUiState.Error)
            assertEquals(
                "Couldn't load your simulations. Check your connection and try again.",
                (state as DashboardUiState.Error).message
            )
        }

    @Test
    fun loadDashboard_calledAgainAfterFailure_recoversToLoaded() =
        runTest(mainDispatcherRule.testDispatcher) {
            coEvery { simulationRepository.listSimulations() } throws
                RuntimeException("UNAVAILABLE")
            val viewModel = DashboardViewModel(simulationRepository, auth)
            assertTrue(viewModel.uiState.value is DashboardUiState.Error)

            // Simulates the user tapping "Retry" once connectivity is restored.
            coEvery { simulationRepository.listSimulations() } returns
                listOf(sampleDto(id = "sim-1", name = "Retry works"))
            viewModel.loadDashboard()

            assertTrue(viewModel.uiState.value is DashboardUiState.Loaded)
        }
}
