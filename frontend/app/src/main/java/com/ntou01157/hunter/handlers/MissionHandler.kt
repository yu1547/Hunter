package com.ntou01157.hunter.handlers

import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.CheckSpotMissionResponse
import android.util.Log
import retrofit2.HttpException
import java.io.IOException

object MissionHandler {

    private val apiService = RetrofitClient.apiService

    private const val TAG = "MissionHandler"

    /**
     * 向後端發送請求，檢查並更新指定補給站的任務進度。
     * @param userId 使用者的 ID
     * @param spotId 補給站的 ID
     * @return CheckSpotMissionResponse 回傳操作結果，包含訊息和任務是否完成的狀態
     * @throws Exception 如果 API 請求失敗
     */
    suspend fun checkSpotMission(userId: String, spotId: String): CheckSpotMissionResponse {
        return try {
            val response = apiService.checkSpotMission(userId, spotId)
            Log.d(TAG, "checkSpotMission response: ${response.message}")
            response
        } catch (e: HttpException) {
            val errorBody = e.response()?.errorBody()?.string()
            val errorMessage = "HTTP 錯誤: ${e.code()} - ${errorBody ?: e.message()}"
            Log.e(TAG, errorMessage)
            // Pass null for the user object since the request failed
            CheckSpotMissionResponse(user = null, message = errorMessage, isMissionCompleted = false)
        } catch (e: IOException) {
            val errorMessage = "網路錯誤: ${e.message}"
            Log.e(TAG, errorMessage)
            CheckSpotMissionResponse(user = null, message = errorMessage, isMissionCompleted = false)
        } catch (e: Exception) {
            val errorMessage = "未知錯誤: ${e.message}"
            Log.e(TAG, errorMessage)
            CheckSpotMissionResponse(user = null, message = errorMessage, isMissionCompleted = false)
        }
    }
}