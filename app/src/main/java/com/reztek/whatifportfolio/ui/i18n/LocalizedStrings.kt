package com.reztek.whatifportfolio.ui.i18n

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import com.reztek.whatifportfolio.R

/**
 * Translates English strings that are still produced by ViewModels, so a
 * language change updates validation, errors, and status text without those
 * classes holding a Context. Unknown text (server messages, names) is returned
 * unchanged.
 */
fun localize(context: Context, text: String): String {
    EXACT[text]?.let { return context.getString(it) }

    ALLOCATIONS_TOTAL.matchEntire(text)?.let { match ->
        return context.getString(R.string.error_allocations_total, match.groupValues[1])
    }
    RUN_HTTP.matchEntire(text)?.let { match ->
        return context.getString(R.string.error_run_http, match.groupValues[1])
    }
    OFFLINE_WAITING.matchEntire(text)?.let { match ->
        val count = match.groupValues[1].toIntOrNull() ?: return text
        val waiting = context.resources.getQuantityString(R.plurals.sync_changes_waiting, count, count)
        return context.getString(R.string.offline_waiting_sync, waiting)
    }
    CHANGES_WAITING.matchEntire(text)?.let { match ->
        val count = match.groupValues[1].toIntOrNull() ?: return text
        return context.resources.getQuantityString(R.plurals.sync_changes_waiting, count, count)
    }

    val passwordInstead = " Your password will be used instead."
    if (text.endsWith(passwordInstead)) {
        val base = EXACT[text.removeSuffix(passwordInstead)] ?: return text
        return context.getString(base) + " " + context.getString(R.string.biometric_password_instead)
    }
    return text
}

@Composable
fun localized(text: String): String = localize(LocalContext.current, text)

private val ALLOCATIONS_TOTAL =
    Regex("""Allocations must total 100% \(currently ([0-9]+(?:[.,][0-9]+)?)%\)\.""")
private val RUN_HTTP =
    Regex("""Couldn't run the simulation \(HTTP (\d+)\)\.""")
private val OFFLINE_WAITING =
    Regex("""Offline - (\d+) changes? waiting to sync\. They sync automatically when you reconnect\.""")
private val CHANGES_WAITING =
    Regex("""(\d+) changes? waiting to sync""")

