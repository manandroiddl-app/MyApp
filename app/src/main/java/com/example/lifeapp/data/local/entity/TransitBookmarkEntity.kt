package com.example.lifeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 跨交通工具通用書籤 Entity
 *
 * PrimaryKey 範例:
 *  - 九巴: "KMB_1A_O_1_BS001"
 *  - 城巴: "CTB_720_O_1_001000"
 */
@Entity(tableName = "transit_bookmarks")
data class TransitBookmarkEntity(
    @PrimaryKey
    val bookmarkId: String,          // 格式: `${company}_${routeName}_${bound}_${serviceType}_${stopId}`
    val routeName: String,           // 例: "1A"
    val company: String,             // 例: "KMB", "CTB", "NWFB"
    val bound: String,               // "O" (Outbound) / "I" (Inbound)
    val serviceType: String,         // 例: "1"
    val originZh: String,            // 起點 (中文)
    val destinationZh: String,       // 終點 (中文)
    val stopId: String,              // 車站 ID
    val stopNameZh: String,          // 車站名稱 (中文)
    val sequence: Int                // 車站順序
) {
    companion object {
        /**
         * 統一的 Bookmark ID 動態生成器，防止全域寫死 "KMB_" 前綴
         */
        fun generateBookmarkId(
            company: String,
            routeName: String,
            bound: String?,
            serviceType: String?,
            stopId: String
        ): String {
            val safeBound = bound ?: "O"
            val safeServiceType = serviceType ?: "1"
            return "${company}_${routeName}_${safeBound}_${safeServiceType}_${stopId}"
        }
    }
}
