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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.rememberCoroutineScope
import org.json.JSONObject

data class ChatMessage(
    val role: String,
    val content: String,
    val timestamp: String = ""
)

@Composable
fun ChatScreen(onClose: () -> Unit) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember {
        mutableStateOf(
            listOf(
                ChatMessage("user", "鑰匙是做什麼？", "2025-07-31T11:58:00Z"),
                ChatMessage("LLM", "您可以在寶箱事件中，使用鑰匙開啟寶箱並獲得獎勵。", "2025-07-31T11:59:00Z")
            )
        )
    }
    val coroutineScope = rememberCoroutineScope()
    var isLoading by remember { mutableStateOf(false) }

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
                .verticalScroll(rememberScrollState())
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
                        messages = messages + ChatMessage("user", input.text, now)
                        isLoading = true
                        coroutineScope.launch {
                            val reply = sendChatToLLM(messages, input.text)
                            messages = messages + ChatMessage("LLM", reply, java.time.Instant.now().toString())
                            isLoading = false
                        }
                        input = TextFieldValue("")
                    }
                },
                enabled = !isLoading
            ) {
                Text(if (isLoading) "傳送中..." else "送出")
            }
        }
    }
}

// 呼叫後端 LLM API，取得回覆
suspend fun sendChatToLLM(messages: List<ChatMessage>, userInput: String): String {
    return withContext(Dispatchers.IO) {
        try {
            val userId = "68846d797609912e5e6ba9af" // 假設的用戶 ID
            val url = URL("http://10.0.2.2:4000/api/chat/$userId") 
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8")
            conn.doOutput = true

            // 準備 history
            val historyArray = org.json.JSONArray()
            for (msg in messages) {
                if (msg.role == "user" || msg.role == "LLM") {
                    val obj = JSONObject()
                    obj.put("role", msg.role)
                    obj.put("content", msg.content)
                    obj.put("timestamp", msg.timestamp)
                    historyArray.put(obj)
                }
            }

            // 準備 body
            val body = JSONObject()
            body.put("message", userInput)
            body.put("history", historyArray)

            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }

            val responseCode = conn.responseCode
            val responseText = try {
                conn.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                conn.errorStream?.bufferedReader()?.use { it.readText() } ?: ""
            }
            val json = JSONObject(responseText.ifEmpty { "{}" })
            if (responseCode in 200..299) {
                json.optString("reply", "伺服器暫無回應")
            } else {
                json.optString("error", "伺服器錯誤或連線失敗")
            }
        } catch (e: Exception) {
            "伺服器錯誤或連線失敗"
        }
    }
}
