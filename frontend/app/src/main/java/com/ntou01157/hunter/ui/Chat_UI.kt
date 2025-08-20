package com.ntou01157.hunter.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.ChatRequest
import com.ntou01157.hunter.api.ChatHistoryItem
import com.ntou01157.hunter.api.ChatResponse
import com.ntou01157.hunter.models.History
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun ChatScreen(
    userId: String = "68846d797609912e5e6ba9af",
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf<List<History>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    fun deleteHistoryAndClose() {
        coroutineScope.launch {
            try {
                RetrofitClient.apiService.deleteChatHistory(userId)
                Log.d("ChatScreen", "已刪除對話紀錄")
            } catch (e: Exception) {
                Log.e("ChatScreen", "刪除對話紀錄失敗", e)
            } finally {
                onClose()
            }
        }
    }

    LaunchedEffect(messages) {
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 標題區域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("客服中心", fontSize = 20.sp)
                TextButton(onClick = { deleteHistoryAndClose() }) {
                    Text("關閉")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 對話區域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(scrollState)
                        .fillMaxWidth()
                ) {
                    for (msg in messages) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (msg.role == "user") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.widthIn(max = 240.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = msg.content,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        fontSize = 15.sp
                                    )
                                    if (msg.timestamp.isNotEmpty()) {
                                        Text(
                                            text = msg.timestamp.substring(0, 19).replace("T", " "),
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.align(Alignment.End)
                                        )
                                    }
                                }
                            }
                        }
                    }
                    if (isLoading) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "生成回覆中請稍後...",
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            // 輸入區域
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("請輸入訊息...") },
                    enabled = !isLoading,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (input.text.isNotBlank() && !isLoading) {
                                sendMessage(
                                    input,
                                    messages,
                                    userId,
                                    coroutineScope,
                                    onResult = { newMessages -> messages = newMessages },
                                    onLoading = { loading -> isLoading = loading },
                                    onInputClear = { input = TextFieldValue("") }
                                )
                            }
                        }
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (input.text.isNotBlank() && !isLoading) {
                            sendMessage(
                                input,
                                messages,
                                userId,
                                coroutineScope,
                                onResult = { newMessages -> messages = newMessages },
                                onLoading = { loading -> isLoading = loading },
                                onInputClear = { input = TextFieldValue("") }
                            )
                        }
                    },
                    enabled = !isLoading
                ) {
                    Text("送出")
                }
            }
        }
    }
}

private fun sendMessage(
    input: TextFieldValue,
    messages: List<History>,
    userId: String,
    coroutineScope: CoroutineScope,
    onResult: (List<History>) -> Unit,
    onLoading: (Boolean) -> Unit,
    onInputClear: () -> Unit
) {
    val now = java.time.Instant.now().toString()
    val userMessage = History("user", input.text, now)
    val currentHistory = messages + userMessage
    onResult(currentHistory)
    onInputClear()
    onLoading(true)
    coroutineScope.launch {
        try {
            val apiHistory = currentHistory.map {
                ChatHistoryItem(it.role, it.content, it.timestamp)
            }
            val request = ChatRequest(
                message = userMessage.content,
                history = apiHistory
            )
            Log.d("ChatScreen", "送出請求: $request")
            val response: ChatResponse = RetrofitClient.apiService
                .chatWithLLM(userId, request)
            Log.d("ChatScreen", "API 回傳: $response")
            if (response.reply.isNotBlank()) {
                val llmReply = History(
                    "LLM",
                    response.reply,
                    java.time.Instant.now().toString()
                )
                onResult(currentHistory + llmReply)
            } else {
                Log.e("ChatScreen", "API 回傳 reply 為空")
                val llmReply = History(
                    "LLM",
                    "伺服器未回傳內容，請稍後再試",
                    java.time.Instant.now().toString()
                )
                onResult(currentHistory + llmReply)
            }
        } catch (e: Exception) {
            Log.e("ChatScreen", "API error", e)
            var errorMsg = "伺服器錯誤，請稍後再試"
            if (e is HttpException) {
                val errorBody = e.response()?.errorBody()?.string()
                Log.e("ChatScreen", "HttpException body: $errorBody")
                if (errorBody != null) {
                    try {
                        val json = org.json.JSONObject(errorBody)
                        val backendMsg = json.optString("error")
                        if (backendMsg.contains("AI 服務暫時無法回應")) {
                            errorMsg = backendMsg
                        } else if (backendMsg.isNotBlank()) {
                            errorMsg = backendMsg
                        }
                    } catch (jsonEx: Exception) {
                        Log.e("ChatScreen", "解析 errorBody 失敗", jsonEx)
                    }
                }
            }
            val llmReply = History(
                "LLM",
                errorMsg,
                java.time.Instant.now().toString()
            )
            onResult(currentHistory + llmReply)
        } finally {
            onLoading(false)
        }
    }
}