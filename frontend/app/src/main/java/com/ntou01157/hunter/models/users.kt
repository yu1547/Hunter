package com.ntou01157.hunter.models

import com.google.firebase.Timestamp

data class User (
    val uid: String,
    val displayName: String,
    val email: String,
    val age: String,
    val gender: String,
    val createdAt: Timestamp = Timestamp.now(),
    val lastLogin: Timestamp = Timestamp.now(),
    val photoURL: String,
    val role: String,
    val score: Double,
    val backpackItems: List<BackpackItem> = emptyList(),
    val missions: List<Mission> = emptyList(),
    val refreshAt: Timestamp = Timestamp.now(),
    val spotsScanLogs: Map<String, Boolean> = emptyMap(),
    val supplyScanLogs: Map<String, Timestamp> = emptyMap(),
    val settings: Settings = Settings(language = "zh-TW"),
    val buff: Map<String, Int> = emptyMap(),
)

data class BackpackItem(
    val itemId: String,
    val quantity: Int
)

data class Mission(
    val taskId: String,
    val state: String,
    val acceptedAt: Timestamp = Timestamp.now(),
    val expireAt: Timestamp = Timestamp.now()
)

data class SpotScanLogs(
    val isCheck: Boolean = false
)

data class SupplyScanLog(
    val spotId: String,
    val nextClaimTime: Timestamp = Timestamp(946684800, 0) // 2000-01-01T00:00:00Z
)

data class Settings(
    val music: Boolean = false,
    val notification: Boolean = false,
    val language: String
)

data class Buff(
    val isBuff: Int = 0
)
