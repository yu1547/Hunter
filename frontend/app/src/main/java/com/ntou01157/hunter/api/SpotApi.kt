package com.ntou01157.hunter.api

import android.util.Log
import com.ntou01157.hunter.models.Spot
import com.ntou01157.hunter.temp.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

object SpotApi {
    private val client = OkHttpClient()

    fun getAllSpots(): List<Spot> {
        val url = "${ApiConfig.BASE_URL}/api/spots"

        val token = TokenManager.idToken
        if (token.isNullOrEmpty()) {
            Log.e("SpotApi", "❌ 沒有 Token，無法呼叫 API")
            return emptyList()
        }

        val request =
                Request.Builder()
                        .url(url)
                        .get()
                        .addHeader("Authorization", "Bearer $token") // ✅ 加上 Token
                        .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotApi", "HTTP ${response.code} from $url")
                    return emptyList()
                }

                val body = response.body?.string()
                if (body.isNullOrEmpty()) {
                    Log.e("SpotApi", "Empty body from $url")
                    return emptyList()
                }

                val root = JSONObject(body)
                if (!root.optBoolean("success", false)) {
                    Log.e("SpotApi", "success=false in response")
                    return emptyList()
                }

                val arr: JSONArray =
                        root.optJSONArray("spots")
                                ?: run {
                                    Log.e("SpotApi", "spots array not found")
                                    return emptyList()
                                }

                val list = ArrayList<Spot>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val spotId = o.optString("_id", "")
                    list.add(
                            Spot(
                                    spotId = spotId,
                                    spotName = o.getString("spotName"),
                                    ChName = o.optString("ChName"),
                                    latitude = o.getDouble("latitude"),
                                    longitude = o.getDouble("longitude"),
                                    description = o.optString("description", "")
                            )
                    )
                }

                Log.i("SpotApi", "取得 Spot 數量：${list.size}")
                list
            }
        } catch (e: Exception) {
            Log.e("SpotApi", "getAllSpots 失敗", e)
            emptyList()
        }
    }
    fun getUserTasks(userId: String): List<Task> {
        val url = "${ApiConfig.BASE_URL}/api/tasks/user/$userId"
        // ... 實作完整的網路請求邏輯，類似 SpotApi.getAllSpots
        // 成功時，解析 JSON 並回傳 List<Task>
        // 失敗時，回傳 emptyList()
        return emptyList() // 這裡先放假的回傳值
    }
}
