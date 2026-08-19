package com.example.lifeapp.data.local

import com.example.lifeapp.data.model.Region

/**
 * 香港 18 區與次區份 (Sub-District) 靜態對照字典
 */
object HongKongDistricts {

    // 18 區與大區 (Region) 映射
    fun getRegionByDistrict(districtName: String): Region {
        return when {
            districtName.contains("中西區") ||
            districtName.contains("灣仔") ||
            districtName.contains("東區") ||
            districtName.contains("南區") -> Region.HONG_KONG

            districtName.contains("油尖旺") ||
            districtName.contains("深水埗") ||
            districtName.contains("九龍城") ||
            districtName.contains("黃大仙") ||
            districtName.contains("觀塘") -> Region.KOWLOON

            districtName.contains("葵青") ||
            districtName.contains("荃灣") ||
            districtName.contains("屯門") ||
            districtName.contains("元朗") ||
            districtName.contains("北區") ||
            districtName.contains("大埔") ||
            districtName.contains("沙田") ||
            districtName.contains("西貢") ||
            districtName.contains("離島") -> Region.NEW_TERRITORIES

            else -> Region.KOWLOON
        }
    }

    // 18 區與次區份 (Sub-District) 關鍵字對照
    val districtSubMap = mapOf(
        "中西區" to listOf("中環", "金鐘", "上環", "西營盤", "石塘咀", "堅尼地城", "半山"),
        "灣仔區" to listOf("灣仔", "銅鑼灣", "跑馬地", "大坑", "天后"),
        "東區" to listOf("北角", "鰂魚涌", "太古", "西灣河", "筲箕灣", "柴灣", "小西灣"),
        "南區" to listOf("香港仔", "鴨脷洲", "黃竹坑", "淺水灣", "赤柱", "石澳", "薄扶林"),

        "油尖旺區" to listOf("尖沙咀", "尖東", "佐敦", "油麻地", "旺角", "太子", "大角咀"),
        "深水埗區" to listOf("深水埗", "長沙灣", "荔枝角", "美孚", "石硤尾", "昂船洲"),
        "九龍城區" to listOf("九龍城", "土瓜灣", "紅磡", "何文田", "啟德", "九龍塘"),
        "黃大仙區" to listOf("黃大仙", "樂富", "鑽石山", "新蒲崗", "慈雲山", "牛池灣"),
        "觀塘區" to listOf("觀塘", "牛頭角", "九龍灣", "藍田", "油塘", "秀茂坪", "安達臣"),

        "葵青區" to listOf("葵芳", "葵興", "大窩口", "青衣", "荔景"),
        "荃灣區" to listOf("荃灣", "深井", "青龍頭", "馬灣"),
        "屯門區" to listOf("屯門", "藍地", "掃管笏", "碼頭區"),
        "元朗區" to listOf("元朗", "天水圍", "錦田", "新田", "落馬洲", "洪水橋"),
        "北區" to listOf("上水", "粉嶺", "沙頭角", "打鼓嶺"),
        "大埔區" to listOf("大埔", "太和", "白石角", "大埔滘"),
        "沙田區" to listOf("沙田", "大圍", "火炭", "馬鞍山", "石門"),
        "西貢區" to listOf("西貢", "將軍澳", "調景嶺", "坑口", "寶琳", "清水灣"),
        "離島區" to listOf("東涌", "赤鱲角", "機場", "迪士尼", "愉景灣", "長洲", "坪洲", "南丫島", "大澳")
    )

    /**
     * 輸入地點名稱與 18 區，自動解析推斷出 Sub-District
     */
    fun parseSubDistrict(district: String, placeName: String): String {
        val keywords = districtSubMap[district]
            ?: districtSubMap.entries.firstOrNull { district.contains(it.key.replace("區", "")) }?.value

        if (keywords != null) {
            for (kw in keywords) {
                if (placeName.contains(kw)) return kw
            }
        }

        // 全域掃描次區份字典
        for ((distKey, kwList) in districtSubMap) {
            for (kw in kwList) {
                if (placeName.contains(kw)) return kw
            }
        }

        return if (district.isNotBlank()) district else "其他"
    }

    /**
     * 輸入地點名稱，自動推算 (District, SubDistrict, Region)
     */
    fun inferHierarchy(placeName: String): Triple<Region, String, String> {
        for ((distKey, kwList) in districtSubMap) {
            for (kw in kwList) {
                if (placeName.contains(kw)) {
                    val region = getRegionByDistrict(distKey)
                    return Triple(region, distKey, kw)
                }
            }
        }
        return Triple(Region.KOWLOON, "九龍城區", "九龍城")
    }
}
