package com.ntou01157.hunter.models

data class RecognizeResponse(
    val success: Boolean,
    val updatedLogs: Map<String, Boolean>?
)
