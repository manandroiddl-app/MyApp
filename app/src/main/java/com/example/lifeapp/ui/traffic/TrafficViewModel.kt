package com.example.lifeapp.ui.traffic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeapp.data.common.TrafficTaggingEngine
import com.example.lifeapp.data.model.TrafficNewsItem
import com.example.lifeapp.data.model.TrafficUiState
import com.example.lifeapp.data.repository.LocationRepository
import com.example.lifeapp.data.repository.TrafficRepository
import com.example.lifeapp.ui.common.AutoRefreshDelegate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 包裝帶有 18 區 Tag 的交通消息 UI Model
 */
data class TaggedTrafficNewsItem(
    val rawItem: TrafficNewsItem,
    val districtTags: List<String>
)

data class EnhancedTrafficUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val updateTime: String = "",
    val taggedTrafficNews: List<TaggedTrafficNewsItem> = emptyList()
)

@HiltViewModel
class TrafficViewModel @Inject constructor(
    private val trafficRepository: TrafficRepository,
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _enhancedUiState = MutableStateFlow(EnhancedTrafficUiState())
    val enhancedUiState: StateFlow<EnhancedTrafficUiState> = _enhancedUiState.asStateFlow()

    private val autoRefreshDelegate = AutoRefreshDelegate(viewModelScope) {
        loadTrafficData(isSilent = true)
    }

    init {
        loadTrafficData(isSilent = false)
    }

    fun refresh() {
        loadTrafficData(isSilent = false)
    }

    fun loadTrafficData(isSilent: Boolean = false) {
        viewModelScope.launch {
            if (!isSilent && _enhancedUiState.value.taggedTrafficNews.isEmpty()) {
                _enhancedUiState.update { it.copy(isLoading = true, errorMessage = null) }
            }

            // Fetch 原始交通消息
            val rawState = trafficRepository.fetchTrafficNews()

            if (rawState.errorMessage != null) {
                _enhancedUiState.value = EnhancedTrafficUiState(
                    isLoading = false,
                    errorMessage = rawState.errorMessage,
                    updateTime = rawState.updateTime
                )
            } else {
                // 讀取 Room 内的街道數據供 Tagging 比對 (如有)
                val dbLocations = try {
                    locationRepository.searchByName("道") // 快速載入街道做 Tagging 比對
                } catch (e: Exception) {
                    emptyList()
                }

                // 進行 18 區 Tag 提煉
                val taggedList = rawState.trafficNews.map { news ->
                    val newsText = "${news.referenceDate ?: ""} ${news.chinText ?: ""}"
                    val tags = TrafficTaggingEngine.extractDistrictTags(newsText, dbLocations)
                    TaggedTrafficNewsItem(rawItem = news, districtTags = tags)
                }

                _enhancedUiState.value = EnhancedTrafficUiState(
                    isLoading = false,
                    errorMessage = null,
                    updateTime = rawState.updateTime,
                    taggedTrafficNews = taggedList
                )
            }
        }
    }

    fun startAutoRefresh() {
        autoRefreshDelegate.start()
    }

    fun stopAutoRefresh() {
        autoRefreshDelegate.stop()
    }

    override fun onCleared() {
        super.onCleared()
        stopAutoRefresh()
    }
}
