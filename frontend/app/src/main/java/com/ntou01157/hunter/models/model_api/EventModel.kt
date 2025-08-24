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
    val location: EventLocation? = null,
    val lastTriggered: String? = null,
)

data class EventMechanics(
    // 直接使用 RewardsModel
    val rewards: RewardsModel,
    val additionalInfo: AdditionalInfo?
)

data class EventLocation(
    val lat: Double,
    val lng: Double
)

data class AdditionalInfo(
    val lastLocationUpdate: String? = null,
    val attackMultiplier: Int? = null,
    val exchangeOptions: List<ExchangeOption>? = null
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