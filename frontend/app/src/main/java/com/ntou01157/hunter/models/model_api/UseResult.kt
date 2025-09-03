package com.ntou01157.hunter.models.model_api

import org.json.JSONArray

data class UseResult(
    val success: Boolean,
    val message: String? = null,
    val duplicate: Boolean = false,
    val effects: JSONArray? = null
)
