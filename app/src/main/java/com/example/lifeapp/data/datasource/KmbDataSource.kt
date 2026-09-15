package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import com.example.lifeapp.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

data class KmbRawStop(
    val stopId: String,
    val nameZh: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double
)

data class KmbRawRouteStop(
    val route: String,
    val bound: String,
    val serviceType: String,
    val seq: Int,
    val stopId: String
)

private data class KmbStopDetailCache(
    val nameZh: String,
    val nameEn: String,
    val lat: Double,
    val lng: Double
)

@Singleton
class KmbDataSource @Inject constructor(
    private val fileLogger: FileLogger
) : BusDataSource {

    private val stopDetailCache = ConcurrentHashMap<String, KmbStopDetailCache>()

    /**
     * 通用 API 重試機制 (防 DNS 抖動、Timeout 等瞬時網絡錯誤)
     */
    private suspend fun <T> retryApiCall(
        times: Int = 3,
        initialDelayMs: Long = 500,
        block: suspend () -> T
    ): T {
        var currentDelay = initialDelayMs
        repeat(times - 1) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                fileLogger.log("[KMB Retry] Attempt ${attempt + 1} failed: ${e.localizedMessage}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block()
    }

    /**
     * 全量取得九巴所有車站詳情 (全量 3-Endpoint 方案)
     */
    suspend fun getAllStops(): List<KmbRawStop> = withContext(Dispatchers.IO) {
        retryApiCall {
            val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/stop")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val dataArray = JSONObject(jsonStr).getJSONArray("data")
                val list = mutableListOf<KmbRawStop>()

                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val stopId = obj.optString("stop")
                    val nameZh = obj.optString("name_tc")
                    val nameEn = obj.optString("name_en")
                    val lat = obj.optString("lat").toDoubleOrNull() ?: 0.0
                    val lng = obj.optString("long").toDoubleOrNull() ?: 0.0

                    list.add(
                        KmbRawStop(
                            stopId = stopId,
                            nameZh = nameZh,
                            nameEn = nameEn,
                            lat = lat,
                            lng = lng
                        )
                    )
                }
                list
            } catch (e: Exception) {
                fileLogger.log("[KMB Error] getAllStops Exception: ${e.localizedMessage}")
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    /**
     * 全量取得九巴所有路線與車站的對照關係 (全量 3-Endpoint 方案)
     */
    suspend fun getAllRouteStops(): List<KmbRawRouteStop> = withContext(Dispatchers.IO) {
        retryApiCall {
            val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route-stop")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val dataArray = JSONObject(jsonStr).getJSONArray("data")
                val list = mutableListOf<KmbRawRouteStop>()

                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val route = obj.optString("route")
                    val rawBound = obj.optString("bound")
                    val bound = if (rawBound.equals("outbound", ignoreCase = true)) "O" else if (rawBound.equals("inbound", ignoreCase = true)) "I" else rawBound
                    val serviceType = obj.optString("service_type", "1")
                    val seq = obj.optInt("seq")
                    val stopId = obj.optString("stop")

                    list.add(
                        KmbRawRouteStop(
                            route = route,
                            bound = bound,
                            serviceType = serviceType,
                            seq = seq,
                            stopId = stopId
                        )
                    )
                }
                list
            } catch (e: Exception) {
                fileLogger.log("[KMB Error] getAllRouteStops Exception: ${e.localizedMessage}")
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun getRoutes(): List<TransitRoute> = withContext(Dispatchers.IO) {
        retryApiCall {
            val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route")
            val connection = url.openConnection() as HttpURLConnection
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            try {
                val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
                val dataArray = JSONObject(jsonStr).getJSONArray("data")
                val list = mutableListOf<TransitRoute>()

                for (i in 0 until dataArray.length()) {
                    val obj = dataArray.getJSONObject(i)
                    val routeName = obj.optString("route")
                    val bound = obj.optString("bound")
                    val serviceType = obj.optString("service_type", "1")

                    list.add(
                        TransitRoute(
                            routeId = "KMB_${routeName}_${bound}_${serviceType}",
                            routeName = routeName,
                            transitType = TransitType.BUS,
                            company = OperatorCompany.KMB,
                            bound = bound,
                            serviceType = serviceType,
                            originZh = obj.optString("orig_tc"),
                            originEn = obj.optString("orig_en"),
                            destinationZh = obj.optString("dest_tc"),
                            destinationEn = obj.optString("dest_en")
                        )
                    )
                }
                list
            } catch (e: Exception) {
                fileLogger.log("[KMB Error] getRoutes Exception: ${e.localizedMessage}")
                throw e
            } finally {
                connection.disconnect()
            }
        }
    }

    override suspend fun getRouteStops(
        route: String,
        bound: String,
        serviceType: String
    ): List<TransitStop> = withContext(Dispatchers.IO) {
        val boundParam = if (bound == "O") "outbound" else "inbound"
        val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route-stop/$route/$boundParam/$serviceType")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        try {
            val jsonStr = retryApiCall {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            val dataArray = JSONObject(jsonStr).getJSONArray("data")
            val rawStops = mutableListOf<Pair<String, Int>>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                rawStops.add(Pair(obj.optString("stop"), obj.optInt("seq")))
            }

            val missingStopIds = rawStops.map { it.first }.filter { !stopDetailCache.containsKey("KMB_${it}") }.distinct()
            if (missingStopIds.isNotEmpty()) {
                missingStopIds.map { stopId ->
                    async {
                        val detail = fetchStopDetailFromApi(stopId)
                        stopDetailCache["KMB_${stopId}"] = detail
                    }
                }.awaitAll()
            }

            rawStops.map { (stopId, seq) ->
                val cachedDetail = stopDetailCache["KMB_${stopId}"]
                TransitStop(
                    stopId = stopId,
                    sequence = seq,
                    nameZh = cachedDetail?.nameZh ?: "車站 $seq",
                    nameEn = cachedDetail?.nameEn ?: "",
                    latitude = cachedDetail?.lat ?: 0.0,
                    longitude = cachedDetail?.lng ?: 0.0
                )
            }
        } catch (e: Exception) {
            fileLogger.log("[KMB Error] getRouteStops Exception: ${e.localizedMessage}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private suspend fun fetchStopDetailFromApi(stopId: String): KmbStopDetailCache {
        return try {
            retryApiCall {
                val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/stop/$stopId")
                val conn = url.openConnection() as HttpURLConnection
                conn.connectTimeout = 15000
                conn.readTimeout = 15000
                val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
                conn.disconnect()
                val dataObj = JSONObject(jsonStr).getJSONObject("data")
                KmbStopDetailCache(
                    nameZh = dataObj.optString("name_tc"),
                    nameEn = dataObj.optString("name_en"),
                    lat = dataObj.optString("lat").toDoubleOrNull() ?: 0.0,
                    lng = dataObj.optString("long").toDoubleOrNull() ?: 0.0
                )
            }
        } catch (e: Exception) {
            fileLogger.log("[KMB Error] fetchStopDetailFromApi Exception for stopId $stopId: ${e.localizedMessage}")
            KmbStopDetailCache(
                nameZh = "",
                nameEn = "",
                lat = 0.0,
                lng = 0.0
            )
        }
    }

    override suspend fun getEta(
        stopId: String,
        route: String,
        serviceType: String,
        bound: String?
    ): List<TransitEta> = withContext(Dispatchers.IO) {
        val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/eta/$stopId/$route/$serviceType")
        val connection = url.openConnection() as HttpURLConnection
        connection.connectTimeout = 15000
        connection.readTimeout = 15000
        try {
            val jsonStr = retryApiCall {
                connection.inputStream.bufferedReader().use { it.readText() }
            }
            val dataArray = JSONObject(jsonStr).getJSONArray("data")
            val list = mutableListOf<TransitEta>()
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault())
            val nowMs = System.currentTimeMillis()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                val dir = obj.optString("dir", "")
                
                // 若有帶入 bound 參數（"I" 或 "O"），則進行方向過濾，排除反方向的過多 ETA
                if (!bound.isNullOrEmpty() && dir.isNotEmpty() && !dir.equals(bound, ignoreCase = true)) {
                    continue
                }

                val etaTimeStr = obj.optString("eta", "")
                val destTc = obj.optString("dest_tc", "")
                val rName = obj.optString("route", route)
                val rkZh = obj.optString("rmk_tc", "")
                val etaSeq = if (obj.has("eta_seq") && !obj.isNull("eta_seq")) obj.optInt("eta_seq") else null

                var minsLeft: Int? = null
                if (etaTimeStr.isNotEmpty() && etaTimeStr != "null") {
                    try {
                        val date = sdf.parse(etaTimeStr)
                        if (date != null) {
                            val diffMs = date.time - nowMs
                            minsLeft = (diffMs / 60000).toInt()
                        }
                    } catch (_: Exception) {}
                }

                list.add(
                    TransitEta(
                        routeName = rName,
                        company = OperatorCompany.KMB,
                        destinationZh = destTc,
                        etaTimestamp = if (etaTimeStr == "null") "" else etaTimeStr,
                        minutesLeft = minsLeft,
                        remarkZh = rkZh,
                        etaSeq = etaSeq
                    )
                )
            }
            list.sortedBy { it.etaSeq ?: Int.MAX_VALUE }
        } catch (e: Exception) {
            fileLogger.log("[KMB Error] getEta Exception: ${e.localizedMessage}")
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}