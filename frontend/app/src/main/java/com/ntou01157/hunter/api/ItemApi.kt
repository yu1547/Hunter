package com.ntou01157.hunter.api

import android.util.Log
import com.ntou01157.hunter.models.model_api.UseResult
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext



object ItemApi {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    data class UseItemResp(
        val success: Boolean,
        val duplicate: Boolean,
        val message: String?,
        val backpackItems: List<BackpackEntry>?,
        val buff: JSONArray?,
        val effects: JSONArray?
    )
    data class BackpackEntry(val itemId: String, val quantity: Int)

    fun generateRequestId(userId: String, itemId: String): String {
        // 每次按一次使用就生成一次
        return "use:$userId:$itemId:${UUID.randomUUID()}"
    }

    suspend fun useItem(userId: String, itemId: String, requestId: String): UseResult =
        withContext(Dispatchers.IO) {
            val url = "${ApiConfig.BASE_URL}/api/items/use/$userId/$itemId"
            val body = JSONObject().put("requestId", requestId).toString().toRequestBody(JSON)
            val req = Request.Builder().url(url).post(body).build()
            try {
                client.newCall(req).execute().use { resp ->
                    val raw = resp.body?.string().orEmpty()
                    val code = resp.code
                    val msgFromErr = try { JSONObject(raw).optString("message", null) } catch (_: Exception) { null }
                    val isDupFromErr = (code == 409) || msgFromErr == "DUPLICATE_REQUEST"

                    if (!resp.isSuccessful || raw.isEmpty()) {
                        Log.e("ItemApi", "HTTP $code from $url body=$raw")
                        return@withContext UseResult(false, msgFromErr ?: "HTTP_$code", isDupFromErr, null)
                    }
                    val jo = JSONObject(raw)
                    val ok = jo.optBoolean("success", false)
                    val msg = jo.optString("message", null)
                    val isDup = (code == 409) || msg == "DUPLICATE_REQUEST"
                    val effects = jo.optJSONArray("effects")
                    UseResult(ok, msg, isDup, effects)
                }
            } catch (e: Exception) {
                Log.e("ItemApi", "useItem 失敗", e)
                UseResult(false, "NETWORK_ERROR", false, null)
            }
        }

}
