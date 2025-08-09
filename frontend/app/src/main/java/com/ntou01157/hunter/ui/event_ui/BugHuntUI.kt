package com.ntou01157.hunter.ui.event_ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugHuntUI() {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var bugInput by remember { mutableStateOf("") }
    var isSolved by remember { mutableStateOf(false) }

    // 模擬獎勵狀態
    var userScore by remember { mutableIntStateOf(0) }

    // 隨機生成解碼結果
    var correctBugCode by remember { mutableStateOf("") }
    var scrambledCode by remember { mutableStateOf("") }

    // 在 UI 首次載入時生成一次解碼結果
    LaunchedEffect(Unit) {
        correctBugCode = generateRandomCode()
        // 將字串轉為字元列表後才能使用 shuffled()
        scrambledCode = correctBugCode.toList().shuffled().joinToString("")
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "在小小的 code 裡面抓阿抓阿抓",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "code 出現了異常，請找出 bug 並回報。請輸入你解出的文字。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 顯示積分
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "積分: $userScore", style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(modifier = Modifier.height(16.dp))

            if (!isSolved) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("錯亂的資訊:", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = scrambledCode,
                            style = MaterialTheme.typography.headlineMedium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = bugInput,
                    onValueChange = { bugInput = it },
                    label = { Text("輸入解碼結果") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (bugInput.lowercase().trim() == correctBugCode) {
                                isSolved = true
                                userScore += 5
                                snackbarHostState.showSnackbar("解碼成功！你獲得了積分+5")
                            } else {
                                snackbarHostState.showSnackbar("解碼失敗，請再試一次。")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "回報 Bug")
                }
            } else {
                Text(
                    text = "你已經成功解決了這個謎題，下次有新的挑戰再回來吧！",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// 函式：生成一個隨機字串作為解碼結果
private fun generateRandomCode(length: Int = 8): String {
    val allowedChars = ('a'..'z') + ('0'..'9')
    return (1..length)
        .map { allowedChars.random() }
        .joinToString("")
}

@Preview(showBackground = true)
@Composable
fun PreviewBugHuntUI() {
    BugHuntUI()
}