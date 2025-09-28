package com.ntou01157.hunter.api

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.ntou01157.hunter.temp.*

object SpotLogApi {
    private val client = OkHttpClient()

    suspend fun getSpotLogs(userId: String): Map<String, Boolean>? = withContext(Dispatchers.IO) {
        val url = "${ApiConfig.BASE_URL}/api/spots/$userId"

        val token = TokenManager.idToken
        if (token.isNullOrEmpty()) {
            Log.e("SpotLogApi", "❌ 沒有 Token，無法呼叫 API")
            return@withContext null
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token") // ✅ 加上 Token
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotLogApi", "HTTP 錯誤碼: ${response.code}")
                    return@withContext null
                }

                val body = response.body?.string() ?: run {
                    Log.e("SpotLogApi", "回傳 body 為空")
                    return@withContext null
                }

                val json = JSONObject(body)

                if (!json.optBoolean("success", false)) {
                    Log.e("SpotLogApi", "success = false")
                    return@withContext null
                }

                val scanLogsJson = json.optJSONObject("spotsScanLogs") ?: run {
                    Log.e("SpotLogApi", "spotsScanLogs 不存在")
                    return@withContext null
                }

                val result = mutableMapOf<String, Boolean>()
                for (key in scanLogsJson.keys()) {
                    result[key] = scanLogsJson.optBoolean(key, false)
                }

                Log.i("SpotLogApi", "解析成功：$result")
                return@withContext result
            }
        } catch (e: Exception) {
            Log.e("SpotLogApi", "getSpotLogs 失敗", e)
            return@withContext null
        }
    }
}
