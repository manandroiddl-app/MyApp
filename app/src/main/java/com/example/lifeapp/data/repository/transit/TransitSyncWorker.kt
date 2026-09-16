package com.example.lifeapp.data.repository.transit

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class TransitSyncWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val transitSyncManager: TransitSyncManager
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_FORCE_SYNC = "KEY_FORCE_SYNC"
        const val WORK_NAME_ONE_TIME = "TransitSyncWorker_OneTime"
        const val WORK_NAME_PERIODIC = "TransitSyncWorker_Periodic"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "transit_sync_channel"
    }

    override suspend fun doWork(): Result {
        // 在執行任務前，將 Worker 提升為 Foreground Service 以防鎖屏斷網/凍結
        try {
            setForeground(createForegroundInfo())
        } catch (e: Exception) {
            // 在某些極端情況下（例如背景限制），setForeground 可能拋出 Exception
        }

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

    private fun createForegroundInfo(): ForegroundInfo {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setContentTitle("交通數據同步中")
            .setContentText("正在更新巴士路線與車站資料，請稍候...")
            .setSmallIcon(android.R.drawable.ic_popup_sync)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "交通數據同步",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "用於交通數據背景同步時保持網絡連線"
            }
            val notificationManager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}