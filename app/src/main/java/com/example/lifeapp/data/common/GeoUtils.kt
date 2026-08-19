package com.example.lifeapp.data.common

import kotlin.math.*

/**
 * 空間地理資訊 (GIS) 計算工具
 */
object GeoUtils {

    /**
     * Point-in-Polygon (Ray-Casting 射線法)
     * 判斷點 (lat, lng) 是否位於 Polygon 多邊形內部
     * polygon: 多邊形頂點串列, 格式為 (lng, lat)
     */
    fun isPointInPolygon(lat: Double, lng: Double, polygon: List<Pair<Double, Double>>): Boolean {
        var intersectCount = 0
        val size = polygon.size
        for (i in 0 until size) {
            val j = (i + 1) % size
            val lng1 = polygon[i].first
            val lat1 = polygon[i].second
            val lng2 = polygon[j].first
            val lat2 = polygon[j].second

            if (((lat1 > lat) != (lat2 > lat)) &&
                (lng < (lng2 - lng1) * (lat - lat1) / (lat2 - lat1) + lng1)
            ) {
                intersectCount++
            }
        }
        return intersectCount % 2 != 0
    }

    /**
     * 計算 Polygon 多邊形的幾何重心 (Centroid)
     * 回傳 Pair(lat, lng)
     */
    fun calculatePolygonCentroid(polygon: List<Pair<Double, Double>>): Pair<Double, Double>? {
        if (polygon.isEmpty()) return null
        var sumLat = 0.0
        var sumLng = 0.0
        for (pt in polygon) {
            sumLng += pt.first
            sumLat += pt.second
        }
        return Pair(sumLat / polygon.size, sumLng / polygon.size)
    }

    /**
     * 兩點間球面距離 (Haversine Formula) - 回傳單位: 米 (m)
     */
    fun calculateHaversineDistanceMeters(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
        val r = 6371000.0 // 地球半徑 (米)
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2).pow(2) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return r * c
    }

    /**
     * Point-to-LineDistance (點到折線的最短距離)
     * 適用於判定點 (lat, lng) 與街道 (LineString Polyline) 的垂直距離
     * 回傳單位: 米 (m)
     */
    fun minDistanceToPolylineMeters(
        pointLat: Double,
        pointLng: Double,
        polyline: List<Pair<Double, Double>>
    ): Double {
        if (polyline.isEmpty()) return Double.MAX_VALUE
        if (polyline.size == 1) {
            return calculateHaversineDistanceMeters(pointLat, pointLng, polyline[0].second, polyline[0].first)
        }

        var minDistance = Double.MAX_VALUE
        for (i in 0 until polyline.size - 1) {
            val p1 = polyline[i]
            val p2 = polyline[i + 1]
            val dist = distanceToSegmentMeters(pointLat, pointLng, p1.second, p1.first, p2.second, p2.first)
            if (dist < minDistance) {
                minDistance = dist
            }
        }
        return minDistance
    }

    private fun distanceToSegmentMeters(
        px: Double, py: Double,
        ax: Double, ay: Double,
        bx: Double, by: Double
    ): Double {
        val l2 = (bx - ax).pow(2) + (by - ay).pow(2)
        if (l2 == 0.0) return calculateHaversineDistanceMeters(px, py, ax, ay)
        var t = ((px - ax) * (bx - ax) + (py - ay) * (by - ay)) / l2
        t = max(0.0, min(1.0, t))
        val projLat = ax + t * (bx - ax)
        val projLng = ay + t * (by - ay)
        return calculateHaversineDistanceMeters(px, py, projLat, projLng)
    }
}
