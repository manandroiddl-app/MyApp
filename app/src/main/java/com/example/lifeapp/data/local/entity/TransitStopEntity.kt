package com.example.lifeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "Transit_Stop",
    primaryKeys = ["co", "stop_id"]
)
data class TransitStopEntity(
    @ColumnInfo(name = "co")
    val co: String,

    @ColumnInfo(name = "stop_id")
    val stopId: String,

    @ColumnInfo(name = "name_tc")
    val nameTc: String? = null,

    @ColumnInfo(name = "name_en")
    val nameEn: String? = null,

    @ColumnInfo(name = "lat")
    val lat: Double,

    @ColumnInfo(name = "lng")
    val lng: Double
)
