package com.ntou01157.hunter.data

// data/EventRepository.kt

import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.CompleteEventRequest
import com.ntou01157.hunter.api.TriggerEventRequest
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.utils.NetworkResult
import com.ntou01157.hunter.models.model_api.*

class EventRepository(private val apiService: ApiService) {

    suspend fun triggerEvent(
        eventId: String,
        userId: String,
        latitude: Double,
        longitude: Double
    ): NetworkResult<EventModel> {
        return try {
            val request = TriggerEventRequest(userId, latitude, longitude)
            val response: EventResponse = apiService.triggerEvent(eventId, request)

            // 使用 let 語法安全地處理 eventData
            response.eventData?.let { eventData ->
                // 如果 eventData 不為 null，回傳 Success
                NetworkResult.Success(eventData)
            } ?: run {
                // 如果 eventData 為 null，回傳 Error
                NetworkResult.Error(response.message)
            }
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "觸發事件失敗")
        }
    }

    suspend fun completeEvent(
        eventId: String,
        userId: String,
        selectedOption: String? = null,
        gameResult: Int? = null
    ): NetworkResult<EventResponse> {
        return try {
            val request = CompleteEventRequest(userId, selectedOption, gameResult)
            val response = apiService.completeEvent(eventId, request)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error(e.message ?: "完成事件失敗")
        }
    }
}