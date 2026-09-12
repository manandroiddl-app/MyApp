package com.example.lifeapp.data.repository.transit.fetcher

import com.example.lifeapp.data.datasource.KmbDataSource
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
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
     * 從 KmbDataSource 獲取並轉換 KMB 的全量路線、車站與關聯資料 (3-Endpoint 全量平行下載)
     */
    suspend fun fetchAllKmbData(): KmbDataBatchResult = withContext(Dispatchers.IO) {
        coroutineScope {
            // 1. 平行發起 3 個全量 API 下載
            val routesDeferred = async { kmbDataSource.getRoutes() }
            val stopsDeferred = async { kmbDataSource.getAllStops() }
            val routeStopsDeferred = async { kmbDataSource.getAllRouteStops() }

            val rawRoutes = routesDeferred.await()
            val rawStops = stopsDeferred.await()
            val rawRouteStops = routeStopsDeferred.await()

            // 2. 轉換 Routes
            val routeEntities = rawRoutes.map { raw ->
                val currentBound = raw.bound ?: "O"
                TransitRouteEntity(
                    co = "KMB",
                    routeName = raw.routeName,
                    bound = currentBound,
                    boundDesc = if (currentBound.equals("I", ignoreCase = true)) "inbound" else "outbound",
                    otherKey = raw.serviceType ?: "1",
                    otherKeyDesc = "service_type",
                    oriTc = raw.originZh,
                    oriEng = raw.originEn,
                    destTc = raw.destinationZh,
                    destEng = raw.destinationEn
                )
            }

            // 3. 轉換 Stops
            val stopEntities = rawStops.map { stop ->
                TransitStopEntity(
                    co = "KMB",
                    stopId = stop.stopId,
                    nameTc = stop.nameZh,
                    nameEn = stop.nameEn,
                    lat = stop.lat,
                    lng = stop.lng
                )
            }

            // 4. 轉換 RouteStops
            val routeStopEntities = rawRouteStops.map { rs ->
                TransitRouteStopEntity(
                    co = "KMB",
                    routeName = rs.route,
                    bound = rs.bound,
                    otherKey = rs.serviceType,
                    seq = rs.seq,
                    stopId = rs.stopId
                )
            }

            KmbDataBatchResult(
                routes = routeEntities,
                stops = stopEntities,
                routeStops = routeStopEntities
            )
        }
    }
}
