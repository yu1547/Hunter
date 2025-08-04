// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/SlimeAttackResponse.kt

package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

/**
 * 史萊姆攻擊事件的 API 回應格式，包含使用者資料及掉落的獎勵物品列表。
 */
data class SlimeAttackResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User,
    val rewards: List<EventReward> // 包含 itemId 和數量
)

/**
 * 用於描述事件獎勵物品的資料類別。
 */
data class EventReward(
    val itemId: String,
    val quantity: Int
)