package com.ntou01157.hunter.models

data class CSRChat(
    val message: String,
    val history: List<History>
)

data class History(
    val role: String,
    val content: String,
    val timestamp: String
)