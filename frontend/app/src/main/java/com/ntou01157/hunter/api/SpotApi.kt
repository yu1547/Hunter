package com.ntou01157.hunter.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import com.ntou01157.hunter.models.Spot

object SpotApi {
    private val client = OkHttpClient()

    fun getAllSpots(): List<Spot> {
        val url = "${ApiConfig.BASE_URL}/api/spots"
        val request = Request.Builder()
            .url(url)
            .get()
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

                // 後端格式：{ "success": true, "spots": [ ... ] }
                val root = JSONObject(body)
                if (!root.optBoolean("success", false)) {
                    Log.e("SpotApi", "success=false in response")
                    return emptyList()
                }

                val arr: JSONArray = root.optJSONArray("spots") ?: run {
                    Log.e("SpotApi", "spots array not found")
                    return emptyList()
                }

                val list = ArrayList<Spot>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)

                    // 你的 _id 是字串（不是 {"$oid": "..."}）
                    val spotId = o.optString("_id", "")

                    list.add(
                        Spot(
                            spotId = spotId,
                            spotName = o.getString("spotName"),
                            ChName = o.getString("ChName"),
                            latitude = o.getDouble("latitude"),
                            longitude = o.getDouble("longitude")
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
}
