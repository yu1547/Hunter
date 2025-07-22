package com.ntou01157.hunter.mock

import com.google.firebase.Timestamp
import com.ntou01157.hunter.models.*

val FakeUser = User(
    uid = "6879fdbc125a5443a1d4bade",
    displayName = "測試用戶",
    email = "test@example.com",
    age = "20",
    gender = "男",
    photoURL = "",
    role = "user",
    score = 100.0,
    backpackItems = listOf(
        BackpackItem(itemId = "1", quantity = 2),
        BackpackItem(itemId = "3", quantity = 5)
    ) ,
    supplyScanLogs = mapOf(
        "station1" to SupplyScanLog(
            spotId = "station1",
            nextClaimTime = Timestamp.now() // 設現在冷卻完成，可以領
        ),
        "station2" to SupplyScanLog(
            spotId = "station2",
            nextClaimTime = Timestamp.now()
        )
    ),
    settings = Settings(
        music = true,
        notification = true,
        language = "zh-TW"
    )

)
