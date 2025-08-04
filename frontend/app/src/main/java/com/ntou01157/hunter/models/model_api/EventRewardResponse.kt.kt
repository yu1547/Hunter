// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/EventRewardResponse.kt

package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

/**
 * 寶箱開啟事件的 API 回應格式，包含使用者資料、積分和物品獎勵。
 */
data class EventRewardResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User,
    val rewards: EventRewards // 包含 points 和 items
)

/**
 * 寶箱事件的獎勵結構，包含積分和隨機獲得的物品列表。
 */
data class EventRewards(
    val points: Int,
    val items: List<EventReward>
)