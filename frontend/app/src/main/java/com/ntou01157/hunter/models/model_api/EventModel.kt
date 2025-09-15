// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/EventModel.kt
package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class EventModel(
    @SerializedName("_id")
    val id: String,
    val name: String,
    val description: String,
    val type: String,
    val spotId: String? = null,
    val options: List<Option>? = null,
    val rewards: Reward? = null,
    val consume: Reward? = null,
    val lastTriggered: String? = null,
)

data class Option(
    val text: String,
    val rewards: Reward,
    val consume: Reward? = null
)

data class Reward(
    val points: Int? = 0,
    val items: List<RewardItemDetail>? = null,
    val title: String? = null
)

data class RewardItemDetail(
    val itemId: String,
    val quantity: Int
)