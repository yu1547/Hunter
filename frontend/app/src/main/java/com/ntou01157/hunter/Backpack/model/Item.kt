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
    @SerializedName("imageResId") val imageResId: Int = R.drawable.item1, // 預設圖片
    var quantity: Int = 1  // 本地屬性，不來自API
) {
    var count: MutableState<Int> = mutableStateOf(quantity)  // 使用傳入的 quantity 初始化 count
}
