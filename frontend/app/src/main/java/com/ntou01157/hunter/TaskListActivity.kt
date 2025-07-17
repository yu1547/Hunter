// TaskData.kt
package com.ntou01157.hunter

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf

// 資料類別（功能邏輯會用到）
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val reward: String,
    val initialTimeMillis: Long,
    var isAccepted: Boolean = false,
    val remainingTimeMillis: MutableState<Long> = mutableStateOf(0L)
)

// 時間格式轉換
fun formatMillis(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis / 60000) % 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
