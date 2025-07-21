package com.ntou01157.hunter.ui

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
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ntou01157.hunter.R
import com.ntou01157.hunter.formatMillis
import com.ntou01157.hunter.models.Task


@Composable
fun TaskListScreen(navController: NavController) {
    val taskList = remember {
        mutableStateListOf(
            Task("1", "任務一", "介紹任務一", "簡單", "11111", taskDuration = 3600_000, rewardScore = 10),
            Task("2", "任務二", "介紹任務二", "普通", "22222", taskDuration = 7200_000, rewardScore = 20),
            Task("3", "任務三", "介紹任務三", "困難", "33333", taskDuration = 1800_000, rewardScore = 50)
        )
    }

    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showStartDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "Home",
                    modifier = Modifier
                        .size(32.dp)
                        .clickable { navController.navigate("main") }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text("任務清單", fontSize = 22.sp)
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF3E5E5))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(taskList) { task ->
                Card(
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { selectedTask = task }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(task.taskName, fontSize = 18.sp)
//                        Text(missions.state, color = if (missions.state == "已接受") Color.Gray else Color.Red)
                    }
                }
            }
        }

        selectedTask?.let { task ->
            AlertDialog(
                onDismissRequest = { selectedTask = null },
                title = { Text(task.taskName) },
                text = {
                    Column {
                        Text("說明：${task.taskDescription}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("目標：${task.taskTarget}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("難度：${task.taskDifficulty}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("時間：${formatMillis(task.taskDuration)}")  // ← 修正拼字
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("獎勵分數：${task.rewardScore}")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedTask = null }) {
                        Text("關閉")
                    }
//                    if (missions.state == "未接受") {
//                        TextButton(onClick = {
//                            taskList.replace(task.taskId) { t ->
//                                t.copy(state = "已接受")
//                            }
//                            selectedTask = null
//                            showStartDialog = true
//                        }) {
//                            Text("接受任務")
//                        }
//                    } else {
//                        TextButton(onClick = { selectedTask = null }) {
//                            Text("關閉")
//                        }
//                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }


        if (showStartDialog) {
            AlertDialog(
                onDismissRequest = { showStartDialog = false },
                title = { Text("開始執行任務！") },
                confirmButton = {},
                dismissButton = {},
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false)
            )

            LaunchedEffect(Unit) {
                delay(1000)
                showStartDialog = false
                navController.navigate("main")
            }
        }
    }
}

// 用於根據 taskId 更新某個任務的擴充函式
fun SnapshotStateList<Task>.replace(taskId: String, updater: (Task) -> Task) {
    val index = indexOfFirst { it.taskId == taskId }
    if (index != -1) {
        this[index] = updater(this[index])
    }
}
