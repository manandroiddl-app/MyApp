package com.example.lifeapp.data.datasource

import com.example.lifeapp.data.model.TransitEta
import com.example.lifeapp.data.model.TransitRoute
import com.example.lifeapp.data.model.TransitStop

interface BusDataSource {
    suspend fun getRoutes(): List<TransitRoute>
    suspend fun getRouteStops(route: String, bound: String, serviceType: String): List<TransitStop>
    suspend fun getEta(stopId: String, route: String, serviceType: String, bound: String? = null): List<TransitEta>
}
