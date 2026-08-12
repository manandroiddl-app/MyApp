package com.example.lifeapp.data.model

import com.google.gson.annotations.SerializedName

data class AppConfig(
    @SerializedName("global_announcement")
    val globalAnnouncement: GlobalAnnouncement? = null,
    
    @SerializedName("hub_screen")
    val hubScreen: HubConfig = HubConfig()
)

data class GlobalAnnouncement(
    val enabled: Boolean = false,
    val title: String = "",
    val message: String = "",
    val level: String = "info" // info, warning, danger
)

data class HubConfig(
    val cards: List<HubCardConfig> = emptyList()
)

data class HubCardConfig(
    val id: String = "",
    val title: String = "",
    val subtitle: String = "",
    val icon: String = "📌",
    val enabled: Boolean = true,
    val badge: String? = null
)
