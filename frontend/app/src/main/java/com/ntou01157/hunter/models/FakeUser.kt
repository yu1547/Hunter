package com.ntou01157.hunter.mock

import com.ntou01157.hunter.models.*

val FakeUser = User(
    uid = "123",
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
    )
)
