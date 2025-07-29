package com.ntou01157.hunter.api

import com.google.gson.Gson
import com.ntou01157.hunter.models.User
import okhttp3.OkHttpClient
import okhttp3.Request

object SpotLogApi {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun getSpotLogs(userId: String): Map<String, Boolean>? {
        val url = "${ApiConfig.BASE_URL}/api/user/$userId/spots-log"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null

                // 假設後端回傳格式是：{ "spotsScanLogs": { "moai": true, ... } }
                val jsonObject = gson.fromJson(body, Map::class.java)
                val rawMap = jsonObject["spotsScanLogs"] as? Map<*, *> ?: return null

                // 將 key/value 強制轉型成 Map<String, Boolean>
                rawMap.mapNotNull {
                    val key = it.key as? String
                    val value = it.value as? Boolean
                    if (key != null && value != null) key to value else null
                }.toMap()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
