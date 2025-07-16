package com.ntou01157.hunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import java.util.concurrent.TimeUnit

// 任務資料類別
data class Task(
    val id: Int,
    val title: String,
    val description: String,
    val reward: String,
    val initialTimeMillis: Long,
    var isAccepted: Boolean = false,
    val remainingTimeMillis: MutableState<Long> = mutableStateOf(0L)
)

@Composable
fun TaskListScreen(navController: NavController) {
    val taskList = remember {
        mutableStateListOf(
            Task(1, "任務1", "任務1的介紹", "任務1的獎勵", 3600_000),
            Task(2, "任務2", "任務2的介紹", "任務2的獎勵", 7200_000),
            Task(3, "任務3", "任務3的介紹", "任務3的獎勵", 1800_000,)
        )
    }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    var showStartDialog by remember { mutableStateOf(false) }

    // 接受任務後倒數，每秒更新一次
    LaunchedEffect(taskList) {
        while (true) {
            delay(1000)
            taskList.removeAll { it.isAccepted && it.remainingTimeMillis.value <= 0L }
            taskList.forEach { task ->
                if (task.isAccepted && task.remainingTimeMillis.value > 0) {
                    task.remainingTimeMillis.value -= 1000
                }
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
                        Text(task.title, fontSize = 18.sp)
                        if (task.isAccepted) {
                            val time by task.remainingTimeMillis
                            Text(formatMillis(time), color = Color.Gray)
                        } else {
                            Text("尚未接受", color = Color.Red)
                        }
                    }
                }
            }
        }

        // 任務說明的彈窗
        selectedTask?.let { task ->
            AlertDialog(
                onDismissRequest = { selectedTask = null },
                title = { Text(task.title) },
                text = {
                    Column {
                        Text("說明：${task.description}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("獎勵：${task.reward}")
                        if (task.isAccepted) {
                            Spacer(modifier = Modifier.height(8.dp))
                            val time by task.remainingTimeMillis
                            Text("剩餘時間：${formatMillis(time)}")
                        }
                    }
                },
                confirmButton = {
                    if (!task.isAccepted) {
                        TextButton(onClick = {
                            task.isAccepted = true
                            task.remainingTimeMillis.value = task.initialTimeMillis
                            selectedTask = null
                            showStartDialog = true
                        }) {
                            Text("接受任務")
                        }
                    } else {
                        TextButton(onClick = { selectedTask = null }) {
                            Text("關閉")
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 接受任務後提示
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
                navController.navigate("main") //
            }
        }
    }
}

// 轉換時間HH:mm:ss
fun formatMillis(millis: Long): String {
    val hours = TimeUnit.MILLISECONDS.toHours(millis)
    val minutes = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
    val seconds = TimeUnit.MILLISECONDS.toSeconds(millis) % 60
    return String.format("%02d:%02d:%02d", hours, minutes, seconds)
}
