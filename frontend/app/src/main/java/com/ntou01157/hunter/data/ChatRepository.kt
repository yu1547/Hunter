package com.ntou01157.hunter.data

import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.ChatRequest
import com.ntou01157.hunter.api.ChatResponse
import com.ntou01157.hunter.utils.NetworkResult

class ChatRepository(private val apiService: ApiService) {
    suspend fun chatWithLLM(userId: String, body: ChatRequest): NetworkResult<ChatResponse?> {
        return try {
            val response = apiService.chatWithLLM(userId, body)
            NetworkResult.Success(response)
        } catch (e: Exception) {
            NetworkResult.Error("Exception: ${e.message}")
        }
    }
}
