package com.example.lifeapp.ui.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * 通用自動輪詢委託元件，負責定時觸發靜默更新與管理 Timer 生命週期
 */
class AutoRefreshDelegate(
    private val scope: CoroutineScope,
    private val intervalMillis: Long = 60000L, // 預設 1 分鐘（60 秒）
    private val onRefresh: () -> Unit
) {
    private var job: Job? = null

    fun start() {
        stop()
        job = scope.launch {
            while (isActive) {
                delay(intervalMillis)
                onRefresh()
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
