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
                    // 從後端獲取每個任務的詳細資訊
                    val taskInfo = apiService.getTask(mission.taskId)
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
        try {
            val user = apiService.acceptTask(userId, taskId)
            user.missions.find { it.taskId == taskId }?.state
        } catch (e: Exception) {
            Log.e("TaskRepo", "使用者 $userId 接受任務 $taskId 失敗", e)
            null
        }
    }

    suspend fun declineTask(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        try {
            val user = apiService.declineTask(userId, taskId)
            user.missions.find { it.taskId == taskId }?.state
        } catch (e: Exception) {
            Log.e("TaskRepo", "使用者 $userId 拒絕任務 $taskId 失敗", e)
            null
        }
    }

    suspend fun completeTask(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        try {
            val user = apiService.completeTask(userId, taskId)
            user.missions.find { it.taskId == taskId }?.state
        } catch (e: Exception) {
            Log.e("TaskRepo", "使用者 $userId 完成任務 $taskId 失敗", e)
            null
        }
    }

    suspend fun claimReward(userId: String, taskId: String): String? = withContext(Dispatchers.IO) {
        try {
            val response = apiService.claimReward(userId, taskId)
            // 獎勵領取後，該任務狀態會變為 claimed，並可能有新任務出現
            // 直接回傳新狀態即可
            response.user.missions.find { it.taskId == taskId }?.state
        } catch (e: Exception) {
            Log.e("TaskRepo", "使用者 $userId 領取任務 $taskId 獎勵失敗", e)
            null
        }
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
