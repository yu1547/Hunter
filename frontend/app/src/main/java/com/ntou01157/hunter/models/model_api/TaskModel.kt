package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

data class Task (
    @SerializedName("_id") val taskId: String,
    @SerializedName("taskName") val taskName: String,
    @SerializedName("taskDescription") val taskDescription: String?,
    @SerializedName("taskDifficulty") val taskDifficulty: String?,
    @SerializedName("taskTarget") val taskTarget: String,
    @SerializedName("checkPlaces") val checkPlaces: List<CheckPlaces> = emptyList(),
    @SerializedName("taskDuration") val taskDuration: Long?, // 單位：秒
    @SerializedName("rewardItems") val rewardItems: List<RewardItem> = emptyList(),
    @SerializedName("rewardScore") val rewardScore: Int,
    @SerializedName("isLLM") val isLLM: Boolean = false
)

data class RewardItem(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)

data class CheckPlaces(
    @SerializedName("spotId") val spotId: String,
)