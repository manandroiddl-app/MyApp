package com.example.lifeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transit_bookmarks")
data class TransitBookmarkEntity(
    @PrimaryKey
    val bookmarkId: String,            // 格式: "KMB_1A_O_1_BS001" (Company_Route_Bound_ServiceType_StopId)
    val routeName: String,             // 路線號碼 (例: "1A")
    val company: String,               // 公司標記 ("KMB")
    val bound: String,                 // 方向 ("I" / "O")
    val serviceType: String,           // 服務類型 ("1")
    val originZh: String,              // 起點名稱
    val destinationZh: String,         // 目的地名稱
    val stopId: String,                // 車站 ID
    val stopNameZh: String,            // 車站名稱 (例: "尖沙咀碼頭")
    val sequence: Int,                 // 站序
    val createdAt: Long = System.currentTimeMillis() // 建立時間戳
)
