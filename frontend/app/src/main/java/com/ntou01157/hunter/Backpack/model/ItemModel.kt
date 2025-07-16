package com.ntou01157.hunter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("_id") val _id: String,
    @SerializedName("itemId") val itemId: String,
    @SerializedName("itemFunc") val itemFunc: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemType") val itemType: Int,
    @SerializedName("itemEffect") val itemEffect: String,
    @SerializedName("itemMethod") val itemMethod: String,
    @SerializedName("itemRarity") val itemRarity: Int,
    @SerializedName("resultId") val resultId: String? = null,
    @SerializedName("itemPic") val itemPic: String = "default_itempic" // 預設圖片名稱
) {
    // 數量由 UserBackpack 提供，不在 Item API 回應中
    var quantity: Int = 0
    // 用於 Compose UI 的可變狀態
    var count: MutableState<Int> = mutableStateOf(quantity)

    // 在物件建立後，根據 quantity 初始化 count
    init {
        count = mutableStateOf(quantity)
    }
}
