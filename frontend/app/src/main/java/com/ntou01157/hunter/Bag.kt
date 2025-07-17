package com.ntou01157.hunter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.ntou01157.hunter.R

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

// 所有可用的物品模板清單（初始值為 0）
val allItemsTemplate = listOf(
    Item(1, "用於開啟寶箱", "金鑰匙", 1, "開啟寶箱", 0, "由銀鑰匙合成", "UR", false, R.drawable.item1),
    Item(2, "讓我充數一下", "史萊姆", 1, "就很可愛讓你觀賞", 0, "路邊撿到的", "S", false, R.drawable.item2),
    Item(3, "用來合成出史萊姆的素材", "史萊姆球", 0, "史萊姆球是史萊姆身體的一部分", 0, "做任務獲得", "R", true, R.drawable.item3),
    Item(4, "就是水", "水滴", 0, "可以用來合成各種素材", 0, "做任務得到", "R", true, R.drawable.item4),
    Item(5, "黃金碎片", "金鑰匙碎片", 0, "可以用來合成金鑰匙", 0, "做任務得到", "R", true, R.drawable.item5),
)
