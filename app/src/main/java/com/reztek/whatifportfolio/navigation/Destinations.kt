package com.reztek.whatifportfolio.navigation

/**
 * Every full-screen destination in the app. Modal overlays are hosted inside
 * their screens and do not add a navigation back-stack entry.
 */
sealed class Destination(val route: String) {
    data object Login : Destination("login")
    data object Dashboard : Destination("dashboard")

    data object SimulationBuilder : Destination("simulation_builder?draftId={draftId}") {
        const val ARG_DRAFT_ID = "draftId"

        fun createRoute(draftId: String? = null) =
            "simulation_builder?draftId=${draftId.orEmpty()}"
    }

    data object SimulationResults : Destination("simulation_results/{simulationId}") {
        const val ARG_SIMULATION_ID = "simulationId"

        fun createRoute(simulationId: String) =
            "simulation_results/$simulationId"
    }

    data object SavedSimulations : Destination("saved_simulations")

    data object SimulationDetail : Destination("simulation_detail/{simulationId}") {
        const val ARG_SIMULATION_ID = "simulationId"

        fun createRoute(simulationId: String) =
            "simulation_detail/$simulationId"
    }

    data object Settings : Destination("settings")

    data object CompareSelection : Destination("compare_selection")

    data object CompareSimulation : Destination("compare/{firstId}/{secondId}") {
        const val ARG_FIRST_ID = "firstId"
        const val ARG_SECOND_ID = "secondId"

        fun createRoute(firstId: String, secondId: String) =
            "compare/${android.net.Uri.encode(firstId)}/${android.net.Uri.encode(secondId)}"
    }
}

/** The three top-level destinations shown in the bottom navigation bar. */
enum class BottomNavDestination(
    val destination: Destination,
    val label: String
) {
    HOME(Destination.Dashboard, "Home"),
    SAVED(Destination.SavedSimulations, "Saved"),
    SETTINGS(Destination.Settings, "Settings")
}