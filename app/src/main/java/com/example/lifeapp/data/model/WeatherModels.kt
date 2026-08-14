package com.example.lifeapp.data.model

// 1. UI 狀態模型
data class WeatherUiState(
    val isLoading: Boolean = true,
    val updateTime: String = "",
    val todayForecast: String = "",
    val generalSituation: String = "",
    val outlook: String = "",
    val tcInfo: String = "",
    val fireDangerWarning: String = "",
    val warningSummary: List<WeatherWarningItem> = emptyList(),
    val districtTemperatures: List<DistrictTemperature> = emptyList(),
    val districtRainfall: List<DistrictRainfall> = emptyList(),
    val uvIndexInfo: UvIndexInfo? = null,
    val nineDayForecast: List<NineDayForecastItem> = emptyList(),
    val errorMessage: String? = null
)

// 2. 天氣警告項目
data class WeatherWarningItem(
    val code: String = "",
    val name: String = "",
    val details: String = ""
) {
    // 是否為預警八號
    fun isTcPre8(): Boolean = code == "WTCPRE8"

    // 取得警告圖示 URL
    fun getIconUrl(): String? {
        val iconName = when (code) {
            "WFROST" -> "frost"
            "WHOT" -> "vhot"
            "WCOLD" -> "cold"
            "WMSGNL" -> "sms"
            "WFNTSA" -> "ntfl"
            "WL" -> "landslip"
            "WTMW" -> "tsunami-warn"
            "WTS" -> "ts"
            "WFIREY" -> "firey"
            "WFIRER" -> "firer"
            "TC1" -> "tc1"
            "TC3" -> "tc3"
            "TC8NE" -> "tc8ne"
            "TC8SE" -> "tc8b"
            "TC8SW" -> "tc8c"
            "TC8NW" -> "tc8d"
            "TC9" -> "tc9"
            "TC10" -> "tc10"
            "WRAINA" -> "raina"
            "WRAINR" -> "rainr"
            "WRAINB" -> "rainb"
            "WTCPRE8" -> "tc8ne" // 特別預警使用 8 號球圖示替代
            else -> null
        }
        return iconName?.let { "https://www.hko.gov.hk/tc/wxinfo/dailywx/images/$it.gif" }
    }
}

// 3. 分區氣溫
data class DistrictTemperature(
    val placeTc: String = "",
    val placeEn: String = "",
    val tempValue: Int = 0,
    val unit: String = "°C",
    val humidityValue: Int? = null
)

// 4. 分區雨量
data class DistrictRainfall(
    val placeTc: String = "",
    val placeEn: String = "",
    val min: Int = 0,
    val max: Int = 0,
    val unit: String = "mm"
)

// 5. 紫外線指數
data class UvIndexInfo(
    val value: String = "0",
    val desc: String = "低"
)

// 6. 九天天氣預報項目
data class NineDayForecastItem(
    val forecastDate: String = "",
    val week: String = "",
    val forecastWeather: String = "",
    val forecastMaxtemp: ForecastVal = ForecastVal(),
    val forecastMintemp: ForecastVal = ForecastVal(),
    val forecastRh: ForecastRhRange = ForecastRhRange(),
    val psr: String = "",
    val wind: String = "",
    val iconCode: Int = 0
)

data class ForecastVal(
    val value: Int = 0,
    val unit: String = "°C"
)

data class ForecastRhRange(
    val minrh: Int = 0,
    val maxrh: Int = 0,
    val unit: String = "%"
)
