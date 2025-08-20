// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/EventResponse.kt
package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

// 確保所有相關的類別都已匯入
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.models.model_api.RewardsModel
import com.ntou01157.hunter.models.model_api.User

data class EventResponse(
    val message: String,
    val eventData: EventModel?,
    val rewards: RewardsModel?,
    val updatedUser: User?
)