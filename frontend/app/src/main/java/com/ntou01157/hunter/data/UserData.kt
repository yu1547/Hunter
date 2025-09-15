package com.ntou01157.hunter.data

import com.ntou01157.hunter.models.Mission
import com.ntou01157.hunter.models.Settings
import com.ntou01157.hunter.models.User
import com.google.firebase.Timestamp
import com.ntou01157.hunter.models.Buff

fun getTestUser(): User {
    return User(
        uid = "68886402bc049f83948150e8",
        displayName = "測試用戶",
        email = "test@example.com",
        age = "25",
        gender = "男",
        photoURL = "https://example.com/photo.jpg",
        role = "player",
        score = 100.0,
        backpackItems = emptyList(),
        missions = listOf(
            Mission(
                taskId = "1",
                state = "available",
                acceptedAt = Timestamp.now(),
                expiresAt = Timestamp.now(),
                refreshedAt = Timestamp.now(),
                haveCheckPlaces = emptyList()
            )
        ),
        spotsScanLogs = emptyMap(),
        supplyScanLogs = mutableMapOf(),
        settings = Settings(
            music = true,
            notification = true,
            language = "zh-TW"
        ),
        buff = null
    )
}
