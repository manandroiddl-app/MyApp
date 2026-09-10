package com.example.lifeapp.data.repository.transit.fetcher

import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity
import javax.inject.Inject
import javax.inject.Singleton

data class KmbDataBatchResult(
    val routes: List<TransitRouteEntity>,
    val stops: List<TransitStopEntity>,
    val routeStops: List<TransitRouteStopEntity>
)

@Singleton
class KmbDataFetcher @Inject constructor(
    private val kmbDataSource: KmbDataSource
) {

    /**
     * 從 KmbDataSource 獲取並轉換 KMB 的全量路線、車站與關聯資料
     */
    suspend fun fetchAllKmbData(): KmbDataBatchResult {
        // 1. 抓取全量 Route
        val rawRoutes = kmbDataSource.getRoutes()
        val routeEntities = rawRoutes.map { raw ->
            TransitRouteEntity(
                co = "KMB",
                routeName = raw.routeName,
                bound = raw.bound ?: "O",
                boundDesc = if ((raw.bound ?: "O").equals("I", ignoreCase = true)) "inbound" else "outbound",
                otherKey = raw.serviceType ?: "1",
                otherKeyDesc = "service_type",
                oriTc = raw.originZh,
                oriEng = raw.originEn,
                destTc = raw.destinationZh,
                destEng = raw.destinationEn
            )
        }

        // 2 & 3. 透過路線疊代抓取對應車站與 RouteStop 關聯
        val stopMap = mutableMapOf<String, TransitStopEntity>()
        val routeStopEntities = mutableListOf<TransitRouteStopEntity>()

        for (route in rawRoutes) {
            val stops = kmbDataSource.getRouteStops(
                route = route.routeName,
                bound = route.bound ?: "O",
                serviceType = route.serviceType ?: "1"
            )

            for (stop in stops) {
                if (!stopMap.containsKey(stop.stopId)) {
                    stopMap[stop.stopId] = TransitStopEntity(
                        co = "KMB",
                        stopId = stop.stopId,
                        nameTc = stop.nameZh,
                        nameEn = stop.nameEn,
                        lat = stop.latitude,
                        lng = stop.longitude
                    )
                }

                routeStopEntities.add(
                    TransitRouteStopEntity(
                        co = "KMB",
                        routeName = route.routeName,
                        bound = route.bound ?: "O",
                        otherKey = route.serviceType ?: "1",
                        seq = stop.sequence,
                        stopId = stop.stopId
                    )
                )
            }
        }

        return KmbDataBatchResult(
            routes = routeEntities,
            stops = stopMap.values.toList(),
            routeStops = routeStopEntities
        )
    }
}
