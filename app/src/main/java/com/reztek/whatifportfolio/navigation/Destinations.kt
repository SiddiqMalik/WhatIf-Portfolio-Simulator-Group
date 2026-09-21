package com.reztek.whatifportfolio.navigation

/**
 * Every full-screen destination in the app, matching the nodes in the
 * Navigation Wireflow diagram (Deliverable 3). Modal overlays (Asset
 * Selector, inline validation states) are NOT routes here — per the design
 * doc they are shown as bottom sheets / in-place overlays and do not push a
 * back-stack entry, so they live inside the screen that hosts them rather
 * than in this graph.
 *
 * Person 1 scope implements Login, Dashboard, and Settings in full. The
 * remaining routes are declared so the graph is complete and navigable, with
 * placeholder screens ready for Person 2/3 to fill in.
 */
sealed class Destination(val route: String) {
    data object Login : Destination("login")
    data object Dashboard : Destination("dashboard")
    data object SimulationBuilder : Destination("simulation_builder?draftId={draftId}") {
        const val ARG_DRAFT_ID = "draftId"
        fun createRoute(draftId: String? = null) = "simulation_builder?draftId=${draftId ?: ""}"
    }
    data object SimulationResults : Destination("simulation_results/{simulationId}") {
        const val ARG_SIMULATION_ID = "simulationId"
        fun createRoute(simulationId: String) = "simulation_results/$simulationId"
    }
    data object SavedSimulations : Destination("saved_simulations")
    data object SimulationDetail : Destination("simulation_detail/{simulationId}") {
        const val ARG_SIMULATION_ID = "simulationId"
        fun createRoute(simulationId: String) = "simulation_detail/$simulationId"
    }
    data object Settings : Destination("settings")
}

/** The three top-level destinations shown in the bottom navigation bar. */
enum class BottomNavDestination(val destination: Destination, val label: String) {
    HOME(Destination.Dashboard, "Home"),
    SAVED(Destination.SavedSimulations, "Saved"),
    SETTINGS(Destination.Settings, "Settings")
}
