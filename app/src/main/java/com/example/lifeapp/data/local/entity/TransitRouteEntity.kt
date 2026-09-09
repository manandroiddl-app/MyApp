package com.example.lifeapp.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "Transit_Route",
    primaryKeys = ["co", "route_name", "bound", "other_key"]
)
data class TransitRouteEntity(
    @ColumnInfo(name = "co")
    val co: String,

    @ColumnInfo(name = "route_name")
    val routeName: String,

    @ColumnInfo(name = "bound")
    val bound: String,

    @ColumnInfo(name = "other_key")
    val otherKey: String = "1",

    @ColumnInfo(name = "other_key_desc")
    val otherKeyDesc: String? = null,

    @ColumnInfo(name = "bound_desc")
    val boundDesc: String? = null,

    @ColumnInfo(name = "ori_tc")
    val oriTc: String? = null,

    @ColumnInfo(name = "ori_eng")
    val oriEng: String? = null,

    @ColumnInfo(name = "dest_tc")
    val destTc: String? = null,

    @ColumnInfo(name = "dest_eng")
    val destEng: String? = null,

    @ColumnInfo(name = "info_type")
    val infoType: String? = null,

    @ColumnInfo(name = "info_val")
    val infoVal: String? = null
)
