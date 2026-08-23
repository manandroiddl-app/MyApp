package com.example.lifeapp.data.repository

import com.example.lifeapp.data.local.dao.TransitBookmarkDao
import com.example.lifeapp.data.local.entity.TransitBookmarkEntity
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BusRepository @Inject constructor(
    private val bookmarkDao: TransitBookmarkDao
) {
    // 車站名稱記憶體快取 (Stop ID -> 繁體中文站名)
    private val stopNameCache = ConcurrentHashMap<String, String>()

    suspend fun getKmbRoutes(): List<TransitRoute> = withContext(Dispatchers.IO) {
        val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route")
        val connection = url.openConnection() as HttpURLConnection
        try {
            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val dataArray = JSONObject(jsonStr).getJSONArray("data")
            val list = mutableListOf<TransitRoute>()
            
            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                list.add(
                    TransitRoute(
                        routeName = obj.optString("route"),
                        bound = obj.optString("bound"),
                        serviceType = obj.optString("service_type"),
                        originZh = obj.optString("orig_tc"),
                        destinationZh = obj.optString("dest_tc")
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

    suspend fun getKmbRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> = withContext(Dispatchers.IO) {
        val boundParam = if (bound == "O") "outbound" else "inbound"
        val url = URL("https://data.etabus.gov.hk/v1/transport/kmb/route-stop/$route/$boundParam/$serviceType")
        val connection = url.openConnection() as HttpURLConnection
        try {
            val jsonStr = connection.inputStream.bufferedReader().use { it.readText() }
            val dataArray = JSONObject(jsonStr).getJSONArray("data")
            val rawStops = mutableListOf<Pair<String, Int>>() // Pair(stopId, seq)

            for (i in 0 until dataArray.length()) {
                val obj = dataArray.getJSONObject(i)
                rawStops.add(Pair(obj.optString("stop"), obj.optInt("seq")))
            }

            // 🎯 修復 4：並行 Fetch 缺失的中文站名，徹底消除「載入中...」
            val missingStopIds = rawStops.map { it.first }.filter { !stopNameCache.containsKey(it) }.distinct()
            if (missingStopIds.isNotEmpty()) {
                missingStopIds.map { stopId ->
                    async {
                        val name = fetchStopNameFromApi(stopId)
                        if (name.isNotEmpty()) {
                            stopNameCache[stopId] = name
                        }
                    }
                }.awaitAll()
            }

            // 組裝帶有繁體中文站名的 TransitStop 列表
            rawStops.map { (stopId, seq) ->
                TransitStop(
                    stopId = stopId,
                    sequence = seq,
                    nameZh = stopNameCache[stopId] ?: "車站 $seq"
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

    suspend fun getKmbEta(stopId: String, route: String, serviceType: String): List<TransitEta> = withContext(Dispatchers.IO) {
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
                val etaTimeStr = obj.optString("eta", "")
                val dir = obj.optString("dir", "")
                val destTc = obj.optString("dest_tc", "")

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
                        dir = dir,
                        serviceType = obj.optString("service_type", "1"),
                        destinationZh = destTc,
                        etaTimestamp = if (etaTimeStr == "null") "" else etaTimeStr,
                        minutesLeft = minsLeft
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

    fun getAllBookmarks(): Flow<List<TransitBookmarkEntity>> = bookmarkDao.getAllBookmarks()
    suspend fun addBookmark(entity: TransitBookmarkEntity) = bookmarkDao.insertBookmark(entity)
    suspend fun removeBookmark(bookmarkId: String) = bookmarkDao.deleteBookmarkById(bookmarkId)
}
