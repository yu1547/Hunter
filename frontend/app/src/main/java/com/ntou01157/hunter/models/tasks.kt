package com.ntou01157.hunter.models

import com.google.firebase.Timestamp

data class Tasks (
    val taskId: String,
    val taskName: String,
    val taskDescription: String,
    val taskDifficulty: String,
    val taskTarget: String,
    val taskState: String,
    val checkPlaces: List<CheckPlace> = emptyList(),
    val taskDuraion: Int,
    val rewardItems: List<RewardItem> = emptyList(),
    val rewardScore: Int
)

data class CheckPlace(
    val spotId: String,
    val arrivedAt: Timestamp = Timestamp.now(),
    val isCheck: Boolean = false
)

data class RewardItem(
    val itemId: String,
    val quantity: Int
)