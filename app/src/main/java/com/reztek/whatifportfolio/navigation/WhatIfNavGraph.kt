package com.reztek.whatifportfolio.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.ui.auth.SignInScreen
import com.reztek.whatifportfolio.ui.dashboard.DashboardScreen
import com.reztek.whatifportfolio.ui.settings.SettingsScreen
import com.reztek.whatifportfolio.ui.simulation.SavedSimulationsScreen
import com.reztek.whatifportfolio.ui.simulation.SimulationBuilderScreen
import com.reztek.whatifportfolio.ui.simulation.SimulationDetailScreen
import com.reztek.whatifportfolio.ui.simulation.SimulationResultsScreen

private const val TAG = "WhatIfNavGraph"

@Composable
fun WhatIfNavGraph(
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (FirebaseAuth.getInstance().currentUser != null) {
        Log.d(TAG, "Existing Firebase session found — starting at Dashboard")
        Destination.Dashboard.route
    } else {
        Log.d(TAG, "No Firebase session — starting at Login")
        Destination.Login.route
    }

    NavHost(navController = navController, startDestination = startDestination) {

        composable(Destination.Login.route) {
            SignInScreen(
                onSignedIn = {
                    Log.i(TAG, "Navigating Login -> Dashboard")
                    navController.navigate(Destination.Dashboard.route) {
                        popUpTo(Destination.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Destination.Dashboard.route) {
            DashboardScreen(
                onNewSimulation = {
                    Log.d(TAG, "Navigating Dashboard -> SimulationBuilder")
                    navController.navigate(Destination.SimulationBuilder.createRoute())
                },
                onOpenSimulation = { simulationId ->
                    Log.d(TAG, "Navigating Dashboard -> SimulationDetail($simulationId)")
                    navController.navigate(Destination.SimulationDetail.createRoute(simulationId))
                },
                onViewAllSaved = {
                    Log.d(TAG, "Navigating Dashboard -> SavedSimulations")
                    navController.navigate(Destination.SavedSimulations.route)
                },
                onBottomNavSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                }
            )
        }

        composable(Destination.SavedSimulations.route) {
            SavedSimulationsScreen(
                onOpenSimulation = { id ->
                    navController.navigate(Destination.SimulationDetail.createRoute(id))
                },
                onRerun = { id ->
                    navController.navigate(Destination.SimulationBuilder.createRoute(id))
                },
                onBottomNavSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                }
            )
        }

        composable(
            route = Destination.SimulationBuilder.route,
                arguments = listOf(
                navArgument(Destination.SimulationBuilder.ARG_DRAFT_ID) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val draftId = entry.arguments?.getString(Destination.SimulationBuilder.ARG_DRAFT_ID)
            SimulationBuilderScreen(
                draftId = draftId?.takeIf { it.isNotBlank() },
                onBack = { navController.popBackStack() },
                onSimulationCreated = { id ->
                    navController.navigate(Destination.SimulationResults.createRoute(id)) {
                        popUpTo(Destination.SimulationBuilder.route) { inclusive = true }
                    }
                },
                onLocalSaved = {
                    navController.navigate(Destination.SavedSimulations.route) {
                        popUpTo(Destination.Dashboard.route)
                    }
                }
            )
        }

        composable(
            route = Destination.SimulationResults.route,
            arguments = listOf(
                navArgument(Destination.SimulationResults.ARG_SIMULATION_ID) {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString(Destination.SimulationResults.ARG_SIMULATION_ID).orEmpty()
            SimulationResultsScreen(
                simulationId = id,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Destination.SimulationDetail.route,
            arguments = listOf(
                navArgument(Destination.SimulationDetail.ARG_SIMULATION_ID) {
                    type = NavType.StringType
                }
            )
        ) { entry ->
            val id = entry.arguments?.getString(Destination.SimulationDetail.ARG_SIMULATION_ID).orEmpty()
            SimulationDetailScreen(
                simulationId = id,
                onBack = { navController.popBackStack() },
                onRerun = { newId ->
                    navController.navigate(Destination.SimulationResults.createRoute(newId))
                },
                onDuplicate = { draftId ->
                    navController.navigate(Destination.SimulationBuilder.createRoute(draftId))
                },
                onDeleted = {
                    navController.navigate(Destination.SavedSimulations.route) {
                        popUpTo(Destination.Dashboard.route)
                    }
                }
            )
        }

        composable(Destination.Settings.route) {
            SettingsScreen(
                onLoggedOut = {
                    Log.i(TAG, "Navigating Settings -> Login (logged out)")
                    navController.navigate(Destination.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onBottomNavSelected = { destination ->
                    navController.navigateToBottomDestination(destination)
                }
            )
        }
    }
}

private fun NavHostController.navigateToBottomDestination(destination: BottomNavDestination) {
    Log.d(TAG, "Bottom nav -> ${destination.label}")
    navigate(destination.destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
