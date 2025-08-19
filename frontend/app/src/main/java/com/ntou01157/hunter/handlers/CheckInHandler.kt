package com.ntou01157.hunter.handlers

import com.ntou01157.hunter.api.RecognizeApiProvider
import com.ntou01157.hunter.models.RecognizeRequest
import com.ntou01157.hunter.models.RecognizeResponse
import retrofit2.HttpException

object CheckInHandler {
    private val service = RecognizeApiProvider.service

    suspend fun checkIn(userId: String, spotName: String, vector: List<Float>): RecognizeResponse {
        val resp = service.recognize(RecognizeRequest(userId, spotName, vector))
        if (resp.isSuccessful) return resp.body() ?: throw IllegalStateException("Empty body")
        throw HttpException(resp)
    }
}
