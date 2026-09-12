package com.example.lifeapp.data.repository.transit

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TransitSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transitSyncManager: TransitSyncManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_FORCE_SYNC = "KEY_FORCE_SYNC"
        const val WORK_NAME_ONE_TIME = "TransitSyncWorker_OneTime"
        const val WORK_NAME_PERIODIC = "TransitSyncWorker_Periodic"
    }

    override suspend fun doWork(): Result {
        val isForceSync = inputData.getBoolean(KEY_FORCE_SYNC, false)

        val success = if (isForceSync) {
            transitSyncManager.performBatchSyncDirectly()
        } else {
            transitSyncManager.checkAndAutoSyncInternal()
        }

        return if (success) {
            Result.success()
        } else {
            Result.retry()
        }
    }
}
