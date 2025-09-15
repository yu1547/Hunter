package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName
import java.util.Date

// 用戶模型
data class User(
    @SerializedName("_id") val id: String,
    @SerializedName("username") val username: String,
    @SerializedName("backpackItems") val backpackItems: List<BackpackItem>,
    @SerializedName("missions") val missions: List<Mission> = emptyList(),
    @SerializedName("gender") val gender: String?,
    @SerializedName("age") val age: String?,
    @SerializedName("photoURL") val photoURL: String? = null,
    @SerializedName("settings") val settings: Settings?,
    @SerializedName("spotsScanLogs") val spotsScanLogs: Map<String, Boolean>? = null,
    @SerializedName("buff") val buff: List<Buff>? = null

    // 其他用戶屬性...
)

data class SpotUpdateBody(@SerializedName("spotId") val spotId: String)

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
    @SerializedName("haveCheckPlaces") val haveCheckPlaces: List<HaveCheckPlaces> = emptyList(),
    @SerializedName("isLLM") val isLLM: Boolean = false
)

// 任務打卡點狀態
data class HaveCheckPlaces(
    @SerializedName("spotId") val spotId: String,
    @SerializedName("isCheck") val isCheck: Boolean
)

// 使用者設定模型
data class Settings(
    @SerializedName("music") val music: Boolean,
    @SerializedName("notification") val notification: Boolean,
    @SerializedName("language") val language: String
)

data class Buff(
    @SerializedName("name") val name: String,                       // e.g. "ancient_branch"
    @SerializedName("expiresAt") val expiresAt: String              // e.g. "2025-08-24T22:46:28.153Z"
) {
    fun expiresAtMillisOrNull(): Long? =
        runCatching { java.time.Instant.parse(expiresAt).toEpochMilli() }.getOrNull()
}

fun List<Buff>?.expireAtOf(name: String): Long? =
    this?.firstOrNull { it.name == name }?.expiresAtMillisOrNull()