package com.ntou01157.hunter.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

data class Item(
    val itemid: Int,
    val itemFunc: String,
    val itemName: String,
    val itemType: Int,
    val itemEffect: String,
    val initialCount: Int,
    val itemMethod: String,
    val itemRarity: String,
    val isblend: Boolean,
    val imageResId: Int
) {
    var count: MutableState<Int> = mutableStateOf(initialCount)
}
