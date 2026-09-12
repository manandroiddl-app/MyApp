package com.example.lifeapp.data.repository.transit

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.dao.TransitDao
import com.example.lifeapp.data.local.entity.TransitLastUpdateEntity
import com.example.lifeapp.data.repository.transit.fetcher.KmbDataFetcher
import com.example.lifeapp.util.TransitDateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransitSyncManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val transitDao: TransitDao,
    private val kmbDataFetcher: KmbDataFetcher
) {

    companion object {
        private const val TAG = "TransitSyncManager"
    }

    private val workManager = WorkManager.getInstance(context)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val syncMutex = Mutex()

    /**
     * 啟動 WorkManager 每日定期 check version 自動更新任務 (開 App 時註冊)
     */
    fun schedulePeriodicSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<TransitSyncWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            TransitSyncWorker.WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * 檢查版本並在需要時執行 Batch Update (用於開 App 時 auto trigger)
     * @return true 代表執行了更新，false 代表已是最新版本或正在更新中
     */
    suspend fun checkAndAutoSync(): Boolean {
        if (_isSyncing.value) return false

        val targetVersion = TransitDateUtils.calculateVersion()
        val currentUpdateRecord = transitDao.getLastUpdate()

        if (currentUpdateRecord != null && currentUpdateRecord.version == targetVersion) {
            return false // 已是最新版本，跳過更新
        }

        return performBatchSync(targetVersion)
    }

    /**
     * 供 TransitSyncWorker 調用的內部自動檢查方法
     */
    suspend fun checkAndAutoSyncInternal(): Boolean {
        return checkAndAutoSync()
    }

    /**
     * 供 TransitSyncWorker 調用的無條件強制同步方法
     */
    suspend fun performBatchSyncDirectly(): Boolean {
        val targetVersion = TransitDateUtils.calculateVersion()
        return performBatchSync(targetVersion)
    }

    /**
     * 手動觸發 Batch Update (用於 UI 按鈕 trigger)
     * 無條件無視舊版本號，強制進行 Network Fetch 並覆蓋 DB
     * @return true 代表成功更新，false 代表正在更新中或更新失敗
     */
    suspend fun forceSync(): Boolean {
        if (_isSyncing.value) return false
        val targetVersion = TransitDateUtils.calculateVersion()
        return performBatchSync(targetVersion)
    }

    private suspend fun performBatchSync(targetVersion: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext syncMutex.withLock {
            if (_isSyncing.value) return@withLock false
            _isSyncing.value = true

            try {
                // 1. Fetch 各營運商資料 (在進入 DB Transaction 之前執行，確保 Fetch 成功才動 DB)
                val kmbData = kmbDataFetcher.fetchAllKmbData()

                // 安全檢查：若 Fetch 回傳空資料，直接終止，避免誤刪 DB
                if (kmbData.routes.isEmpty() && kmbData.stops.isEmpty()) {
                    Log.e(TAG, "Batch sync failed: Fetched data is empty")
                    return@withLock false
                }

                // 2. 在 Room Coroutine Transaction (withTransaction) 內進行全量寫入與版本記錄，確保原子性
                appDatabase.withTransaction {
                    // 寫入前先清空相關 Table，確保廢棄或舊格式資料不殘留
                    transitDao.clearRoutes()
                    transitDao.clearStops()
                    transitDao.clearRouteStops()

                    // 寫入 Routes, Stops, RouteStops
                    transitDao.insertRoutes(kmbData.routes)
                    transitDao.insertStops(kmbData.stops)
                    transitDao.insertRouteStops(kmbData.routeStops)

                    // 更新版本記錄表
                    val nowMillis = System.currentTimeMillis()
                    val lastUpdateEntity = TransitLastUpdateEntity(
                        id = 1,
                        version = targetVersion,
                        lastUpdateTime = nowMillis
                    )
                    transitDao.insertOrUpdateLastUpdate(lastUpdateEntity)
                }

                true
            } catch (e: Exception) {
                Log.e(TAG, "Batch sync failed", e)
                false
            } finally {
                _isSyncing.value = false
            }
        }
    }
}
