package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KmbDataSource @Inject constructor() : BusDataSource {

    private val stopNameCache = ConcurrentHashMap<String, String>()

    override suspend fun getRoutes(): List<TransitRoute> = withContext(Dispatchers.IO) {
        val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route")
        val connection = url.openConnection() as HttpURLConnection
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
            emptyList()
        } finally {
            connection.disconnect()
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
        try {
            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val dataArray = JSONObject(jsonStr).getJSONArray("data")
            val rawStops = mutableListOf<Pair<String, Int>>()

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                rawStops.add(Pair(obj.optString("stop"), obj.optInt("seq")))
            }

            val missingStopIds = rawStops.map { it.first }.filter { !stopNameCache.containsKey("KMB_${it}") }.distinct()
            if (missingStopIds.isNotEmpty()) {
                missingStopIds.map { stopId ->
                    async {
                        val name = fetchStopNameFromApi(stopId)
                        if (name.isNotEmpty()) {
                            stopNameCache["KMB_${stopId}"] = name
                        }
                    }
                }.awaitAll()
            }

            rawStops.map { (stopId, seq) ->
                TransitStop(
                    stopId = stopId,
                    sequence = seq,
                    nameZh = stopNameCache["KMB_${stopId}"] ?: "車站 $seq",
                    nameEn = "",
                    latitude = 0.0,
                    longitude = 0.0
                )
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchStopNameFromApi(stopId: String): String {
        return try {
            val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/stop/$stopId")
            val conn = url.openConnection() as HttpURLConnection
            val jsonStr = conn.inputStream.bufferedReader().use { it.readText() }
            conn.disconnect()
            val dataObj = JSONObject(jsonStr).getJSONObject("data")
            dataObj.optString("name_tc")
        } catch (e: Exception) {
            ""
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
        try {
            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
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
                        remarkZh = rkZh
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection.disconnect()
        }
    }
}
