package com.ntou01157.hunter.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object SpotLogApi {
    private val client = OkHttpClient()

    fun getSpotLogs(userId: String): Map<String, Boolean>? {
        val url = "${ApiConfig.BASE_URL}/api/spots/$userId"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotLogApi", "HTTP 錯誤碼: ${response.code}")
                    return null
                }

                val body = response.body?.string() ?: run {
                    Log.e("SpotLogApi", "回傳 body 為空")
                    return null
                }

                val json = JSONObject(body)

                if (!json.optBoolean("success", false)) {
                    Log.e("SpotLogApi", "success = false")
                    return null
                }

                val scanLogsJson = json.optJSONObject("spotsScanLogs") ?: run {
                    Log.e("SpotLogApi", "spotsScanLogs 不存在")
                    return null
                }

                val result = mutableMapOf<String, Boolean>()
                for (key in scanLogsJson.keys()) {
                    result[key] = scanLogsJson.optBoolean(key, false)
                }

                Log.i("SpotLogApi", "解析成功：$result")
                return result
            }
        } catch (e: Exception) {
            Log.e("SpotLogApi", "getSpotLogs 失敗", e)
            return null
        }
    }

}
