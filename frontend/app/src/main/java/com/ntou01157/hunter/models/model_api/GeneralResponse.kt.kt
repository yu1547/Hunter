// frontend/app/src/main/java/com/ntou01157/hunter/models/model_api/GeneralResponse.kt

package com.ntou01157.hunter.models.model_api

import com.google.gson.annotations.SerializedName

/**
 * 通用的 API 回應格式，用於處理只包含成功狀態、訊息和使用者資料的請求。
 */
data class GeneralResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User
)