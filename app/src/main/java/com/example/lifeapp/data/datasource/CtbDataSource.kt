package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.api.CtbApiService
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CtbDataSource @Inject constructor(
    private val ctbApiService: CtbApiService
) {

    /**
     * 獲取所有城巴路線 (自動拆解為去程/回程)
     */
    suspend fun getRoutes(): List<TransitRoute> {
        val response = ctbApiService.getCtbAllRoutes()
        val dtoList = response.data ?: emptyList()

        val result = mutableListOf<TransitRoute>()
        dtoList.forEach { dto ->
            val routeName = dto.route ?: return@forEach

            // Outbound (去程 - bound "O")
            result.add(
                TransitRoute(
                    routeId = "${OperatorCompany.CTB.name}_${routeName}_O_1",
                    routeName = routeName,
                    transitType = TransitType.BUS,
                    company = OperatorCompany.CTB,
                    originZh = dto.origTc ?: "",
                    originEn = dto.origEn,
                    destinationZh = dto.destTc ?: "",
                    destinationEn = dto.destEn,
                    bound = "O",
                    serviceType = "1"
                )
            )

            // Inbound (回程 - bound "I")
            result.add(
                TransitRoute(
                    routeId = "${OperatorCompany.CTB.name}_${routeName}_I_1",
                    routeName = routeName,
                    transitType = TransitType.BUS,
                    company = OperatorCompany.CTB,
                    originZh = dto.destTc ?: "",
                    originEn = dto.destEn,
                    destinationZh = dto.origTc ?: "",
                    destinationEn = dto.origEn,
                    bound = "I",
                    serviceType = "1"
                )
            )
        }
        return result
    }

    /**
     * 獲取指定路線的車站清單
     */
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> = coroutineScope {
        val directionParam = if (bound.equals("I", ignoreCase = true)) "inbound" else "outbound"
        val response = ctbApiService.getCtbRouteStops(
            companyId = "CTB",
            route = route,
            direction = directionParam
        )
        val routeStops = response.data ?: return@coroutineScope emptyList()

        // 併發拉取每個車站的詳細名稱與經緯度資訊
        val deferredStops = routeStops.map { rs ->
            async {
                val stopId = rs.stopId ?: return@async null
                val stopInfoResponse = runCatching { ctbApiService.getCtbStopInfo(stopId) }.getOrNull()
                val stopInfo = stopInfoResponse?.data

                TransitStop(
                    stopId = stopId,
                    sequence = rs.seq ?: 0,
                    nameZh = stopInfo?.nameTc ?: "車站 $stopId",
                    nameEn = stopInfo?.nameEn ?: "Stop $stopId",
                    latitude = stopInfo?.lat?.toDoubleOrNull() ?: 0.0,
                    longitude = stopInfo?.long?.toDoubleOrNull() ?: 0.0
                )
            }
        }
        deferredStops.awaitAll().filterNotNull().sortedBy { it.sequence }
    }

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間
     */
    suspend fun getEta(stopId: String, route: String, serviceType: String): List<TransitEta> {
        val response = ctbApiService.getCtbEta(
            companyId = "CTB",
            stopId = stopId,
            route = route
        )
        val etaDtoList = response.data ?: emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
        }
        val currentTime = System.currentTimeMillis()

        return etaDtoList.mapNotNull { dto ->
            val etaStr = dto.eta ?: return@mapNotNull null
            val etaTime = runCatching { dateFormat.parse(etaStr)?.time }.getOrNull()

            val minutesLeft = if (etaTime != null) {
                val diffMinutes = ((etaTime - currentTime) / (1000 * 60)).toInt()
                if (diffMinutes < 0) 0 else diffMinutes
            } else null

            TransitEta(
                routeName = dto.route ?: route,
                company = OperatorCompany.CTB,
                destinationZh = dto.destTc ?: "",
                etaTimestamp = etaStr,
                remarkZh = dto.rmkTc ?: "",
                minutesLeft = minutesLeft
            )
        }.sortedBy { it.minutesLeft ?: Int.MAX_VALUE }
    }
}
