package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop

/**
 * 交通數據源統一介面 (Strategy Pattern)
 * 提供異構交通數據 (九巴、城巴等) 的統一存取標準
 */
interface BusDataSource {
    
    /**
     * 獲取該營運商的所有路線清單
     */
    suspend fun getRoutes(): List<TransitRoute>

    /**
     * 獲取指定路線與方向的車站清單
     *
     * @param route 路線名稱 (例如 "1A")
     * @param bound 方向 ("outbound" / "inbound" 或 "O" / "I")
     * @param serviceType 服務類型 (例如 "1")
     */
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop>

    /**
     * 獲取指定車站與路線的實時 ETA 到站時間
     *
     * @param stopId 車站 ID
     * @param route 路線名稱
     * @param serviceType 服務類型
     */
    suspend fun getEta(stopId: String, route: String, serviceType: String): List<TransitEta>
}
