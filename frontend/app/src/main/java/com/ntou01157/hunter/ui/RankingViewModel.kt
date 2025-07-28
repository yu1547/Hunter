// frontend/app/src/main/java/com/ntou01157/hunter/ui/RankingViewModel.kt
package com.ntou01157.hunter.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.MainApplication // 確保導入您的 Application 類
import com.ntou01157.hunter.data.RankRepository
import com.ntou01157.hunter.models.model_api.RankResponse // 確保導入正確的 RankResponse
import com.ntou01157.hunter.utils.NetworkResult // 確保導入 NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


/**
 * 處理排行榜數據獲取和狀態管理的 ViewModel。
 */
class RankingViewModel(private val rankRepository: RankRepository) : ViewModel() {

    // 使用 MutableStateFlow 來管理排行榜數據的狀態
    // 初始狀態為 Loading，表示正在加載數據
    private val _rankData = MutableStateFlow<NetworkResult<RankResponse>>(NetworkResult.Loading())
    val rankData: StateFlow<NetworkResult<RankResponse>> = _rankData.asStateFlow()

    /**
     * 從數據儲存庫獲取排行榜數據。
     */
    fun fetchRankData(userId: String) {
        viewModelScope.launch {
            // 在每次請求開始前，將狀態設置為 Loading
            _rankData.value = NetworkResult.Loading()
            // 調用 Repository 獲取數據，並更新 StateFlow 的值
            _rankData.value = rankRepository.getRank(userId)
        }
    }
}

/**
 * 用於實例化 RankingViewModel 的 Factory。
 * 這是因為 ViewModel 構造函數需要 RankRepository 參數。
 */
class RankingViewModelFactory(private val application: MainApplication) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RankingViewModel::class.java)) {
            // 使用 MainApplication 中提供的 rankRepository 實例
            @Suppress("UNCHECKED_CAST")
            return RankingViewModel(application.rankRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}