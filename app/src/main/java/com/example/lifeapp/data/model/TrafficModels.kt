package com.example.lifeapp.data.model

import org.simpleframework.xml.Element
import org.simpleframework.xml.ElementList
import org.simpleframework.xml.Root

// XML 根節點 <specialtrafficnews>
@Root(name = "specialtrafficnews", strict = false)
data class SpecialTrafficNewsResponse @JvmOverloads constructor(
    @field:ElementList(inline = true, required = false, name = "message")
    var messageList: List<TrafficMessage>? = mutableListOf()
)

// XML 子節點 <message>
@Root(name = "message", strict = false)
data class TrafficMessage @JvmOverloads constructor(
    @field:Element(name = "msgID", required = false)
    var msgID: String? = "",

    @field:Element(name = "chinText", required = false)
    var chinText: String? = "",

    @field:Element(name = "engText", required = false)
    var engText: String? = "",

    @field:Element(name = "ReferenceDate", required = false)
    var referenceDate: String? = ""
)

// UI 顯示用的乾淨模型
data class TrafficItem(
    val id: String,
    val title: String,
    val timeText: String
)

// Traffic UI 綜合狀態
data class TrafficUiState(
    val isLoading: Boolean = true,
    val items: List<TrafficItem> = emptyList(),
    val updateTimeText: String = "",
    val errorMessage: String? = null
)