private val EXACT = mapOf(
    "Enter your email address." to R.string.error_email_required,
    "Enter a valid email address." to R.string.error_email_invalid,
    "Password must be at least 6 characters." to R.string.error_password_short,
    "Incorrect email or password." to R.string.error_wrong_password,
    "No account found for that email. Create one first." to R.string.error_user_not_found,
    "That email is already registered. Sign in instead." to R.string.error_email_in_use,
    "Network error. Check your connection and try again." to R.string.error_network,
    "Email/password sign-in is disabled in Firebase Console." to R.string.error_sign_in_disabled,
    "Registration failed. Please try again." to R.string.error_registration_failed,
    "Sign-in failed. Please try again." to R.string.error_sign_in_failed,
    "Give your scenario a name." to R.string.error_scenario_name,
    "Initial investment must be greater than 0." to R.string.error_initial_positive,
    "Contribution must be 0 or more." to R.string.error_contribution,
    "End date must be after start date." to R.string.error_end_after_start,
    "Use valid dates (YYYY-MM-DD)." to R.string.error_dates_format,
    "Add at least one asset." to R.string.error_add_asset,
    "Allocation percentages must be 0 or more." to R.string.error_allocation_percent,
    "Add a name, assets totaling 100%, and valid dates to run." to R.string.error_run_requirements,
    "Please sign in again to run simulations." to R.string.error_sign_in_again,
    "Market data provider failed. Try again in a moment." to R.string.error_market_data,
    "Couldn't run the simulation. Try again." to R.string.error_run_generic,
    "Enter a scenario name and a positive initial investment." to R.string.error_projection_required,
    "Couldn't save projection" to R.string.error_save_projection,
    "Projection saved on this device" to R.string.message_projection_saved,
    "Couldn't load your simulations. Check your connection and try again." to R.string.dashboard_error_generic,
    "Couldn't load simulation results." to R.string.error_load_results,
    "Couldn't load this simulation." to R.string.error_load_simulation,
    "Simulation saved" to R.string.message_simulation_saved,
    "Rerun failed" to R.string.error_rerun_failed,
    "Delete failed" to R.string.error_delete_failed,
    "Couldn't load cloud simulations." to R.string.error_load_cloud,
    "You're offline and nothing is saved on this device yet. Connect once to load your simulations." to R.string.error_offline_empty,
    "Saved on this device - will sync when you're back online" to R.string.message_saved_offline,
    "Deleted on this device - will sync when you're back online" to R.string.message_deleted_offline,
    "All changes synced" to R.string.message_all_synced,
    "Still waiting to sync - check your connection" to R.string.message_sync_waiting,
    "One of the selected simulations could not be read. Go back and choose two again." to R.string.error_compare_invalid,
    "Choose two different simulations to compare." to R.string.error_compare_same,
    "One of the selected simulations no longer exists. Go back and choose two again." to R.string.error_compare_missing,
    "Couldn't reach the server. Check your connection and try again." to R.string.error_compare_offline,
    "The server couldn't load a simulation right now. Try again." to R.string.error_compare_server,
    "Something went wrong while loading the comparison. Try again." to R.string.error_compare_unknown,
    "Ask for your fingerprint or face when the app opens." to R.string.biometric_subtitle_available,
    "No fingerprint or face is enrolled. Add one in your device settings first." to R.string.biometric_subtitle_none_enrolled,
    "The biometric sensor is not available right now. Try again later." to R.string.biometric_subtitle_hw_unavailable,
    "This device has no biometric hardware." to R.string.biometric_subtitle_no_hardware,
    "A security update is needed before biometrics can be used." to R.string.biometric_subtitle_security_update,
    "Biometric status could not be determined on this device." to R.string.biometric_subtitle_unknown,
    "Unlock cancelled. Tap the button to try again." to R.string.biometric_error_cancelled,
    "The biometric prompt was interrupted. Tap the button to try again." to R.string.biometric_error_interrupted,
    "Too many attempts. Wait a moment, or use your password instead." to R.string.biometric_error_lockout,
    "Biometrics are locked on this device. Use your password instead." to R.string.biometric_error_lockout_permanent,
    "No fingerprint or face is enrolled. Use your password instead." to R.string.biometric_error_none_enrolled,
    "Biometrics are unavailable. Use your password instead." to R.string.biometric_error_unavailable,
    "The prompt timed out. Tap the button to try again." to R.string.biometric_error_timeout,
    "Couldn't read your biometric. Try again." to R.string.biometric_error_unreadable,
    "Biometric authentication failed. Try again or use your password." to R.string.biometric_error_generic,
    "Not recognised. Try again." to R.string.biometric_error_not_recognised,
    "No email/password account is available for fallback." to R.string.biometric_error_no_password_account,
    "Password verification failed. Check your password and try again." to R.string.biometric_error_password_failed,
    "On device" to R.string.chip_on_device,
    "Cloud" to R.string.chip_cloud,
    "Pending sync" to R.string.sync_status_pending,
    "Deleting..." to R.string.sync_status_deleting,
    "Sync failed" to R.string.sync_status_failed,
    "Offline - showing the copy saved on this device." to R.string.offline_cached_copy,
    "Offline - showing saved simulations. Changes sync when you reconnect." to R.string.offline_saved_list,
    "Use your device biometrics to continue." to R.string.lock_message_default,
    "Use your device biometrics to continue, or verify your account password." to R.string.lock_message_or_password,
    "Strong biometrics are currently unavailable. Verify your account password to continue." to R.string.lock_message_password_only
)
