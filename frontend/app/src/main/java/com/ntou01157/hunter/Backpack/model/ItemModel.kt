package com.ntou01157.hunter.Backpack.model

import com.google.gson.annotations.SerializedName

data class Item(
    @SerializedName("_id") val _id: String,
    @SerializedName("itemFunc") val itemFunc: String,
    @SerializedName("itemName") val itemName: String,
    @SerializedName("itemType") val itemType: Int,
    @SerializedName("itemEffect") val itemEffect: String,
    @SerializedName("itemMethod") val itemMethod: String,
    @SerializedName("itemRarity") val itemRarity: Int,
    @SerializedName("resultId") val resultId: String? = null,
    @SerializedName("itemPic") val itemPic: String = "default_itempic" // 預設圖片名稱
)
