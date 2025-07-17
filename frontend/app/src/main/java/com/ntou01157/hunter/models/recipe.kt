package com.ntou01157.hunter.models

data class recipe(
    val resultItemId: Int,
    val requiredItems: Map<Int, Int> // itemId -> count
)

