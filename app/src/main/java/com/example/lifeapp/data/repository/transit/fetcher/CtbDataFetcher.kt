package com.example.lifeapp.data.repository.transit.fetcher

import android.util.Log
import com.example.lifeapp.data.datasource.CtbDataSource
import com.example.lifeapp.data.local.entity.TransitRouteEntity
import com.example.lifeapp.data.local.entity.TransitRouteStopEntity
import com.example.lifeapp.data.local.entity.TransitStopEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

data class CtbDataBatchResult(
    val routes: List<TransitRouteEntity>,
    val stops: List<TransitStopEntity>,
    val routeStops: List<TransitRouteStopEntity>
)

@Singleton
class CtbDataFetcher @Inject constructor(
    private val ctbDataSource: CtbDataSource
) {

    /**
     * 從 CtbDataSource 獲取並轉換城巴 (CTB) 的全量路線、車站與關聯資料
     */
    suspend fun fetchAllCtbData(): CtbDataBatchResult = withContext(Dispatchers.IO) {
        // 1. 撈取全量 Route DTO 列表
        val rawRoutes = ctbDataSource.getAllRoutesRaw()
        Log.d("CtbDataFetcher", "Fetched rawRoutes count: ${rawRoutes.size}")

        // 2. 併發拉取所有 Route-Stops (已包含 bound "I" / "O" 的測試與篩選)
        val rawRouteStops = ctbDataSource.getAllRouteStopsParallel(rawRoutes)
        Log.d("CtbDataFetcher", "Fetched rawRouteStops count: ${rawRouteStops.size}")

        // 3. 記憶體去重: 萃取所有唯一的 stop_id 集合
        val uniqueStopIds = rawRouteStops.mapNotNull { it.stopId }.toSet()
        Log.d("CtbDataFetcher", "Unique stopIds count: ${uniqueStopIds.size}")

        // 4. 併發撈取去重後的 Stop 座標與名稱
        val rawStops = ctbDataSource.getStopsParallel(uniqueStopIds)
        Log.d("CtbDataFetcher", "Fetched rawStops details count: ${rawStops.size}")

        // 5. 建立 RouteStop Entities
        val routeStopEntities = rawRouteStops.mapNotNull { rs ->
            val routeName = rs.route ?: return@mapNotNull null
            val bound = rs.dir ?: return@mapNotNull null
            val seq = rs.seq ?: return@mapNotNull null
            val stopId = rs.stopId ?: return@mapNotNull null

            TransitRouteStopEntity(
                co = "CTB",
                routeName = routeName,
                bound = bound,
                otherKey = "1",
                seq = seq,
                stopId = stopId
            )
        }

        // 6. 建立 Valid Bounds Set (從 RouteStops 獲取所有實際存在的 route + bound 組合)
        val validRouteBoundSet = routeStopEntities
            .map { Pair(it.routeName, it.bound) }
            .toSet()

        // 7. 建立 Route Entities (依據 Mapping 規則)
        val rawRouteMap = rawRoutes.filter { !it.route.isNullOrEmpty() }.associateBy { it.route!! }
        val routeEntities = mutableListOf<TransitRouteEntity>()

        validRouteBoundSet.forEach { (routeName, bound) ->
            val rawRoute = rawRouteMap[routeName] ?: return@forEach
            val isInbound = bound.equals("I", ignoreCase = true)

            // inbound 時起終點對調
            val oriTc = if (isInbound) rawRoute.destTc else rawRoute.origTc
            val oriEng = if (isInbound) rawRoute.destEn else rawRoute.origEn
            val destTc = if (isInbound) rawRoute.origTc else rawRoute.destTc
            val destEng = if (isInbound) rawRoute.origEn else rawRoute.destEn

            routeEntities.add(
                TransitRouteEntity(
                    co = "CTB",
                    coTc = "城巴",
                    routeName = routeName,
                    bound = bound,
                    otherKey = "1",
                    otherKeyDesc = "default",
                    boundDesc = if (isInbound) "inbound" else "outbound",
                    oriTc = oriTc,
                    oriEng = oriEng,
                    destTc = destTc,
                    destEng = destEng
                )
            )
        }

        // 8. 建立 Stop Entities (依據 Mapping 規則)
        val stopEntities = rawStops.mapNotNull { stop ->
            val stopId = stop.stopId ?: return@mapNotNull null
            TransitStopEntity(
                co = "CTB",
                stopId = stopId,
                nameTc = stop.nameTc,
                nameEn = stop.nameEn,
                lat = stop.lat?.toDoubleOrNull() ?: 0.0,
                lng = stop.long?.toDoubleOrNull() ?: 0.0
            )
        }

        CtbDataBatchResult(
            routes = routeEntities,
            stops = stopEntities,
            routeStops = routeStopEntities
        )
    }
}