package com.example.lifeapp.data.model

// 1. 天氣警告項目
data class WeatherWarningItem(
    val code: String = "",
    val name: String = "",
    val actionCode: String = ""
)

// 2. 分區氣溫項目
data class DistrictTemperature(
    val place: String = "",
    val value: Int = 0,
    val unit: String = "°C"
)

// 3. 九天天氣預報項目
data class ForecastVal(
    val value: Int = 0,
    val unit: String = "°C"
)

data class ForecastItem(
    val forecastDate: String = "",      // 例如: 20260812
    val week: String = "",              // 例如: 星期三
    val forecastWeather: String = "",   // 天氣描述
    val forecastMaxtemp: ForecastVal = ForecastVal(),
    val forecastMintemp: ForecastVal = ForecastVal()
)

// 4. 天氣頁面總 UI 狀態
data class WeatherUiState(
    val isLoading: Boolean = true,
    val warningSummary: List<WeatherWarningItem> = emptyList(), // 1. 生效中的天氣警告
    val districtTemperatures: List<DistrictTemperature> = emptyList(), // 2. 分區天氣
    val todayForecast: String = "",                             // 3. 今日天氣預報
    val nineDayForecast: List<ForecastItem> = emptyList(),      // 4. 九天天氣預報
    val updateTime: String = "",
    val errorMessage: String? = null
)
