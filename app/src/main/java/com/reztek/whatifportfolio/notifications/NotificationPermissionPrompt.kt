package com.reztek.whatifportfolio.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.reztek.whatifportfolio.data.preferences.LocaleHelper
import com.reztek.whatifportfolio.data.preferences.UserPreferencesRepository
import kotlinx.coroutines.launch

private const val TAG = "NotificationPermission"

/**
 * Asks for POST_NOTIFICATIONS once the user is signed in.
 * Settings is not the only place that can show the prompt: a tester who
 * never opens it would otherwise get no notifications on Android 13+.
 */
@Composable
fun NotificationPermissionPrompt() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var signedInUid by remember { mutableStateOf(FirebaseAuth.getInstance().currentUser?.uid) }

    DisposableEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val listener = FirebaseAuth.AuthStateListener { signedInUid = it.currentUser?.uid }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        Log.i(TAG, "POST_NOTIFICATIONS granted=$granted")
        LocaleHelper.markNotificationPermissionAsked(context)
        scope.launch {
            val repository = UserPreferencesRepository(context)
            repository.setNotificationsEnabled(granted)
            LocaleHelper.setNotificationsAllowed(context, granted)
            if (granted) {
                FcmTokenRegistrar.registerCurrentToken()
            } else {
                FcmTokenRegistrar.clearToken()
            }
        }
    }

    LaunchedEffect(signedInUid) {
        if (signedInUid.isNullOrBlank()) return@LaunchedEffect
        if (!LocaleHelper.notificationsAllowed(context)) return@LaunchedEffect
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return@LaunchedEffect
        if (LocaleHelper.notificationPermissionAsked(context)) return@LaunchedEffect
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            LocaleHelper.markNotificationPermissionAsked(context)
            FcmTokenRegistrar.registerCurrentToken()
            return@LaunchedEffect
        }
        Log.i(TAG, "Requesting POST_NOTIFICATIONS for signed-in user")
        LocaleHelper.markNotificationPermissionAsked(context)
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
