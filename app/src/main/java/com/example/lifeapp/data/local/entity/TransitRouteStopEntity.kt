package com.example.lifeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "Transit_Route_Stop",
    primaryKeys = ["co", "route_name", "bound", "other_key", "seq"],
    indices = [
        Index(value = ["co", "stop_id"])
    ]
)
data class TransitRouteStopEntity(
    @ColumnInfo(name = "co")
    val co: String,

    @ColumnInfo(name = "route_name")
    val routeName: String,

    @ColumnInfo(name = "bound")
    val bound: String,

    @ColumnInfo(name = "other_key")
    val otherKey: String = "1",

    @ColumnInfo(name = "seq")
    val seq: Int,

    @ColumnInfo(name = "stop_id")
    val stopId: String,

    @ColumnInfo(name = "info_type")
    val infoType: String? = null,

    @ColumnInfo(name = "info_val")
    val infoVal: String? = null
)
