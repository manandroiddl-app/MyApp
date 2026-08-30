package com.example.trafficinquiry.data.repository

import com.example.trafficinquiry.data.model.OperatorCompany
import com.example.trafficinquiry.data.model.TransitEta
import com.example.trafficinquiry.data.model.TransitRoute
import com.example.trafficinquiry.data.model.TransitStop

/**
 * 交通數據源介面 (Strategy Pattern)
 * 定義所有交通機構 (如 KMB, CTB, MTR) 必須實作的數據抓取標準
 */
interface TransitDataSource {
    /** 數據源對應的交通公司類型 */
    val company: OperatorCompany

    /** 獲取該交通機構的所有路線列表 */
    suspend fun getRoutes(): List<TransitRoute>

    /** 獲取指定路線的車站列表 */
    suspend fun getRouteStops(
        routeName: String,
        bound: String,
        serviceType: String
    ): List<TransitStop>

    /** 獲取指定車站與路線的即時到站時間 (ETA) */
    suspend fun getEta(
        stopId: String,
        routeName: String,
        serviceType: String
    ): List<TransitEta>
}
