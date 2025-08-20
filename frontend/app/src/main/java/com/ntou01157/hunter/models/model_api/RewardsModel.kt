// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/RewardsModel.kt
package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class RewardsModel(
    val points: Int?,
    val items: List<RewardItem>?,
    val title: String?
)
