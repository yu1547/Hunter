package com.ntou01157.hunter.data

import android.util.Log
import com.ntou01157.hunter.api.CreateLLMMissionRequest
import com.ntou01157.hunter.api.Location
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.UserTask
import com.ntou01157.hunter.models.model_api.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TaskRepository {
    private val apiService = RetrofitClient.apiService

    suspend fun refreshAndGetTasks(userId: String): List<UserTask> = withContext(Dispatchers.IO) {
        try {
            val user = apiService.refreshAllMissions(userId)
            val taskDetailsList = user.missions.mapNotNull { mission ->
                try {
                    val taskInfo: Task = if (mission.isLLM) {
                        // 這是 LLM 任務，從 task endpoint 獲取
                        apiService.getTask(mission.taskId)
                    } else {
                        // 這是一般任務(事件)，從 event endpoint 獲取
                        val event = apiService.getEventById(mission.taskId)
                        // 將 EventModel 轉換為 Task 模型
                        Task(
                            taskId = event.id,
                            taskName = event.name,
                            taskDescription = event.description,
                            taskTarget = event.type, // 使用 event type 作為 target
                            taskDifficulty = "easy",
                            taskDuration = null, // 事件沒有持續時間
                            rewardScore = event.rewards?.points ?: 0,
                            isLLM = false
                        )
                    }
                    UserTask(task = taskInfo, state = mission.state)
                } catch (e: Exception) {
                    Log.e("TaskRepo", "獲取任務 ${mission.taskId} 詳細資訊失敗", e)
                    null
                }
            }
            Log.d("TaskRepo", "為使用者 $userId 獲取了 ${taskDetailsList.size} 個任務")
            return@withContext taskDetailsList
        } catch (e: Exception) {
            Log.e("TaskRepo", "為使用者 $userId 刷新任務失敗", e)
            throw e
        }
    }

    suspend fun acceptTask(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        // 直接拋出例外給呼叫端處理，避免吞錯
        val user = apiService.acceptTask(userId, taskId)
        user.missions.find { it.taskId == taskId }?.state
    }

    suspend fun declineTask(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        // 直接拋出例外給呼叫端處理，避免吞錯
        val user = apiService.declineTask(userId, taskId)
        user.missions.find { it.taskId == taskId }?.state
    }

    suspend fun completeTask(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        // 直接拋出例外給呼叫端處理，避免吞錯
        val user = apiService.completeTask(userId, taskId)
        user.missions.find { it.taskId == taskId }?.state
    }

    suspend fun claimReward(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        // 直接拋出例外給呼叫端處理，避免吞錯
        val response = apiService.claimReward(userId, taskId)
        response.user.missions.find { it.taskId == taskId }?.state
    }

    suspend fun createLLMMission(userId: String, latitude: Double, longitude: Double): List<UserTask> = withContext(Dispatchers.IO) {
        try {
            val request = CreateLLMMissionRequest(userLocation = Location(latitude, longitude))
            apiService.createLLMMission(userId, request)
            // 創建後立即刷新任務列表
            return@withContext refreshAndGetTasks(userId)
        } catch (e: Exception) {
            Log.e("TaskRepo", "為使用者 $userId 創建 LLM 任務失敗", e)
            throw e
        }
    }
}