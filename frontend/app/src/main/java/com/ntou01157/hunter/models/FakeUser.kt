package com.ntou01157.hunter.mock

import com.google.firebase.Timestamp
import com.ntou01157.hunter.models.*

val FakeUser =
        User(
                uid = "68a48da731f22c76b7a5f52c",
                displayName = "測試用戶",
                email = "test@example.com",
                age = "20",
                gender = "男",
                photoURL = "",
                role = "user",
                score = 100.0,
                backpackItems =
                        listOf(
                                BackpackItem(itemId = "1", quantity = 2),
                                BackpackItem(itemId = "3", quantity = 5)
                        ),
                supplyScanLogs =
                        mutableMapOf("station1" to Timestamp.now(), "station2" to Timestamp.now()),
                settings = Settings(music = true, notification = true, language = "zh-TW")
        )
