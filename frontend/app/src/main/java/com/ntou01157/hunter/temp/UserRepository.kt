package com.ntou01157.hunter.temp

import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.api.*

class UserRepository {

    suspend fun updatePhotoUrl(userId: String, photoUrl: String): Boolean {
        return try {
            val response = RetrofitClient.apiService.updatePhotoUrl(userId, PhotoUrlBody(photoUrl))
            response.isSuccessful
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
