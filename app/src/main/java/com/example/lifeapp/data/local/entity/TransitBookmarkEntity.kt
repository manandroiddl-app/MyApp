package com.example.lifeapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transit_bookmarks")
data class TransitBookmarkEntity(
    @PrimaryKey
    val bookmarkId: String,
    val routeName: String,
    val company: String,
    val bound: String?,
    val serviceType: String?,
    val originZh: String,
    val destinationZh: String,
    val stopId: String,
    val stopNameZh: String,
    val sequence: Int
) {
    companion object {
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
