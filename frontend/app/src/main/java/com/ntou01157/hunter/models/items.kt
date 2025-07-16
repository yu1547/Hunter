package com.ntou01157.hunter.models

data class Item(
    val itemId: String,
    val itemPic: String,
    val itemFunc: String,
    val itemName: String,
    val itemType: Int,
    val itemEffect: String,
    val itemMethod: String,
    val itemRarity: Int,
    val resultId: String
    )