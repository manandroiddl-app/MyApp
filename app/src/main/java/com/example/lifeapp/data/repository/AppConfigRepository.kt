package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.AppConfigApiService
import com.example.lifeapp.data.model.AppConfig
import com.example.lifeapp.data.model.GlobalAnnouncement
import com.example.lifeapp.data.model.HubCardConfig
import com.example.lifeapp.data.model.HubConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepository @Inject constructor(
    private val configApiService: AppConfigApiService
) {
    // ⚠️【請務必確認】在 GitHub 網頁上將下面網址替換為你真實的 config.json Raw 網址！
    private val rawConfigUrl = "https://raw.githubusercontent.com/manandroiddl-app/MyApp/refs/heads/main/config.json"

    private val defaultConfig = AppConfig(
        globalAnnouncement = null,
        hubScreen = HubConfig(
            cards = listOf(
                HubCardConfig("weather", "香港天氣", "警告、特別提示、分區氣溫/濕度/UV及預報", "☀️", true, "即時"),
                HubCardConfig("traffic", "交通消息", "特別交通預告及即時路況", "🚗", true, null),
                HubCardConfig("bus", "交通工具到站時間", "九巴 ETA 搜尋與 1 分鐘定時自動更新", "🚌", true, "熱門")
            )
        )
    )

    private val _appConfig = MutableStateFlow(defaultConfig)
    val appConfig: StateFlow<AppConfig> = _appConfig.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    suspend fun loadRemoteConfig() {
        withContext(Dispatchers.IO) {
            _isRefreshing.value = true
            runCatching {
                val cacheBustingUrl = "$rawConfigUrl?t=${System.currentTimeMillis()}"
                val jsonObject = configApiService.getRemoteConfigRaw(cacheBustingUrl)
                val parsedConfig = Gson().fromJson(jsonObject, AppConfig::class.java)

                if (parsedConfig != null) {
                    // 複製物件強制產生新引用，觸發 StateFlow 重新發射
                    _appConfig.value = parsedConfig.copy()
                }
            }.onFailure { e ->
                _appConfig.value = defaultConfig.copy(
                    globalAnnouncement = GlobalAnnouncement(
                        enabled = true,
                        title = "Remote Config 讀取失敗",
                        message = "無法連接 GitHub，請檢查 rawConfigUrl 網址是否正確。錯誤: ${e.localizedMessage}",
                        level = "warning"
                    )
                )
            }
            _isRefreshing.value = false
        }
    }
}
