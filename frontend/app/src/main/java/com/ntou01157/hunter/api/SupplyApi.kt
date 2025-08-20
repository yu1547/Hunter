package com.ntou01157.hunter.api

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import com.ntou01157.hunter.models.Supply
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

object SupplyApi {
    private val client = OkHttpClient()
    private val JSON = "application/json; charset=utf-8".toMediaType()

    // 取得所有補給站
    fun getAll(): List<Supply> {
        val url = "${ApiConfig.BASE_URL}/api/supplies"
        val req = Request.Builder().url(url).get().build()
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
        val nextClaimTime: String? = null, // ISO8601 UTC
        val drops: List<String>? = null
    )

    // 領取補給：POST /api/supplies/{userId}/{supplyId}/claim
    fun claim(userId: String, supplyId: String): ClaimResponse {
        val url = "${ApiConfig.BASE_URL}/api/supplies/claim/$userId/$supplyId"
        val body = "{}".toRequestBody(JSON) // 無 body，保持 POST
        val req = Request.Builder().url(url).post(body).build()
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
                val drops = if (dropsArr != null) List(dropsArr.length()) { i -> dropsArr.getString(i) } else null
                ClaimResponse(success, reason, next, drops)
            }
        } catch (e: Exception) {
            Log.e("SupplyApi", "claim 失敗", e)
            ClaimResponse(success = false, reason = "NETWORK_ERROR")
        }
    }

    // 工具：UTC ISO8601 -> millis
    fun parseUtcMillis(s: String?): Long? {
        if (s.isNullOrBlank()) return null
        return try {
            val fmt = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            fmt.parse(s)?.time
        } catch (_: Exception) { null }
    }
}
