package com.ntou01157.hunter.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.ChatRequest
import com.ntou01157.hunter.api.ChatHistoryItem
import com.ntou01157.hunter.api.ChatResponse
import com.ntou01157.hunter.models.History
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject

@Composable
fun ChatScreen(
    userId: String = "68846d797609912e5e6ba9af",
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf<List<History>>(emptyList()) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val jsonString = context.assets.open("csr.json").bufferedReader().use { it.readText() }
            val jsonObject = JSONObject(jsonString)
            val historyArray = jsonObject.getJSONArray("history")
            val loadedMessages = mutableListOf<History>()
            for (i in 0 until historyArray.length()) {
                val msgObject = historyArray.getJSONObject(i)
                loadedMessages.add(
                    History(
                        role = msgObject.getString("role"),
                        content = msgObject.getString("content"),
                        timestamp = msgObject.getString("timestamp")
                    )
                )
            }
            messages = loadedMessages
        } catch (e: Exception) {
            Log.e("ChatScreen", "Error loading chat history", e)
        }
    }

    LaunchedEffect(messages) {
        coroutineScope.launch {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF6EDF7))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("客服中心", fontSize = 20.sp)
            TextButton(onClick = onClose) {
                Text("關閉")
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState)
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp)
        ) {
            for (msg in messages) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = if (msg.role == "user") Arrangement.End else Arrangement.Start
                ) {
                    Surface(
                        color = if (msg.role == "user") Color(0xFFbc8f8f) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = msg.content,
                                color = if (msg.role == "user") Color.White else Color.Black
                            )
                            if (msg.timestamp.isNotEmpty()) {
                                Text(
                                    text = msg.timestamp,
                                    fontSize = 10.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("請輸入訊息...") }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    val now = java.time.Instant.now().toString()
                    if (input.text.isNotBlank()) {
                        val userMessage = History("user", input.text, now)
                        val currentHistory = messages + userMessage
                        messages = currentHistory // 先顯示使用者訊息
                        input = TextFieldValue("")
                        coroutineScope.launch {
                            try {
                                val apiHistory = currentHistory.map {
                                    ChatHistoryItem(it.role, it.content, it.timestamp)
                                }
                                val request = ChatRequest(
                                    message = userMessage.content,
                                    history = apiHistory
                                )
                                val response: ChatResponse = RetrofitClient.apiService
                                    .chatWithLLM(userId, request)
                                val llmReply = History(
                                    "LLM",
                                    response.reply,
                                    java.time.Instant.now().toString()
                                )
                                messages = currentHistory + llmReply
                            } catch (e: Exception) {
                                val errorMsg = "伺服器錯誤，請稍後再試"
                                val llmReply = History(
                                    "LLM",
                                    errorMsg,
                                    java.time.Instant.now().toString()
                                )
                                messages = currentHistory + llmReply
                                Log.e("ChatScreen", "API error", e)
                            }
                        }
                    }
                }
            ) {
                Text("送出")
            }
        }
    }
}