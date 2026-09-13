package com.example.lifeapp.data.local.view

import androidx.room.ColumnInfo
import androidx.room.DatabaseView

@DatabaseView(
    viewName = "vw_transit_route_stop",
    value = """
        SELECT 
            r.co,
            r.co_tc,
            r.route_name,
            r.bound,
            r.other_key,
            r.ori_tc,
            r.ori_eng,
            r.dest_tc,
            r.dest_eng,
            rs.seq,
            s.stop_id,
            s.name_tc,
            s.name_en,
            s.lat,
            s.lng
        FROM Transit_Route_Stop rs
        LEFT JOIN Transit_Route r
            ON (rs.co = r.co AND rs.route_name = r.route_name AND rs.bound = r.bound AND rs.other_key = r.other_key)
        LEFT JOIN Transit_Stop s
            ON (rs.co = s.co AND rs.stop_id = s.stop_id)
    """
)
data class TransitRouteStopView(
    @ColumnInfo(name = "co")
    val co: String,

    @ColumnInfo(name = "co_tc")
    val coTc: String? = null,

    @ColumnInfo(name = "route_name")
    val routeName: String,

    @ColumnInfo(name = "bound")
    val bound: String,

    @ColumnInfo(name = "other_key")
    val otherKey: String,

    @ColumnInfo(name = "ori_tc")
    val oriTc: String? = null,

    @ColumnInfo(name = "ori_eng")
    val oriEng: String? = null,

    @ColumnInfo(name = "dest_tc")
    val destTc: String? = null,

    @ColumnInfo(name = "dest_eng")
    val destEng: String? = null,

    @ColumnInfo(name = "seq")
    val seq: Int,

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
