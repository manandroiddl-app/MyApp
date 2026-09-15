package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.api.CtbApiService
import com.example.lifeapp.data.api.CtbRouteDto
import com.example.lifeapp.data.api.CtbRouteStopDto
import com.example.lifeapp.data.api.CtbStopDto
import com.example.lifeapp.data.model.OperatorCompany
import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop
import com.example.lifeapp.data.model.TransitType
import com.example.lifeapp.util.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CtbDataSource @Inject constructor(
    private val ctbApiService: CtbApiService,
    private val fileLogger: FileLogger
) {

    private val semaphore = Semaphore(10)

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
                fileLogger.log("[CTB Retry] Attempt ${attempt + 1} failed: ${e.localizedMessage}. Retrying in ${currentDelay}ms...")
                delay(currentDelay)
                currentDelay *= 2
            }
        }
        return block() // 最後一次嘗試，若仍失敗則直接拋出 Exception 讓外層 runCatching 捕獲
    }

    /**
     * 獲取所有城巴路線 (自動拆解為去程/回程)
     */
    suspend fun getRoutes(): List<TransitRoute> = withContext(Dispatchers.IO) {
        val response = retryApiCall { ctbApiService.getCtbAllRoutes() }
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
        result
    }

    /**
     * 獲取指定路線的車站清單
     */
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop> = withContext(Dispatchers.IO) {
        val directionParam = if (bound.equals("I", ignoreCase = true)) "inbound" else "outbound"
        val response = retryApiCall {
            ctbApiService.getCtbRouteStops(
                companyId = "CTB",
                route = route,
                direction = directionParam
            )
        }
        val routeStops = response.data ?: return@withContext emptyList()

        // 併發拉取每個車站的詳細名稱與經緯度資訊
        coroutineScope {
            val deferredStops = routeStops.map { rs ->
                async {
                    val stopId = rs.stopId ?: return@async null
                    val stopInfoResponse = runCatching {
                        retryApiCall { ctbApiService.getCtbStopInfo(stopId) }
                    }.getOrNull()
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
    }

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間 (支援 bound 方向過濾)
     */
    suspend fun getEta(stopId: String, route: String, serviceType: String, bound: String? = null): List<TransitEta> = withContext(Dispatchers.IO) {
        val response = retryApiCall {
            ctbApiService.getCtbEta(
                companyId = "CTB",
                stopId = stopId,
                route = route
            )
        }
        val etaDtoList = response.data ?: emptyList()

        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("Asia/Hong_Kong")
        }
        val currentTime = System.currentTimeMillis()

        etaDtoList.mapNotNull { dto ->
            // 方向過濾：若指定了 bound，則僅保留 dto.dir 相符的班次 (如 "O" 或 "I")
            if (!bound.isNullOrEmpty() && dto.dir != null && !dto.dir.equals(bound, ignoreCase = true)) {
                return@mapNotNull null
            }

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
                minutesLeft = minutesLeft,
                etaSeq = dto.etaSeq
            )
        }.sortedBy { it.etaSeq ?: Int.MAX_VALUE }
    }

    /**
     * 全量獲取城巴所有路線原始 DTO (Phase 2 Batch Sync 專用)
     */
    suspend fun getAllRoutesRaw(): List<CtbRouteDto> = withContext(Dispatchers.IO) {
        val response = retryApiCall { ctbApiService.getCtbAllRoutes() }
        response.data ?: emptyList()
    }

    /**
     * 併發測試每條路線的 inbound 與 outbound 方向並獲取 RouteStop 列表 (Phase 2 Batch Sync 專用)
     */
    suspend fun getAllRouteStopsParallel(routes: List<CtbRouteDto>): List<CtbRouteStopDto> = coroutineScope {
        val directions = listOf("inbound", "outbound")
        val deferredList = routes.flatMap { routeDto ->
            val routeName = routeDto.route ?: return@flatMap emptyList()
            directions.map { dir ->
                async {
                    semaphore.withPermit {
                        runCatching {
                            retryApiCall {
                                val response = ctbApiService.getCtbRouteStops("CTB", routeName, dir)
                                response.data ?: emptyList()
                            }
                        }.onFailure { ex ->
                            fileLogger.log("[CTB Error] Failed to fetch RouteStop for route: $routeName, dir: $dir -> ${ex.localizedMessage}")
                        }.getOrDefault(emptyList())
                    }
                }
            }
        }
        deferredList.awaitAll().flatten()
    }

    /**
     * 傳入去重後的 stop_id 集合，併發撈取車站詳細座標與名稱 (Phase 2 Batch Sync 專用)
     */
    suspend fun getStopsParallel(stopIds: Set<String>): List<CtbStopDto> = coroutineScope {
        val deferredList = stopIds.map { stopId ->
            async {
                semaphore.withPermit {
                    runCatching {
                        retryApiCall {
                            val response = ctbApiService.getCtbStopInfo(stopId)
                            response.data
                        }
                    }.onFailure { ex ->
                        fileLogger.log("[CTB Error] Failed to fetch Stop Info for stopId: $stopId -> ${ex.localizedMessage}")
                    }.getOrNull()
                }
            }
        }
        deferredList.awaitAll().filterNotNull()
    }
}