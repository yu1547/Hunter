// frontend/app/src/main/java/com/ntou01157/hunter/data/RankRepository.kt
package com.ntou01157.hunter.data

import com.ntou01157.hunter.api.RankApi
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.utils.NetworkResult

// 將 RankApi 的實例作為參數傳入
class RankRepository(private val rankApi: RankApi) { // <-- 這裡的類型現在是 RankApi

    suspend fun getRank(): NetworkResult<RankResponse> {
        return try {
            val response = rankApi.getRank() // <-- 調用 RankApi 的 getRank 方法
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