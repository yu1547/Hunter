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
import kotlinx.coroutines.launch


@Composable
fun TaskListScreen(navController: NavController) {
    val userId = "6880f31469ff254ed2fb0cc1" // 假定使用者ID
    val userTaskList = remember { mutableStateListOf<UserTask>() }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // 載入及刷新任務
    fun refreshTasks() {
        coroutineScope.launch {
            isLoading = true
            try {
                val tasks = TaskRepository.refreshAndGetTasks(userId)
                userTaskList.clear()
                userTaskList.addAll(tasks)
                errorMessage = null
            } catch (e: Exception) {
                errorMessage = "無法載入任務: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        refreshTasks()
    }

    var selectedUserTask by remember { mutableStateOf<UserTask?>(null) }
    var showMessageDialog by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.padding(top = 25.dp, bottom = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "回首頁",
                        modifier = Modifier.size(40.dp)
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
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Text(errorMessage!!, color = Color.Red, modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(userTaskList) { userTask ->
                        TaskItem(userTask) {
                            selectedUserTask = it
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
                        val taskId = userTask.task.taskId
                        var success = false
                        var message = ""

                        try {
                            when (action) {
                                "accept" -> {
                                    val newState = TaskRepository.acceptTask(userId, taskId)
                                    if (newState != null) {
                                        val index = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                        if (index != -1) {
                                            userTaskList[index] = userTaskList[index].copy(state = newState)
                                        }
                                        message = "任務已接受！"
                                        success = true
                                    }
                                }
                                "decline" -> {
                                    TaskRepository.declineTask(userId, taskId)
                                    val index = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                    if (index != -1) {
                                        userTaskList[index] = userTaskList[index].copy(state = "declined")
                                    }
                                    message = "任務已拒絕"
                                    success = true
                                }
                                "complete" -> {
                                    val newState = TaskRepository.completeTask(userId, taskId)
                                    if (newState != null) {
                                        val index = userTaskList.indexOfFirst { it.task.taskId == taskId }
                                        if (index != -1) {
                                            userTaskList[index] = userTaskList[index].copy(state = newState)
                                        }
                                        message = "任務已完成！"
                                        success = true
                                    }
                                }
                                "claim" -> {
                                    TaskRepository.claimReward(userId, taskId)
                                    message = "獎勵已領取！"
                                    refreshTasks() // 刷新整個列表
                                    success = true
                                }
                            }
                        } catch (e: Exception) {
                            // 錯誤處理
                            success = false
                        }

                        if (success) {
                            showMessageDialog = message
                        } else {
                            showMessageDialog = "操作失敗"
                        }
                        selectedUserTask = null // 關閉對話框
                    }
                }
            )
        }

        // 顯示操作結果訊息
        showMessageDialog?.let { message ->
            AlertDialog(
                onDismissRequest = { showMessageDialog = null },
                title = { Text("通知") },
                text = { Text(message) },
                confirmButton = {
                    TextButton(onClick = { showMessageDialog = null }) {
                        Text("確定")
                    }
                }
            )
        }
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
                text = when(userTask.state) {
                    "available" -> "未接受"
                    "in_progress" -> "進行中"
                    "completed" -> "已完成"
                    "claimed" -> "已領取"
                    "declined" -> "已拒絕"
                    else -> userTask.state
                },
                color = when(userTask.state) {
                    "available" -> Color.Red
                    "in_progress" -> Color(0xFFFFA500) // Orange
                    "completed" -> Color.Green
                    else -> Color.Gray
                }
            )
        }
    }
}

@Composable
fun TaskDialog(userTask: UserTask, onDismiss: () -> Unit, onAction: (String) -> Unit) {
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
                Text("難度：${task.taskDifficulty}")
                Spacer(modifier = Modifier.height(8.dp))
                task.taskDuration?.let { Text("時間：${formatMillis(it * 1000)}") } // taskDuration 是秒，formatMillis 需要毫秒
                Spacer(modifier = Modifier.height(8.dp))
                Text("獎勵分數：${task.rewardScore}")
            }
        },
        confirmButton = {
            when (userTask.state) {
                "available" -> {
                    Button(onClick = { onAction("accept") }) { Text("接受任務") }
                }
                "in_progress" -> {
                    Button(onClick = { onAction("complete") }) { Text("完成任務 (測試)") }
                }
                "completed" -> {
                    Button(onClick = { onAction("claim") }) { Text("領取獎勵") }
                }
            }
        },
        dismissButton = {
            Row {
                if (userTask.state == "available" || userTask.state == "in_progress") {
                    TextButton(onClick = { onAction("decline") }) {
                        Text("拒絕", color = Color.Red)
                    }
                }
                TextButton(onClick = onDismiss) { Text("關閉") }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
