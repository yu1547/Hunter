package com.ntou01157.hunter.api

import android.util.Log
import com.ntou01157.hunter.models.Supply
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.ntou01157.hunter.temp.*

object SupplyApi {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    private fun authHeader(builder: Request.Builder): Request.Builder {
        val token = TokenManager.idToken
        return if (!token.isNullOrEmpty()) {
            builder.addHeader("Authorization", "Bearer $token")
        } else builder
    }

    // 取得所有補給站
    fun getAll(): List<Supply> {
        val url = "${ApiConfig.BASE_URL}/api/supplies"
        val req = authHeader(Request.Builder().url(url).get()).build()
        return try {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    Log.e("SupplyApi", "HTTP ${resp.code} from $url")
                    return emptyList()
                }
                val body = resp.body?.string() ?: return emptyList()
                val root = JSONObject(body)
                if (!root.optBoolean("success", false)) return emptyList()
                val arr: JSONArray = root.optJSONArray("data") ?: return emptyList()

                val list = ArrayList<Supply>(arr.length())
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    list.add(
                            Supply(
                                    supplyId = o.getString("_id"),
                                    name = o.getString("name"),
                                    latitude = o.getDouble("latitude"),
                                    longitude = o.getDouble("longitude")
                            )
                    )
                }
                list
            }
        } catch (e: Exception) {
            Log.e("SupplyApi", "getAll 失敗", e)
            emptyList()
        }
    }

    data class ClaimResponse(
        val success: Boolean,
        val reason: String? = null,
        val nextClaimTime: String? = null,
        val drops: List<String>? = null
    )

    // 領取補給
    fun claim(userId: String, supplyId: String): ClaimResponse {
        val url = "${ApiConfig.BASE_URL}/api/supplies/claim/$userId/$supplyId"
        val body = "{}".toRequestBody(JSON)
        val req = authHeader(Request.Builder().url(url).post(body)).build()
        return try {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || raw.isEmpty()) {
                    Log.e("SupplyApi", "HTTP ${resp.code} from $url body=$raw")
                    return ClaimResponse(success = false, reason = "HTTP_${resp.code}")
                }
                val jo = JSONObject(raw)
                val success = jo.optBoolean("success", false)
                val reason = jo.optString("reason", null)
                val next = jo.optString("nextClaimTime", null)
                val dropsArr = jo.optJSONArray("drops")
                val drops =
                        if (dropsArr != null) List(dropsArr.length()) { i -> dropsArr.getString(i) }
                        else null
                ClaimResponse(success, reason, next, drops)
            }
        } catch (e: Exception) {
            Log.e("SupplyApi", "claim 失敗", e)
            ClaimResponse(success = false, reason = "NETWORK_ERROR")
        }
    }

    data class DailyEventResponse(
            val success: Boolean,
            val hasEvent: Boolean = false,
            val eventName: String? = null,
            val message: String? = null
    )

    // 新增：查詢特定地點是否有每日事件
    suspend fun getDailyEventForSpot(supplyId: String): DailyEventResponse =
            withContext(Dispatchers.IO) {
                val url = "${ApiConfig.BASE_URL}/api/events/daily-event/$supplyId"
                val req = Request.Builder().url(url).get().build()

                try {
                    client.newCall(req).execute().use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        if (!resp.isSuccessful || raw.isEmpty()) {
                            Log.e("SupplyApi", "HTTP ${resp.code} from $url body=$raw")
                            return@withContext DailyEventResponse(
                                    success = false,
                                    message = "HTTP_${resp.code}"
                            )
                        }
                        val jo = JSONObject(raw)
                        val success = jo.optBoolean("success", false)
                        if (success) {
                            DailyEventResponse(
                                    success = true,
                                    hasEvent = jo.optBoolean("hasEvent", false),
                                    eventName = jo.optString("eventName", null)
                            )
                        } else {
                            DailyEventResponse(
                                    success = false,
                                    message = jo.optString("message", "未知錯誤")
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SupplyApi", "getDailyEventForSpot 失敗", e)
                    DailyEventResponse(success = false, message = "NETWORK_ERROR")
                }
            }
    // 新增：呼叫後端 triggerStonePile API
    suspend fun triggerStonePile(userId: String): ClaimResponse =
            withContext(Dispatchers.IO) {
                val url = "${ApiConfig.BASE_URL}/api/events/trigger-stone-pile"
                val json = JSONObject().apply { put("userId", userId) }.toString()
                val body = json.toRequestBody(JSON)
                val req = Request.Builder().url(url).post(body).build()
                return@withContext try {
                    client.newCall(req).execute().use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        val jo = JSONObject(raw)
                        ClaimResponse(
                                success = jo.optBoolean("success", false),
                                reason = jo.optString("message", "未知錯誤")
                        )
                    }
                } catch (e: Exception) {
                    ClaimResponse(success = false, reason = "NETWORK_ERROR")
                }
            }

    // 新增：呼叫後端 trade API
    suspend fun trade(userId: String, tradeType: String): ClaimResponse =
            withContext(Dispatchers.IO) {
                val url = "${ApiConfig.BASE_URL}/api/events/trade"
                val json =
                        JSONObject()
                                .apply {
                                    put("userId", userId)
                                    put("tradeType", tradeType)
                                }
                                .toString()
                val body = json.toRequestBody(JSON)
                val req = Request.Builder().url(url).post(body).build()
                return@withContext try {
                    client.newCall(req).execute().use { resp ->
                        val raw = resp.body?.string().orEmpty()
                        val jo = JSONObject(raw)
                        ClaimResponse(
                                success = jo.optBoolean("success", false),
                                reason = jo.optString("message", "未知錯誤")
                        )
                    }
                } catch (e: Exception) {
                    ClaimResponse(success = false, reason = "NETWORK_ERROR")
                }
            }

    // 工具：UTC ISO8601 -> millis
    fun parseUtcMillis(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            val fmt =
                    SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                        timeZone = TimeZone.getTimeZone("UTC")
                    }
            fmt.parse(s)?.time
        } catch (_: Exception) {
            null
        }
    }

    data class StatusResponse(
        val success: Boolean,
        val canClaim: Boolean,
        val nextClaimTime: String? = null
    )

    // 查補給站狀態：GET /api/supplies/status/{userId}/{supplyId}
    fun status(userId: String, supplyId: String): StatusResponse {
        val url = "${ApiConfig.BASE_URL}/api/supplies/status/$userId/$supplyId"
        val req = Request.Builder().url(url).get().build()
        return try {
            client.newCall(req).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful || raw.isEmpty()) {
                    Log.e("SupplyApi", "HTTP ${resp.code} from $url body=$raw")
                    return StatusResponse(success = false, canClaim = false)
                }
                val jo = JSONObject(raw)
                StatusResponse(
                    success = jo.optBoolean("success", false),
                    canClaim = jo.optBoolean("canClaim", false),
                    nextClaimTime = jo.optString("nextClaimTime", null)
                )
            }
        } catch (e: Exception) {
            Log.e("SupplyApi", "status 失敗", e)
            StatusResponse(success = false, canClaim = false)
        }
    }
}
