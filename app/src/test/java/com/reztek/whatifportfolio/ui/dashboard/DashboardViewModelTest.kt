package com.reztek.whatifportfolio.ui.dashboard

import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.reztek.whatifportfolio.MainDispatcherRule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Date

// Tests for DashboardViewModel. It loads data in its init block, so mocks
// must be set up before the ViewModel is constructed.
class DashboardViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var usersCollection: CollectionReference
    private lateinit var userDocument: DocumentReference
    private lateinit var simulationsCollection: CollectionReference
    private lateinit var orderedQuery: Query
    private lateinit var limitedQuery: Query

    private companion object {
        const val TEST_UID = "uid-12345"
    }

    @Before
    fun setUp() {
        auth = mockk(relaxed = true)
        firestore = mockk()
        usersCollection = mockk()
        userDocument = mockk()
        simulationsCollection = mockk()
        orderedQuery = mockk()
        limitedQuery = mockk()

        val user = mockk<FirebaseUser>(relaxed = true)
        every { user.uid } returns TEST_UID
        every { user.displayName } returns "David Nkosi"
        every { auth.currentUser } returns user

        every { firestore.collection("users") } returns usersCollection
        every { usersCollection.document(TEST_UID) } returns userDocument
        every { userDocument.collection("simulations") } returns simulationsCollection
        every {
            simulationsCollection.orderBy("updatedAt", Query.Direction.DESCENDING)
        } returns orderedQuery
        every { orderedQuery.limit(3L) } returns limitedQuery
    }

    /** Builds a mock Firestore document with the fields the Dashboard maps. */
    private fun simulationDocument(
        id: String,
        name: String?,
        finalValue: Double? = 10_000.0,
        percentReturn: Double? = 12.5,
        updatedAtMillis: Long? = 1_700_000_000_000L
    ): DocumentSnapshot = mockk<DocumentSnapshot>(relaxed = true).also { doc ->
        every { doc.id } returns id
        every { doc.getString("name") } returns name
        every { doc.getDouble("finalValue") } returns finalValue
        every { doc.getDouble("percentReturn") } returns percentReturn
        every { doc.getTimestamp("updatedAt") } returns updatedAtMillis?.let { millis ->
            mockk<Timestamp>().also { ts -> every { ts.toDate() } returns Date(millis) }
        }
    }

    private fun stubSnapshotWith(documents: List<DocumentSnapshot>) {
        val snapshot = mockk<QuerySnapshot>()
        every { snapshot.documents } returns documents
        every { limitedQuery.get() } returns Tasks.forResult(snapshot)
    }

    @Test
    fun loadDashboard_whenNoAuthenticatedUser_emitsSignedOutError() {
        every { auth.currentUser } returns null

        val viewModel = DashboardViewModel(firestore, auth)

        val state = viewModel.uiState.value
        assertTrue(state is DashboardUiState.Error)
        assertEquals("You've been signed out.", (state as DashboardUiState.Error).message)
    }

    @Test
    fun loadDashboard_whenNoAuthenticatedUser_doesNotQueryFirestore() {
        every { auth.currentUser } returns null

        DashboardViewModel(firestore, auth)

        verify(exactly = 0) { firestore.collection(any()) }
    }

    @Test
    fun loadDashboard_withValidDocuments_mapsThemToSimulationSummaries() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubSnapshotWith(
                listOf(
                    simulationDocument(
                        id = "sim-1",
                        name = "Tech heavy 10yr",
                        finalValue = 152_340.75,
                        percentReturn = 52.34,
                        updatedAtMillis = 1_700_000_000_000L
                    )
                )
            )

            val viewModel = DashboardViewModel(firestore, auth)

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
    fun loadDashboard_requestsThreeMostRecentSimulationsForSignedInUser() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubSnapshotWith(emptyList())

            DashboardViewModel(firestore, auth)

            verify { usersCollection.document(TEST_UID) }
            verify { simulationsCollection.orderBy("updatedAt", Query.Direction.DESCENDING) }
            verify { orderedQuery.limit(3L) }
        }

    @Test
    fun loadDashboard_dropsDocumentsWithoutAName() =
        runTest(mainDispatcherRule.testDispatcher) {
            // A doc with no name shouldn't show up as a blank row.
            stubSnapshotWith(
                listOf(
                    simulationDocument(id = "sim-good", name = "Balanced 5yr"),
                    simulationDocument(id = "sim-broken", name = null)
                )
            )

            val viewModel = DashboardViewModel(firestore, auth)

            val loaded = viewModel.uiState.value as DashboardUiState.Loaded
            assertEquals(1, loaded.recentSimulations.size)
            assertEquals("sim-good", loaded.recentSimulations.single().id)
        }

    @Test
    fun loadDashboard_whenNumericFieldsMissing_defaultsThemToZero() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubSnapshotWith(
                listOf(
                    simulationDocument(
                        id = "sim-partial",
                        name = "Draft simulation",
                        finalValue = null,
                        percentReturn = null,
                        updatedAtMillis = null
                    )
                )
            )

            val viewModel = DashboardViewModel(firestore, auth)

            val summary = (viewModel.uiState.value as DashboardUiState.Loaded)
                .recentSimulations.single()
            assertEquals(0.0, summary.finalValue, 0.001)
            assertEquals(0.0, summary.percentReturn, 0.001)
            assertEquals(0L, summary.updatedAtMillis)
        }

    @Test
    fun loadDashboard_withNegativeReturn_preservesTheSign() =
        runTest(mainDispatcherRule.testDispatcher) {
            // Negative returns should come through as-is, not get dropped.
            stubSnapshotWith(
                listOf(
                    simulationDocument(
                        id = "sim-loss",
                        name = "2008 crash scenario",
                        finalValue = 4_820.10,
                        percentReturn = -51.8
                    )
                )
            )

            val viewModel = DashboardViewModel(firestore, auth)

            val summary = (viewModel.uiState.value as DashboardUiState.Loaded)
                .recentSimulations.single()
            assertEquals(-51.8, summary.percentReturn, 0.001)
        }

    @Test
    fun loadDashboard_withNoSavedSimulations_emitsLoadedWithEmptyList() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubSnapshotWith(emptyList())

            val viewModel = DashboardViewModel(firestore, auth)

            val state = viewModel.uiState.value
            assertTrue(state is DashboardUiState.Loaded)
            assertTrue((state as DashboardUiState.Loaded).recentSimulations.isEmpty())
        }

    @Test
    fun loadDashboard_greetsUserByFirstNameOnly() =
        runTest(mainDispatcherRule.testDispatcher) {
            stubSnapshotWith(emptyList())

            val viewModel = DashboardViewModel(firestore, auth)

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
            stubSnapshotWith(emptyList())

            val viewModel = DashboardViewModel(firestore, auth)

            assertEquals(
                "there",
                (viewModel.uiState.value as DashboardUiState.Loaded).displayName
            )
        }

    @Test
    fun loadDashboard_whenFirestoreReadFails_emitsRecoverableError() =
        runTest(mainDispatcherRule.testDispatcher) {
            every { limitedQuery.get() } returns
                Tasks.forException(RuntimeException("UNAVAILABLE: network error"))

            val viewModel = DashboardViewModel(firestore, auth)

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
            every { limitedQuery.get() } returns
                Tasks.forException(RuntimeException("UNAVAILABLE"))
            val viewModel = DashboardViewModel(firestore, auth)
            assertTrue(viewModel.uiState.value is DashboardUiState.Error)

            // Simulates the user tapping "Retry" once connectivity is restored.
            stubSnapshotWith(listOf(simulationDocument(id = "sim-1", name = "Retry works")))
            viewModel.loadDashboard()

            assertTrue(viewModel.uiState.value is DashboardUiState.Loaded)
        }
}
