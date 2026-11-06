package com.reztek.whatifportfolio.data.sync

import android.content.Context
import android.util.Log
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit

private const val TAG = "SimulationSyncWorker"
private const val MAX_RUN_ATTEMPTS = 5

/**
 * Background job that pushes queued offline changes to the REST API.
 * WorkManager only starts it when the device has a network connection, retries
 * with exponential backoff, and survives app restarts.
 *
 * PART3-ASH: offline mode with sync.
 */
class SimulationSyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "Sync work started (attempt ${runAttemptCount + 1})")
        return try {
            val repository = OfflineSimulationRepository.create(applicationContext)
            when (repository.syncPending()) {
                SyncOutcome.DONE -> Result.success()
                SyncOutcome.RETRY -> retryOrGiveUp()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sync work crashed", e)
            retryOrGiveUp()
        }
    }

    // Remaining rows stay queued (or are marked failed) and are picked up on the
    // next app launch, connectivity change or manual "Sync now".
    private fun retryOrGiveUp(): Result =
        if (runAttemptCount + 1 >= MAX_RUN_ATTEMPTS) Result.failure() else Result.retry()
}

/** Enqueues a (network-constrained) sync run; safe to call as often as needed. */
object SyncScheduler {
    private const val UNIQUE_NAME = "simulation-sync"

    fun enqueue(context: Context) {
        val request = OneTimeWorkRequestBuilder<SimulationSyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .build()
        // APPEND_OR_REPLACE: a change made while a run is in progress gets its own follow-up run.
        WorkManager.getInstance(context.applicationContext)
            .enqueueUniqueWork(UNIQUE_NAME, ExistingWorkPolicy.APPEND_OR_REPLACE, request)
        Log.d(TAG, "Sync work enqueued")
    }
}
