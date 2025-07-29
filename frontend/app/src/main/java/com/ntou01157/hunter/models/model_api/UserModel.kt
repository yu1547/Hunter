package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName
import java.util.Date

// 用戶模型
data class User(
    @SerializedName("_id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("backpackItems") val backpackItems: List<BackpackItem>,
    @SerializedName("missions") val missions: List<Mission> = emptyList()
    // 其他用戶屬性...
)

// 背包中的物品
data class BackpackItem(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)

// 使用者任務進度模型
data class Mission(
    @SerializedName("taskId") val taskId: String,
    @SerializedName("state") val state: String,
    @SerializedName("acceptedAt") val acceptedAt: Date?,
    @SerializedName("expiresAt") val expiresAt: Date?,
    @SerializedName("refreshedAt") val refreshedAt: Date?,
    @SerializedName("checkPlaces") val checkPlaces: List<CheckPlace> = emptyList()
)

// 任務打卡點狀態
data class CheckPlace(
    @SerializedName("spotId") val spotId: String,
    @SerializedName("isCheck") val isCheck: Boolean
)