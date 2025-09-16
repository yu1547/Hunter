package com.ntou01157.hunter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ntou01157.hunter.R
import com.ntou01157.hunter.formatMillis
import com.ntou01157.hunter.models.model_api.UserTask
import com.ntou01157.hunter.data.TaskRepository
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.api.RetrofitClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import android.util.Log
import retrofit2.HttpException // 新增：用來判斷並解析 HTTP 錯誤

@Composable
fun TaskListScreen(navController: NavController) {
    // 1) 以 email 解析 userId —— 用狀態保存，避免重組造成的再次請求
    var userId by remember { mutableStateOf<String?>(null) }
    var resolvingId by remember { mutableStateOf(true) } //可以改false
    var resolveError by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 2) 用目前登入者 email 向後端查 userId
    LaunchedEffect(Unit) {
        try {
            resolvingId = true
            resolveError = null

            val email = FirebaseAuth.getInstance().currentUser?.email
                ?: run {
                    resolveError = "尚未登入，無法取得 Email"
                    return@LaunchedEffect
                }

            // --- 依你的 ApiService 定義選擇一種 ---
            // (A) 若 getUserByEmail 回傳「單一 User 物件」
            val user = RetrofitClient.apiService.getUserByEmail(email)
            userId = user.id

            // (B) 若回傳「List<User>」，請改用以下寫法
            // val users = RetrofitClient.apiService.getUserByEmail(email)
            // val user = users.firstOrNull() ?: run {
            //     resolveError = "找不到使用者：$email"
            //     return@LaunchedEffect
            // }
            // userId = user.id
            // --------------------------------------

            Log.d("TaskListScreen", "以 email=$email 取得 userId=$userId")
        } catch (e: Exception) {
            resolveError = "以 Email 取得使用者 ID 失敗：${e.message}"
            Log.e("TaskListScreen", "resolve userId error", e)
        } finally {
            resolvingId = false
        }
        // userId = "68886402bc049f83948150e8"
        // resolvingId = false
        // resolveError = null
    }

    // 3) 任務列表 UI 狀態
    val userTaskList = remember { mutableStateListOf<UserTask>() }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 4) 依 userId 載入任務清單（初次/重試皆可呼叫）
    fun refreshTasks(uid: String) {
        coroutineScope.launch {
            isLoading = true
            errorMessage = null
            try {
                Log.d("TaskListScreen", "開始載入任務，用戶ID: $uid")
                val tasks = TaskRepository.refreshAndGetTasks(uid)
                Log.d("TaskListScreen", "任務數量=${tasks.size}")
                userTaskList.clear()
                userTaskList.addAll(tasks)
                if (tasks.isEmpty()) {
                    errorMessage = "目前沒有任務"
                }
            } catch (e: Exception) {
                val detail = if (e is HttpException) {
                    val code = e.code()
                    val rawBody = try { e.response()?.errorBody()?.string() } catch (ee: Exception) { null }
                    "HTTP $code ${rawBody ?: "(無錯誤內容)"}"
                } else {
                    e.message ?: e.toString()
                }
                errorMessage = "無法載入任務：$detail"
                Log.e("TaskListScreen", "載入任務失敗 userId=$uid detail=$detail", e)
            } finally {
                isLoading = false
            }
        }
    }

    // 5) 只有在拿到 userId 後才載入任務
    LaunchedEffect(userId) {
        userId?.let { refreshTasks(it) }
    }

    // 6) 互動對話框狀態
    var selectedUserTask by remember { mutableStateOf<UserTask?>(null) }
    var showMessageDialog by remember { mutableStateOf<String?>(null) }

    // 路線規劃 LLM 敘述 Dialog 狀態
    var showRouteDialog by remember { mutableStateOf(false) }
    var routeLLMText by remember { mutableStateOf("") }

    // 假設你有兩個地點選擇 start/end
    var startLocation by remember { mutableStateOf(" ") }
    var endLocation by remember { mutableStateOf(" ") }

    // 新增：呼叫 LLM API 取得貼心提示（只生成溫馨提示，不描述地點）
    suspend fun requestRouteLLMDescription(start: String, end: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val message = "請用繁體中文給我一段貼心提示，提醒在限時內完成任務，並鼓勵玩家旅途順利，給予信心喊話和注意安全。"
                val response = RetrofitClient.apiService.chatWithLLM(
                    userId ?: "",
                    com.ntou01157.hunter.api.ChatRequest(
                        message = message,
                        history = emptyList()
                    )
                )
                response.reply
            } catch (e: Exception) {
                "無法取得貼心提示：${e.message}"
            }
        }
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 25.dp, start = 16.dp)
                        .clickable { navController.navigate("main") }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.home_icon),
                        contentDescription = "回首頁",
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text("任務清單", fontSize = 22.sp)
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF3E5E5))
        ) {
            // 先處理「解析 userId」這層狀態
            when {
                resolvingId -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("正在取得使用者資訊…", color = Color.Gray)
                    }
                }
                resolveError != null -> {
                    Column(
                        Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(resolveError ?: "取得使用者資訊失敗", color = Color.Red)
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = {
                            // 重新嘗試解析 userId
                            resolvingId = true
                            resolveError = null
                            userId = null
                            // 註：LaunchedEffect(Unit) 本身不會重跑，
                            // 若要就地重跑，可把「解析 userId」抽成函式直接呼叫，
                            // 或用一個隱藏的 state key 觸發。
                        }) {
                            Text("重試")
                        }
                    }
                }
                userId == null -> {
                    // 極少數保底狀態
                    Text("無使用者 ID，請重新登入", color = Color.Red, modifier = Modifier.align(Alignment.Center))
                }
                else -> {
                    // 顯示任務列表 + 產生 LLM 任務按鈕 (修正原本未定義 llmTasks、item 範圍錯誤與語法錯誤)
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        ) {
                            // 只計算 isLLM 任務數量，若不足 3 顯示按鈕
                            val llmCount = userTaskList.count { it.task.isLLM && it.state != "claimed" }
                            if (llmCount < 3) {
                                Button(
                                    onClick = {
                                        coroutineScope.launch {
                                            val uid = userId
                                            if (uid == null) {
                                                showMessageDialog = "尚未取得使用者 ID"
                                                return@launch
                                            }
                                            isLoading = true
                                            errorMessage = null
                                            try {
                                                // TODO: 將經緯度改為實際使用者位置
                                                val newTasks = TaskRepository.createLLMMission(uid, 25.017, 121.542)
                                                userTaskList.clear()
                                                userTaskList.addAll(newTasks)
                                                showMessageDialog = "已生成新的探索任務！"
                                            } catch (e: Exception) {
                                                errorMessage = "生成任務失敗：${e.message}"
                                            } finally {
                                                isLoading = false
                                            }
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) { Text("生成探索任務") }
                                Spacer(Modifier.height(12.dp))
                            }

                            if (errorMessage != null) {
                                Text(errorMessage ?: "", color = Color.Red)
                                Spacer(Modifier.height(8.dp))
                                Button(onClick = { userId?.let { refreshTasks(it) } }) { Text("重試") }
                                Spacer(Modifier.height(12.dp))
                            }

                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(userTaskList) { userTask ->
                                    TaskItem(userTask) { selectedUserTask = it }
                                }
                            }
                        }
                    }
                }
            }
        }

        // 任務詳情與操作對話框
        selectedUserTask?.let { userTask ->
            TaskDialog(
                userTask = userTask,
                onDismiss = { selectedUserTask = null },
                onAction = { action ->
                    coroutineScope.launch {
                        val uid = userId
                        if (uid == null) {
                            showMessageDialog = "尚未取得使用者 ID"
                            selectedUserTask = null
                            return@launch
                        }

                        val taskId = userTask.task.taskId
                        var success = false
                        var message = ""

                        try {
                            when (action) {
                                "accept" -> {
                                    val newState = TaskRepository.acceptTask(uid, taskId)
                                    if (newState != null) {
                                        val idx = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                        if (idx != -1) {
                                            userTaskList[idx] = userTaskList[idx].copy(state = newState)
                                        }
                                        message = "任務已接受！"
                                        success = true
                                    }
                                }
                                "decline" -> {
                                    TaskRepository.declineTask(uid, taskId)
                                    val idx = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                    if (idx != -1) {
                                        userTaskList[idx] = userTaskList[idx].copy(state = "declined")
                                    }
                                    message = "任務已拒絕"
                                    success = true
                                }
                                "complete" -> {
                                    val newState = TaskRepository.completeTask(uid, taskId)
                                    if (newState != null) {
                                        val idx = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                        if (idx != -1) {
                                            userTaskList[idx] = userTaskList[idx].copy(state = newState)
                                        }
                                        message = "任務已完成！"
                                        success = true
                                    }
                                }
                                "claim" -> {
                                    TaskRepository.claimReward(uid, taskId)
                                    message = "獎勵已領取！"
                                    refreshTasks(uid)
                                    success = true
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("TaskListScreen", "任務操作失敗", e)
                            success = false
                        }

                        showMessageDialog = if (success) message else "操作失敗"
                        selectedUserTask = null
                    }
                }
            )
        }

        // 操作結果訊息
        showMessageDialog?.let { msg ->
            AlertDialog(
                onDismissRequest = { showMessageDialog = null },
                title = { Text("通知") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { showMessageDialog = null }) { Text("確定") }
                }
            )
        }

            // // 路線規劃按鈕不使用 align，改用 Column + Spacer 讓按鈕在右下角
            // Column(
            //     modifier = Modifier
            //         .fillMaxSize()
            //         .padding(24.dp),
            //     verticalArrangement = Arrangement.Bottom,
            //     horizontalAlignment = Alignment.End
            // ) {
            //     Button(
            //         onClick = {
            //             coroutineScope.launch {
            //                 routeLLMText = requestRouteLLMDescription(startLocation, endLocation)
            //                 showRouteDialog = true
            //             }
            //         }
            //     ) {
            //         Text("接受路線規劃")
            //     }
            // }

            // // 顯示 LLM 貼心提示 Dialog
            // if (showRouteDialog) {
            //     AlertDialog(
            //         onDismissRequest = { showRouteDialog = false },
            //         title = { Text("貼心提示") },
            //         text = { Text(routeLLMText) },
            //         confirmButton = {
            //             TextButton(onClick = { showRouteDialog = false }) { Text("確定") }
            //         }
            //     )
            // }
        }
    }

