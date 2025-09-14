package com.ntou01157.hunter.ui

import android.util.Log
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.ChatRequest
import com.ntou01157.hunter.api.ChatHistoryItem
import com.ntou01157.hunter.api.ChatResponse
import com.ntou01157.hunter.models.History
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import retrofit2.HttpException
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.times


@Composable
fun ChatScreen(
    onClose: () -> Unit
) {
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var messages by remember { mutableStateOf<List<History>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    // 新增：用 email 抓 userId
    var userIdState by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        // 測試用，正式請用 API 取得，並註解掉這行!!!!
        userIdState = "68846d797609912e5e6ba9af"

        // 若要恢復原本 API 取得，請註解掉上面這行，並還原下方 try-catch 區塊 !!!!!!!
        /*
        try {
            val email = FirebaseAuth.getInstance().currentUser?.email
                ?: run {
                    Log.e("ChatScreen", "尚未登入，無法取得 email")
                    return@LaunchedEffect
                }
            val user = RetrofitClient.apiService.getUserByEmail(email)
            userIdState = user.id
            Log.d("ChatScreen", "取得 userId=${userIdState}")
        } catch (e: Exception) {
            Log.e("ChatScreen", "以 email 取得 userId 失敗：${e.message}", e)
        }
        */
    }

    fun deleteHistoryAndClose() {
        val uid = userIdState ?: return
        coroutineScope.launch {
            try {
                RetrofitClient.apiService.deleteChatHistory(uid)
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
        modifier = Modifier.fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.csr_dialog),
            contentDescription = "客服視窗背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 5.dp, start = 30.dp, end = 30.dp)
        ) {
            val screenHeight = LocalConfiguration.current.screenHeightDp.dp
            val screenWidth = LocalConfiguration.current.screenWidthDp.dp
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 0.0001f * screenHeight), // 調整比例，往上移動
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "獵人智能客服",
                    fontSize = 22.sp,
                    color = Color.Black
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 0.0001f * screenHeight,
                            end = screenWidth * 0.00001f // 依比例再往右移動
                        ),
                    contentAlignment = Alignment.TopEnd
                ) {
                    IconButton(onClick = {
                        Log.d("ChatScreen", "關閉按鈕被點擊")
                        deleteHistoryAndClose()
                    }) {
                        Image(
                            painter = painterResource(id = R.drawable.csr_close_button),
                            contentDescription = "關閉客服視窗",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 對話區域
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(5.dp)
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

            Spacer(modifier = Modifier.height(2.dp))

            // 輸入區域
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .padding(bottom = 5.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.csr_user_input),
                    contentDescription = "輸入訊息背景",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                )
                // TextField 佔滿寬度，送出按鈕獨立右下
                TextField(
                    value = input,
                    onValueChange = { input = it },
                    modifier = Modifier
                        .align(Alignment.CenterStart) // 讓整個輸入框在容器中垂直置中
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(start = 10.dp, end = 100.dp), // 移除原本 top = 24.dp 以便真正置中
                    placeholder = {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.CenterStart // 垂直置中 + 靠左
                        ) {
                            Text(
                                "請輸入訊息...",
                                fontSize = 13.sp
                            )
                        }
                    },
                    enabled = !isLoading,
                    singleLine = false,
                    maxLines = 4,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        disabledContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent
                    ),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            val uid = userIdState ?: return@KeyboardActions
                            if (input.text.isNotBlank() && !isLoading) {
                                sendMessage(
                                    input,
                                    messages,
                                    uid,
                                    coroutineScope,
                                    onResult = { newMessages -> messages = newMessages },
                                    onLoading = { loading -> isLoading = loading },
                                    onInputClear = { input = TextFieldValue("") }
                                )
                            }
                        }
                    )
                )
                // 送出按鈕獨立在右
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(end = 5.dp, bottom = 27.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    IconButton(
                        onClick = {
                            Log.d("ChatScreen", "送出訊息按鈕被點擊")
                            val uid = userIdState ?: return@IconButton
                            if (input.text.isNotBlank() && !isLoading) {
                                sendMessage(
                                    input,
                                    messages,
                                    uid,
                                    coroutineScope,
                                    onResult = { newMessages -> messages = newMessages },
                                    onLoading = { loading -> isLoading = loading },
                                    onInputClear = { input = TextFieldValue("") }
                                )
                            }
                        },
                        enabled = !isLoading
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.csr_upload_button),
                            contentDescription = "送出訊息",
                            modifier = Modifier.size(30.dp)
                        )
                    }
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
                        val detailMsg = json.optString("detail")
                        if (detailMsg.contains("timeout")) {
                            errorMsg = "AI 回覆逾時，請稍後再試"
                        } else if (backendMsg.contains("AI 服務暫時無法回應")) {
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


@Preview(showBackground = true, widthDp = 720, heightDp = 1280)
@Composable
fun ChatScreenPreview() {
    ChatScreen(onClose = {})
}
