package com.ntou01157.hunter.data

import androidx.compose.runtime.mutableStateListOf
import com.ntou01157.hunter.models.Task

fun getTasks() = mutableStateListOf(
    Task("1", "任務一", "介紹任務一", "簡單", "11111", taskDuration = 3600_000, rewardScore = 10),
    Task("2", "任務二", "介紹任務二", "普通", "22222", taskDuration = 7200_000, rewardScore = 20),
    Task("3", "任務三", "介紹任務三", "困難", "33333", taskDuration = 1800_000, rewardScore = 50)
)
