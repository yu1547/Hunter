package com.ntou01157.hunter.model.model_api

import com.google.gson.annotations.SerializedName

// 用戶模型
data class User(
    @SerializedName("_id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("backpackItems") val backpackItems: List<BackpackItem>
    // 其他用戶屬性...
)

// 背包中的物品
data class BackpackItem(
    @SerializedName("itemId") val itemId: String,
    @SerializedName("quantity") val quantity: Int
)
