package com.ntou01157.hunter

import com.google.firebase.Timestamp
import java.util.concurrent.TimeUnit

fun isSupplyAvailable(nextClaimTime: Timestamp?): Boolean {
    val now = Timestamp.now()
    return nextClaimTime == null || now.seconds >= nextClaimTime.seconds
}

fun formattedRemainingCooldown(nextClaimTime: Timestamp?): String {
    if (nextClaimTime == null) return "0 秒"
    val now = Timestamp.now()
    val remainingSeconds = nextClaimTime.seconds - now.seconds
    if (remainingSeconds <= 0) return "0 秒"

    val minutes = TimeUnit.SECONDS.toMinutes(remainingSeconds)
    val seconds = remainingSeconds % 60
    return "${minutes}分 ${seconds}秒"
}
