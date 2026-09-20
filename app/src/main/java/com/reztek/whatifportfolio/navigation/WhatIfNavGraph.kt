package com.reztek.whatifportfolio.navigation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.ui.auth.SignInScreen
import com.reztek.whatifportfolio.ui.dashboard.DashboardScreen
import com.reztek.whatifportfolio.ui.settings.SettingsScreen

private const val TAG = "WhatIfNavGraph"

/**
 * Root navigation graph — the code counterpart of the Navigation Wireflow
 * diagram (Deliverable 3). Every [Destination] is a node; every
 * `navController.navigate(...)` call below is a labelled edge in that
 * diagram.
 *
 * Auth-gating strategy: rather than a separate splash/auth-check screen, the
 * start destination is decided once at graph-construction time from
 * [FirebaseAuth.currentUser]. Login uses `popUpTo(Login) { inclusive = true }`
 * on success so a signed-in user can never navigate back into it.
 */
@Composable
fun WhatIfNavGraph(
    navController: NavHostController = rememberNavController(),
    onGoogleSignInRequested: () -> Unit
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
                onSignInClicked = onGoogleSignInRequested,
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
            PlaceholderScreen(title = "Saved Simulations")
        }

        composable(Destination.SimulationBuilder.route) {
            PlaceholderScreen(title = "Simulation Builder")
        }

        composable(Destination.SimulationResults.route) {
            PlaceholderScreen(title = "Simulation Results")
        }

        composable(Destination.SimulationDetail.route) {
            PlaceholderScreen(title = "Simulation Detail")
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

/**
 * Shared bottom-tab navigation behaviour: single-top, restores previously
 * saved state per tab, and pops back to the graph's start so repeated tab
 * taps don't stack duplicate destinations — standard Compose Navigation
 * pattern for bottom bars (Google, Jetpack Navigation Compose docs).
 */
private fun NavHostController.navigateToBottomDestination(destination: BottomNavDestination) {
    Log.d(TAG, "Bottom nav -> ${destination.label}")
    navigate(destination.destination.route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
