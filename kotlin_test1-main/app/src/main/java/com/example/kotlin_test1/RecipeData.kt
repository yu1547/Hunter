package com.example.kotlin_test1

val recipes = listOf(
    Recipe(
        resultItemId = 2,  //合成id 2的東西
        requiredItems = mapOf(
            3 to 3,  // 需要 id 3 的素材 3 個
            4 to 1   // 需要 id 4 的素材 1 個
        )
    ),
    Recipe(
        resultItemId = 1,
        requiredItems = mapOf(
            5 to 3,
        )
    )
)