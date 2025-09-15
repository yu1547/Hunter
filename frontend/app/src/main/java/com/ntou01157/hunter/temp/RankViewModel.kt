package com.ntou01157.hunter.temp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.utils.NetworkResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.Response

class RankingViewModel : ViewModel() {

    private val _rankData =
        MutableStateFlow<NetworkResult<RankResponse>>(NetworkResult.Loading())
    val rankData: StateFlow<NetworkResult<RankResponse>> = _rankData

    /** fire-and-forget 版本 */
    fun fetchRankData(userId: String) = viewModelScope.launch {
        fetchRankDataOnce(userId)
    }

    /** 等待結果的版本（回傳是否已拿到 userRank） */
    suspend fun fetchRankDataOnce(userId: String): Boolean {
        return try {
            _rankData.value = NetworkResult.Loading()

            // ← 這裡是 Response<RankResponse>
            val http: Response<RankResponse> = RetrofitClient.apiService.getRank(userId)

            if (http.isSuccessful) {
                val body = http.body()
                if (body != null) {
                    _rankData.value = NetworkResult.Success(body)
                    body.userRank != null
                } else {
                    _rankData.value = NetworkResult.Error("Empty response body")
                    false
                }
            } else {
                _rankData.value =
                    NetworkResult.Error("HTTP ${http.code()} ${http.message()}")
                false
            }
        } catch (e: Exception) {
            _rankData.value = NetworkResult.Error(e.message ?: "Network error")
            false
        }
    }
}
