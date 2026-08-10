package com.example.lifeapp.data.model

data class RealtimeWeatherResponse(
    val temperature: StationGroup?,
    val humidity: StationGroup?,
    val uvindex: UvGroup?,
    val warningMessage: List<String>?
)

data class StationGroup(val data: List<StationRecord>?)
data class StationRecord(val place: String, val value: Int, val unit: String)

data class UvGroup(val data: List<UvRecord>?)
data class UvRecord(val value: Float, val desc: String)

data class LocationStation(
    val nameTc: String,
    val nameEn: String,
    val temp: Int?
)

typealias WarningSummaryResponse = Map<String, WarningItem>?
data class WarningItem(val name: String, val code: String, val actionCode: String)

data class ForecastLocalWeatherResponse(
    val generalSituation: String?,
    val forecastDesc: String?,
    val outlook: String?
)

data class NineDayForecastResponse(
    val generalSituation: String?,
    val weatherForecast: List<DayForecast>?
)

data class DayForecast(
    val forecastDate: String,
    val week: String,
    val forecastWind: String,
    val forecastWeather: String,
    val forecastMaxtemp: TempVal?,
    val forecastMintemp: TempVal?,
    val forecastMaxrh: TempVal?,
    val forecastMinrh: TempVal?
)

data class TempVal(val value: Int, val unit: String)

data class FullWeatherUiState(
    val isLoading: Boolean = true,
    val warnings: List<String> = emptyList(),
    val locations: List<LocationStation> = emptyList(),
    val selectedLocation: LocationStation? = null,
    val currentHumidity: String = "--%",
    val currentUv: String = "無數據",
    val todayForecastDesc: String = "載入中...",
    val nineDayForecasts: List<DayForecast> = emptyList(),
    val errorMessage: String? = null
)

val stationNameEnMap = mapOf(
    "香港天文台" to "Hong Kong Observatory",
    "京士柏" to "King's Park",
    "黃竹坑" to "Wong Chuk Hang",
    "打鼓嶺" to "Ta Kwu Ling",
    "流浮山" to "Lau Fau Shan",
    "大埔" to "Tai Po",
    "沙田" to "Sha Tin",
    "屯門" to "Tuen Mun",
    "將軍澳" to "Tseung Kwan O",
    "西貢" to "Sai Kung",
    "長洲" to "Cheung Chau",
    "赤鱲角" to "Chek Lap Kok",
    "青衣" to "Tsing Yi",
    "石崗" to "Shek Kong",
    "荃灣可風中學" to "Tsuen Wan Ho Fung",
    "荃灣城門谷" to "Tsuen Wan Shing Mun Valley",
    "香港公園" to "Hong Kong Park",
    "筲箕灣" to "Shau Kei Wan",
    "九龍城" to "Kowloon City",
    "跑馬地" to "Happy Valley",
    "黃大仙" to "Wong Tai Sin",
    "赤柱" to "Stanley",
    "觀塘" to "Kwun Tong",
    "深水埗" to "Sham Shui Po",
    "大美督" to "Tai Mei Tuk"
)
