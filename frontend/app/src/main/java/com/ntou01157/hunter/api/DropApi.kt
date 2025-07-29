package com.ntou01157.hunter.api
//掉落物api
import com.ntou01157.hunter.models.DropResult
import com.google.gson.Gson
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody


object DropApi {
    private val client = OkHttpClient()
    private val gson = Gson()

    fun getDrop(userId: String, difficulty: Int): DropResult? {
        val url = "${ApiConfig.BASE_URL}/api/drop/$userId/$difficulty"

        val request = Request.Builder()
            .url(url)
            .post(RequestBody.create(null, "")) // 空的 POST
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                gson.fromJson(body, DropResult::class.java)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
