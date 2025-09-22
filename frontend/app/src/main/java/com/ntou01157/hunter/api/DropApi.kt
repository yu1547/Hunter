package com.ntou01157.hunter.api

import com.ntou01157.hunter.models.DropResult
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import com.ntou01157.hunter.temp.*

object DropApi {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun getDrop(userId: String, difficulty: Int): DropResult? {
        val url = "${ApiConfig.BASE_URL}/api/drop/$userId/$difficulty"

        val builder = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, "")) // 空的 POST

        // ✅ 帶上 Firebase Token
        TokenManager.idToken?.let { token ->
            builder.addHeader("Authorization", "Bearer $token")
        }

        val request = builder.build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    println("DropApi Error: HTTP ${response.code}")
                    return null
                }
                val body = response.body?.string() ?: return null
                gson.fromJson(body, DropResult::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
