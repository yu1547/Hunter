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
fun ChatScreen(onClose: () -> Unit) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf<List<History>>(emptyList()) }
    val context = LocalContext.current

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
                        val userMessage = History("user", input.text, now)
                        val llmReply = History("LLM", "我收到了您的訊息，但目前無法處理請求。", java.time.Instant.now().toString())
                        messages = messages + userMessage + llmReply
                        input = TextFieldValue("")
                    }
                }
            ) {
                Text("送出")
            }
        }
    }
}