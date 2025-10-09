package com.ntou01157.hunter.api

import android.util.Log
import com.ntou01157.hunter.models.Spot
import com.ntou01157.hunter.temp.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

// 新增：導入 Task 相關模型
import com.ntou01157.hunter.models.model_api.CheckPlaces
import com.ntou01157.hunter.models.model_api.RewardItem
import com.ntou01157.hunter.models.model_api.Task
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
    // --- ✅ 已完成 getUserTasks 的實作 ---
    fun getUserTasks(userId: String): List<Task> {
        val url = "${ApiConfig.BASE_URL}/api/tasks/user/$userId"

        val token = TokenManager.idToken
        if (token.isNullOrEmpty()) {
            Log.e("SpotApi", "❌ 沒有 Token，無法呼叫 getUserTasks API")
            return emptyList()
        }

        val request = Request.Builder()
            .url(url)
            .get()
            .addHeader("Authorization", "Bearer $token")
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e("SpotApi", "getUserTasks 錯誤: HTTP ${response.code} from $url")
                    return emptyList()
                }
                val body = response.body?.string() ?: return emptyList()
                val root = JSONObject(body)

                if (!root.optBoolean("success", false)) {
                    Log.e("SpotApi", "getUserTasks 回應 success=false")
                    return emptyList()
                }

                val tasksArray = root.optJSONArray("tasks") ?: return emptyList()
                val tasksList = mutableListOf<Task>()

                for (i in 0 until tasksArray.length()) {
                    val taskJson = tasksArray.getJSONObject(i)

                    // 解析 nested checkPlaces
                    val checkPlacesArray = taskJson.optJSONArray("checkPlaces")
                    val checkPlacesList = mutableListOf<CheckPlaces>()
                    if (checkPlacesArray != null) {
                        for (j in 0 until checkPlacesArray.length()) {
                            val placeJson = checkPlacesArray.getJSONObject(j)
                            checkPlacesList.add(
                                CheckPlaces(spotId = placeJson.getString("spotId"))
                            )
                        }
                    }

                    // 解析 nested rewardItems
                    val rewardItemsArray = taskJson.optJSONArray("rewardItems")
                    val rewardItemsList = mutableListOf<RewardItem>()
                    if (rewardItemsArray != null) {
                        for (j in 0 until rewardItemsArray.length()) {
                            val itemJson = rewardItemsArray.getJSONObject(j)
                            rewardItemsList.add(
                                RewardItem(
                                    itemId = itemJson.getString("itemId"),
                                    quantity = itemJson.getInt("quantity")
                                )
                            )
                        }
                    }

                    val task = Task(
                        taskId = taskJson.getString("_id"),
                        taskName = taskJson.getString("taskName"),
                        taskDescription = taskJson.optString("taskDescription", null),
                        taskDifficulty = taskJson.optString("taskDifficulty", null),
                        taskTarget = taskJson.getString("taskTarget"),
                        checkPlaces = checkPlacesList,
                        taskDuration = taskJson.optLong("taskDuration", 0L).takeIf { it > 0 },
                        rewardItems = rewardItemsList,
                        rewardScore = taskJson.getInt("rewardScore"),
                        isLLM = taskJson.optBoolean("isLLM", false)
                    )
                    tasksList.add(task)
                }
                Log.i("SpotApi", "成功為使用者 $userId 取得 ${tasksList.size} 個任務")
                tasksList
            }
        } catch (e: Exception) {
            Log.e("SpotApi", "getUserTasks 失敗 (userId: $userId)", e)
            emptyList()
        }
    }
}
