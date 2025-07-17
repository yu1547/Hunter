package com.ntou01157.hunter

import com.ntou01157.hunter.models.recipe

val recipes = listOf(
    recipe(
        resultItemId = 2,  //合成id 2的東西
        requiredItems = mapOf(
            3 to 3,  // 需要 id 3 的素材 3 個
            4 to 1   // 需要 id 4 的素材 1 個
        )
    ),
    recipe(
        resultItemId = 1,
        requiredItems = mapOf(
            5 to 3,
        )
    )
)