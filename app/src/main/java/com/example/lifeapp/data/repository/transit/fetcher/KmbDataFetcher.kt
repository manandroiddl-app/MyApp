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
                routeName = raw.route,
                bound = raw.bound,
                boundDesc = if (raw.bound.equals("I", ignoreCase = true)) "inbound" else "outbound",
                otherKey = raw.serviceType ?: "1",
                otherKeyDesc = "service_type",
                oriTc = raw.origTc,
                oriEng = raw.origEn,
                destTc = raw.destTc,
                destEng = raw.destEn
            )
        }

        // 2. 抓取全量 Stop
        val rawStops = kmbDataSource.getStops()
        val stopEntities = rawStops.map { raw ->
            TransitStopEntity(
                co = "KMB",
                stopId = raw.stop,
                nameTc = raw.nameTc,
                nameEn = raw.nameEn,
                lat = raw.lat?.toDoubleOrNull() ?: 0.0,
                lng = raw.long?.toDoubleOrNull() ?: 0.0
            )
        }

        // 3. 抓取全量 Route-Stop 關聯
        val rawRouteStops = kmbDataSource.getRouteStops()
        val routeStopEntities = rawRouteStops.map { raw ->
            TransitRouteStopEntity(
                co = "KMB",
                routeName = raw.route,
                bound = raw.bound,
                otherKey = raw.serviceType ?: "1",
                seq = raw.seq,
                stopId = raw.stop
            )
        }

        return KmbDataBatchResult(
            routes = routeEntities,
            stops = stopEntities,
            routeStops = routeStopEntities
        )
    }
}
