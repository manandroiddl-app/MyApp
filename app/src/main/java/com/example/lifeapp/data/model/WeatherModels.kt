package com.example.lifeapp.data.model

// 1. 天氣警告項目 (含詳細內文)
data class WeatherWarningItem(
    val code: String = "",
    val name: String = "",
    val details: String = "" // 更詳細的天氣警告內容
)

// 2. 分區天氣項目 (含中英文名、溫度、濕度)
data class DistrictTemperature(
    val placeTc: String = "",
    val placeEn: String = "",
    val tempValue: Int = 0,
    val humidityValue: Int? = null,
    val unit: String = "°C"
)

// 👈 2.1 新增：分區雨量項目
data class DistrictRainfall(
    val placeTc: String = "",
    val placeEn: String = "",
    val max: Int = 0,
    val min: Int = 0,
    val unit: String = "mm"
)

// 3. 紫外線指數
data class UvIndexInfo(
    val value: String = "",
    val desc: String = ""
)

// 4. 九天天氣預報項目
data class ForecastVal(
    val value: Int = 0,
    val unit: String = "°C"
)

data class ForecastItem(
    val forecastDate: String = "",
    val week: String = "",
    val forecastWeather: String = "",
    val forecastMaxtemp: ForecastVal = ForecastVal(),
    val forecastMintemp: ForecastVal = ForecastVal()
)

// 5. 天氣頁面總 UI 狀態
data class WeatherUiState(
    val isLoading: Boolean = true,
    val warningSummary: List<WeatherWarningItem> = emptyList(),
    val districtTemperatures: List<DistrictTemperature> = emptyList(),
    val districtRainfall: List<DistrictRainfall> = emptyList(), // 👈 新增雨量列表
    val uvIndexInfo: UvIndexInfo? = null,
    val todayForecast: String = "",
    val nineDayForecast: List<ForecastItem> = emptyList(),
    val updateTime: String = "",
    val errorMessage: String? = null
)
