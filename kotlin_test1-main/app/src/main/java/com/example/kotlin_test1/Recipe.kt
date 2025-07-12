package com.example.kotlin_test1

data class Recipe(
    val resultItemId: Int,
    val requiredItems: Map<Int, Int> // itemId -> count
)

