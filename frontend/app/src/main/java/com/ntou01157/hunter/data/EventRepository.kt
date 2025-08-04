package com.ntou01157.hunter.data

import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.ExchangeRequest
import com.ntou01157.hunter.api.AttackRequest
import com.ntou01157.hunter.api.OpenTreasureBoxRequest
import com.ntou01157.hunter.api.BlessRequest
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.models.model_api.EventRewardResponse
import com.ntou01157.hunter.models.model_api.GeneralResponse
import com.ntou01157.hunter.models.model_api.SlimeAttackResponse
import com.ntou01157.hunter.utils.NetworkResult

class EventRepository(private val apiService: ApiService) {

    // 獲取每日事件
    suspend fun getDailyEvents(): NetworkResult<List<EventModel>> {
        return try {
            val response = apiService.getDailyEvents()
            if (response.isSuccessful) {
                val events = response.body()
                events?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("API 請求失敗: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }

    // 獲取永久事件
    suspend fun getPermanentEvents(): NetworkResult<List<EventModel>> {
        return try {
            val response = apiService.getPermanentEvents()
            if (response.isSuccessful) {
                val events = response.body()
                events?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("API 請求失敗: ${response.code()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }

    // 神秘商人交易
    suspend fun exchangeItems(userId: String, exchangeOption: String): NetworkResult<com.ntou01157.hunter.api.GeneralResponse> {
        return try {
            val response = apiService.postMerchantExchange(ExchangeRequest(userId, exchangeOption))
            if (response.isSuccessful) {
                val result = response.body()
                result?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("交易失敗: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }

    // 史萊姆攻擊
    suspend fun attackSlime(userId: String, totalDamage: Int, usedTorch: Boolean): NetworkResult<com.ntou01157.hunter.api.SlimeAttackResponse> {
        return try {
            val response = apiService.postSlimeAttack(AttackRequest(userId, totalDamage, usedTorch))
            if (response.isSuccessful) {
                val result = response.body()
                result?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("史萊姆攻擊處理失敗: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }

    // 開啟寶箱
    suspend fun openTreasureBox(userId: String, keyId: String): NetworkResult<com.ntou01157.hunter.api.EventRewardResponse> {
        return try {
            val response = apiService.postOpenTreasureBox(OpenTreasureBoxRequest(userId, keyId))
            if (response.isSuccessful) {
                val result = response.body()
                result?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("開啟寶箱失敗: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }

    // 古樹的祝福
    suspend fun blessFromAncientTree(userId: String, option: String): NetworkResult<com.ntou01157.hunter.api.GeneralResponse> {
        return try {
            val response = apiService.postAncientTreeBlessing(BlessRequest(userId, option))
            if (response.isSuccessful) {
                val result = response.body()
                result?.let {
                    NetworkResult.Success(it)
                } ?: NetworkResult.Error("API 回應為空")
            } else {
                NetworkResult.Error("古樹祝福失敗: ${response.errorBody()?.string()}")
            }
        } catch (e: Exception) {
            NetworkResult.Error("網路錯誤: ${e.message}")
        }
    }
}