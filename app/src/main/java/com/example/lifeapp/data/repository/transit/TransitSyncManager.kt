package com.example.lifeapp.data.repository.transit

import android.content.Context
import android.util.Log
import androidx.room.withTransaction
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.example.lifeapp.data.local.AppDatabase
import com.example.lifeapp.data.local.dao.TransitDao
import com.example.lifeapp.data.local.entity.TransitLastUpdateEntity
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity
import com.example.lifeapp.data.repository.transit.fetcher.CtbDataFetcher
import com.example.lifeapp.data.repository.transit.fetcher.KmbDataFetcher
import com.example.lifeapp.util.FileLogger
import com.example.lifeapp.util.TransitDateUtils
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
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
    private val kmbDataFetcher: KmbDataFetcher,
    private val ctbDataFetcher: CtbDataFetcher,
    private val fileLogger: FileLogger
) {

    companion object {
        private const val TAG = "TransitSyncManager"
    }

    private val workManager = WorkManager.getInstance(context)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val syncMutex = Mutex()
    private val externalScope = CoroutineScope(Dispatchers.Default)

    init {
        observeWorkManagerStatus()
    }

    /**
     * 監聽 WorkManager 的工作狀態，自動更新 _isSyncing State
     */
    private fun observeWorkManagerStatus() {
        externalScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(TransitSyncWorker.WORK_NAME_ONE_TIME)
                .collect { workInfos ->
                    val isOneTimeRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
                    if (isOneTimeRunning) {
                        _isSyncing.value = true
                    }
                }
        }
        externalScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(TransitSyncWorker.WORK_NAME_PERIODIC)
                .collect { workInfos ->
                    val isPeriodicRunning = workInfos.any { it.state == WorkInfo.State.RUNNING }
                    if (isPeriodicRunning) {
                        _isSyncing.value = true
                    }
                }
        }
    }

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
     * 透過 WorkManager 觸發一次性同步（支援 Foreground Service 防鎖屏中斷）
     */
    fun triggerOneTimeSync(isForceSync: Boolean = false) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putBoolean(TransitSyncWorker.KEY_FORCE_SYNC, isForceSync)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<TransitSyncWorker>()
            .setConstraints(constraints)
            .setInputData(inputData)
            .build()

        // ✅ 已修正：使用正確認稱 ExistingWorkPolicy.REPLACE
        workManager.enqueueUniqueWork(
            TransitSyncWorker.WORK_NAME_ONE_TIME,
            ExistingWorkPolicy.REPLACE,
            oneTimeWorkRequest
        )
    }

    /**
     * 檢查版本並在需要時執行 Batch Update (用於開 App 時 auto trigger)
     * @return true 代表發起了更新檢查或執行中，false 代表已是最新版本
     */
    suspend fun checkAndAutoSync(): Boolean {
        if (_isSyncing.value) return false

        val targetVersion = TransitDateUtils.calculateVersion()
        val currentUpdateRecord = transitDao.getLastUpdate()

        if (currentUpdateRecord != null && currentUpdateRecord.version == targetVersion) {
            return false // 已是最新版本，跳過更新
        }

        triggerOneTimeSync(isForceSync = false)
        return true
    }

    /**
     * 供 TransitSyncWorker 調用的內部自動檢查方法
     */
    suspend fun checkAndAutoSyncInternal(): Boolean {
        val targetVersion = TransitDateUtils.calculateVersion()
        val currentUpdateRecord = transitDao.getLastUpdate()

        if (currentUpdateRecord != null && currentUpdateRecord.version == targetVersion) {
            return true // 已是最新版本，視為成功
        }

        return performBatchSync(targetVersion)
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
     * 無條件無視舊版本號，發起 Foreground Service Worker 進行 Network Fetch 並覆蓋 DB
     * @return true 代表成功發起更新，false 代表正在更新中
     */
    suspend fun forceSync(): Boolean {
        if (_isSyncing.value) return false
        triggerOneTimeSync(isForceSync = true)
        return true
    }

    private suspend fun performBatchSync(targetVersion: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext syncMutex.withLock {
            if (_isSyncing.value) return@withLock false
            _isSyncing.value = true

            fileLogger.clearLog()
            fileLogger.log("=== Starting Batch Sync ===")
            fileLogger.log("Log File Path: ${fileLogger.getLogFilePath()}")

            try {
                val fetchStartTime = System.currentTimeMillis()
                fileLogger.log("Starting Parallel Network Fetch (KMB & CTB)...")

                // 1. 平行 Fetch KMB 與 CTB 資料 (在進入 DB Transaction 前執行)
                val (kmbData, ctbData) = coroutineScope {
                    val kmbDeferred = async {
                        fileLogger.log("KMB Fetch initiated...")
                        val res = kmbDataFetcher.fetchAllKmbData()
                        fileLogger.log("KMB Fetch success -> routes: ${res.routes.size}, stops: ${res.stops.size}, routeStops: ${res.routeStops.size}")
                        res
                    }
                    val ctbDeferred = async {
                        fileLogger.log("CTB Fetch initiated...")
                        val res = ctbDataFetcher.fetchAllCtbData()
                        fileLogger.log("CTB Fetch success -> routes: ${res.routes.size}, stops: ${res.stops.size}, routeStops: ${res.routeStops.size}")
                        res
                    }
                    Pair(kmbDeferred.await(), ctbDeferred.await())
                }

                val fetchDuration = System.currentTimeMillis() - fetchStartTime
                fileLogger.log("Network Fetch completed in ${fetchDuration}ms")

                // 嚴格安全檢查：只要任意一家營運商資料為空，立即終止以防止清理 DB 時遺失資料
                if (kmbData.routes.isEmpty() || ctbData.routes.isEmpty()) {
                    fileLogger.log("ERROR: Batch sync failed - One or more operator data is empty. (KMB: ${kmbData.routes.size}, CTB: ${ctbData.routes.size})")
                    return@withLock false
                }

                // 合併兩家營運商全量資料
                val allRoutes = mutableListOf<TransitRouteEntity>().apply {
                    addAll(kmbData.routes)
                    addAll(ctbData.routes)
                }
                val allStops = mutableListOf<TransitStopEntity>().apply {
                    addAll(kmbData.stops)
                    addAll(ctbData.stops)
                }
                val allRouteStops = mutableListOf<TransitRouteStopEntity>().apply {
                    addAll(kmbData.routeStops)
                    addAll(ctbData.routeStops)
                }

                fileLogger.log("Combined Totals -> routes: ${allRoutes.size}, stops: ${allStops.size}, routeStops: ${allRouteStops.size}")

                // 2. 在 Room Coroutine Transaction (withTransaction) 內進行全量寫入與版本記錄，確保原子性
                val dbStartTime = System.currentTimeMillis()
                fileLogger.log("Starting Room DB Transaction...")

                appDatabase.withTransaction {
                    // 寫入前先清空相關 Table，確保廢棄或舊格式資料不殘留
                    transitDao.clearRoutes()
                    transitDao.clearStops()
                    transitDao.clearRouteStops()

                    // 分批寫入 Routes, Stops, RouteStops (1000 條/批)
                    allRoutes.chunked(1000).forEach { transitDao.insertRoutes(it) }
                    allStops.chunked(1000).forEach { transitDao.insertStops(it) }
                    allRouteStops.chunked(1000).forEach { transitDao.insertRouteStops(it) }

                    // 更新版本記錄表
                    val nowMillis = System.currentTimeMillis()
                    val lastUpdateEntity = TransitLastUpdateEntity(
                        id = 1,
                        version = targetVersion,
                        lastUpdateTime = nowMillis
                    )
                    transitDao.insertOrUpdateLastUpdate(lastUpdateEntity)
                }

                val dbDuration = System.currentTimeMillis() - dbStartTime
                fileLogger.log("Room DB Transaction completed successfully in ${dbDuration}ms!")

                true
            } catch (e: Exception) {
                fileLogger.log("CRITICAL ERROR: Batch sync failed with Exception: ${e.message}\n${e.stackTraceToString()}")
                Log.e(TAG, "Batch sync failed with Exception: ${e.message}", e)
                false
            } finally {
                _isSyncing.value = false
                fileLogger.log("=== Batch Sync Finished ===")
            }
        }
    }
}