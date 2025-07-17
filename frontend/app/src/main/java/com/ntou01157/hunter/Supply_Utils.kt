package com.ntou01157.hunter

import com.google.firebase.Timestamp
//補給站冷卻時間計算
// 冷卻時間15分鐘
private const val COOLDOWN_MILLIS = 15 * 60 * 1000L

// 判斷是否可領
fun isSupplyAvailable(nextClaimTime: Timestamp?): Boolean {
    val now = System.currentTimeMillis()
    return nextClaimTime?.toDate()?.time?.let {
        it + COOLDOWN_MILLIS < now
    } ?: true
}

// 剩餘冷卻時間
fun remainingCooldownMillis(nextClaimTime: Timestamp?): Long {
    val now = System.currentTimeMillis()
    return nextClaimTime?.toDate()?.time?.let {
        (it + COOLDOWN_MILLIS) - now
    } ?: 0L
}

// 格式化倒數時間
fun formattedRemainingCooldown(nextClaimTime: Timestamp?): String {
    val totalSeconds = remainingCooldownMillis(nextClaimTime) / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes}分${seconds}秒"
}