@Composable
fun TaskItem(userTask: UserTask, onClick: (UserTask) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick(userTask) }
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(userTask.task.taskName, fontSize = 18.sp)
            Text(
                text = when (userTask.state) {
                    "available"   -> "未接受"
                    "in_progress" -> "進行中"
                    "completed"   -> "已完成"
                    "claimed"     -> "已領取"
                    "declined"    -> "已拒絕"
                    else          -> userTask.state
                },
                color = when (userTask.state) {
                    "available"   -> Color.Red
                    "in_progress" -> Color(0xFFFFA500)
                    "completed"   -> Color.Green
                    else          -> Color.Gray
                }
            )
        }
    }
}

@Composable
fun TaskDialog(
    userTask: UserTask,
    onDismiss: () -> Unit,
    onAction: (String) -> Unit
) {
    val task = userTask.task
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.taskName) },
        text = {
            Column {
                Text("說明：${task.taskDescription}")
                Spacer(modifier = Modifier.height(8.dp))
                Text("目標：${task.taskTarget}")
                Spacer(modifier = Modifier.height(8.dp))
                if (task.isLLM) {
                    Text("難度：${task.taskDifficulty}")
                    Spacer(modifier = Modifier.height(8.dp))
                    task.taskDuration?.let { Text("時間：${formatMillis(it * 1000)}") } // taskDuration 是秒，formatMillis 需要毫秒
                    Spacer(modifier = Modifier.height(8.dp))
                }
                Text("獎勵分數：${task.rewardScore}")
            }
        },
        confirmButton = {
            when (userTask.state) {
                "available"   -> Button(onClick = { onAction("accept") }) { Text("接受任務") }
                "in_progress" -> Button(onClick = { onAction("complete") }) { Text("完成任務 (測試)") }
                "completed"   -> Button(onClick = { onAction("claim") }) { Text("領取獎勵") }
            }
        },
        dismissButton = {
            Row {
                if (userTask.state == "available" || userTask.state == "in_progress") {
                    TextButton(onClick = { onAction("decline") }) { Text("拒絕", color = Color.Red) }
                }
                TextButton(onClick = onDismiss) { Text("關閉") }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}