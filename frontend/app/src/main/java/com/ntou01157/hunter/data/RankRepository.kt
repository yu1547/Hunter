// frontend/app/src/main/java/com/ntou01157/hunter/data/RankRepository.kt
package com.ntou01157.hunter.data

import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.utils.NetworkResult

// 將 RankApi 的實例作為參數傳入
class RankRepository(private val apiService: ApiService) { // <-- 這裡的類型現在是 RankApi
    suspend fun getRank(userId: String): NetworkResult<RankResponse> {
        return try {
            val response = apiService.getRank(userId)
            if (response.isSuccessful && response.body() != null) {
                NetworkResult.Success(response.body()!!)
            } else {
                NetworkResult.Error("Error: ${response.code()} ${response.message()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("Exception: ${e.message}")
        }
    }
}