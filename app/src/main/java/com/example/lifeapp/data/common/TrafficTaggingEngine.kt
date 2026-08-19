package com.example.lifeapp.data.common

import com.example.lifeapp.data.local.HongKongDistricts
import com.example.lifeapp.data.model.LocationEntity

object TrafficTaggingEngine {

    /**
     * 輸入通告內文，透過比對資料庫與 18 區地名字典，自動提取出影響的 18 區標籤清單
     */
    fun extractDistrictTags(newsText: String, dbLocations: List<LocationEntity> = emptyList()): List<String> {
        if (newsText.isBlank()) return listOf("全港")

        val matchedDistricts = mutableSetOf<String>()

        // 1. 直攻：比對 18 區名稱 (例如: "油尖旺區", "屯門區", "灣仔區")
        for (district in HongKongDistricts.districtSubMap.keys) {
            val simpleName = district.replace("區", "")
            if (newsText.contains(district) || newsText.contains(simpleName)) {
                matchedDistricts.add(district)
            }
        }

        // 2. 次區份比對 (例如內文提到 "旺角"、"尖沙咀"、"掃管笏")
        for ((district, subList) in HongKongDistricts.districtSubMap) {
            for (sub in subList) {
                if (newsText.contains(sub)) {
                    matchedDistricts.add(district)
                    break
                }
            }
        }

        // 3. 街道/站點名比對 (利用從 CSDI/九巴 下載的本地 Room 街道數據)
        if (matchedDistricts.isEmpty() && dbLocations.isNotEmpty()) {
            for (location in dbLocations) {
                if (location.nameTc.length >= 3 && newsText.contains(location.nameTc)) {
                    if (location.districtName.isNotBlank()) {
                        matchedDistricts.add(location.districtName)
                    }
                }
            }
        }

        // 若完全沒有命中任何特定地區，視為全港性交通消息 (如氣象、特別班次)
        return if (matchedDistricts.isEmpty()) listOf("全港") else matchedDistricts.toList()
    }
}
