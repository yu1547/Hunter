// TaskData.kt
package com.ntou01157.hunter

// 時間格式轉換
fun formatMillis(millis: Long): String {
    val hours = millis / 3600000
    val minutes = (millis / 60000) % 60
    val seconds = (millis / 1000) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
