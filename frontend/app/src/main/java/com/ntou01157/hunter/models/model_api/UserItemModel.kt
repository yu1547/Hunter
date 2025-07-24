package com.ntou01157.hunter.model.model_api

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

/*
    代表使用者收到的任務，結合了任務的靜態數據 (Task) 和動態狀態 (state)。
*/
data class UserTask(
    val task: Task,
    var state: String
)

/*
    代表使用者背包中的一個物品，結合了物品的靜態數據 (Item) 和動態數據 (quantity)。
*/
data class UserItem(
    val item: Item,
    var quantity: Int
) {
    // 用於 Compose UI 的可變狀態
    var count: MutableState<Int> = mutableStateOf(quantity)
}
