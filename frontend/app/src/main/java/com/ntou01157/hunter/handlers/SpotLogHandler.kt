package com.ntou01157.hunter.handlers

import com.ntou01157.hunter.api.SpotLogApi

object SpotLogHandler {
    fun getSpotLogs(userId: String): Map<String, Boolean> {
        return SpotLogApi.getSpotLogs(userId) ?: emptyMap()
    }
}
