package com.reztek.whatifportfolio

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

private const val TAG = "WhatIfApplication"

/**
 * Application entry point. Initialises Firebase once for the process and
 * logs the app-level lifecycle event, per the requirement to demonstrate
 * "a clear programmatic understanding of your application's lifecycle."
 */
class WhatIfApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "Application onCreate")
        FirebaseApp.initializeApp(this)
    }
}
