package com.example.lifeapp.data.repository

import android.util.Log
import com.example.lifeapp.data.api.AppConfigApiService
import com.example.lifeapp.data.model.AppConfig
import com.example.lifeapp.data.model.GlobalAnnouncement
import com.example.lifeapp.data.model.HubCardConfig
import com.example.lifeapp.data.model.HubConfig
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppConfigRepository @Inject constructor(
    private val configApiService: AppConfigApiService
) {
    // 👈 請填入你自己的 GitHub Raw 網址
    private val rawConfigUrl = "https://raw.githubusercontent.com/manandroiddl-app/MyApp/refs/heads/main/config.json"

    // 本地預設保底設定 (Fallback)，確保無網路時 App 一樣能正常運作
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

    suspend fun loadRemoteConfig() {
        runCatching {
            // 加上時間戳破壞 CDN 快取，確保每次重新整理均獲取最新檔案
            val liveUrl = "$rawConfigUrl?t=${System.currentTimeMillis()}"
            val jsonObject = configApiService.getRemoteConfigRaw(liveUrl)
            val parsedConfig = Gson().fromJson(jsonObject, AppConfig::class.java)

            if (parsedConfig != null) {
                _appConfig.value = parsedConfig
                Log.d("AppConfigRepo", "Remote config loaded successfully")
            }
        }.onFailure { e ->
            Log.e("AppConfigRepo", "Failed to fetch remote config, using defaults", e)
        }
    }
}
