package com.ntou01157.hunter

data class Recipe(
    val resultItemId: Int,
    val requiredItems: Map<Int, Int> // itemId -> count
)

