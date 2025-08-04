// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/EventModel.kt

package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class EventModel(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val mechanics: EventMechanics,
    val location: EventLocation? = null, // 每日事件可能會有位置資訊
    val lastTriggered: String? = null // 每日事件可能會有上次觸發時間
)

data class EventMechanics(
    val rewards: List<Reward>,
    val additionalInfo: AdditionalInfo? // 額外的事件參數
)

data class Reward(
    val itemId: String?,
    val quantity: Int?,
    val points: Int?,
    val title: String?
)

data class EventLocation(
    val lat: Double,
    val lng: Double
)

data class AdditionalInfo(
    // 這裡可以根據事件類型定義不同的欄位
    val lastLocationUpdate: String? = null,
    val attackMultiplier: Int? = null, // 史萊姆事件
    val exchangeOptions: List<ExchangeOption>? = null // 神秘商人事件
)

data class ExchangeOption(
    val option: String,
    val requiredItems: List<RequiredItem>,
    val rewardItems: List<RewardItem>
)

data class RequiredItem(
    val itemId: String,
    val quantity: Int
)

data class RewardItem(
    val itemId: String,
    val quantity: Int
)