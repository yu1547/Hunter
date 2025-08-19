package com.ntou01157.hunter.handlers

import com.ntou01157.hunter.api.SpotApi
import com.ntou01157.hunter.api.SpotLogApi
import com.ntou01157.hunter.models.Spot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SpotLogHandler {

    suspend fun getSpotLogs(userId: String): Map<String, Boolean> {
        return withContext(Dispatchers.IO) {
            SpotLogApi.getSpotLogs(userId) ?: emptyMap()
        }
    }


    suspend fun getSpotPages(): List<List<Spot>> {
        return withContext(Dispatchers.IO) {
            val spots = SpotApi.getAllSpots()
            println("取得 Spot 數量：${spots.size}")
            spots.chunked(4)
        }
    }
}
