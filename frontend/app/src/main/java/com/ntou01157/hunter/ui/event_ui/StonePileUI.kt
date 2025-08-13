package com.ntou01157.hunter.ui.event_ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.GetStonePileStatusResponse
import com.ntou01157.hunter.api.TriggerStonePileRequest
import com.ntou01157.hunter.api.TriggerStonePileResponse
import kotlinx.coroutines.launch
import android.util.Log

val eventApiService = RetrofitClient.apiService
const val userId = "6880f31469ff254ed2fb0cc1"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StonePileUI(onEventCompleted: (message: String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isLoading = remember { mutableStateOf(true) }
    val canTriggerToday = remember { mutableStateOf(false) }

    fun fetchInitialData() {
        coroutineScope.launch {
            isLoading.value = true
            try {
                val statusResponse: GetStonePileStatusResponse = eventApiService.getStonePileStatus(userId)
                canTriggerToday.value = !statusResponse.hasTriggeredToday
            } catch (e: Exception) {
                Log.e("StonePileUI", "獲取狀態失敗", e)
                snackbarHostState.showSnackbar("無法連接伺服器，請稍後再試。")
            } finally {
                isLoading.value = false
            }
        }
    }

    LaunchedEffect(key1 = userId) {
        fetchInitialData()
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
                text = "神秘的石堆",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "你發現了一堆不尋常的石頭，你決定搬開它。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.stone_pile),
                contentDescription = "Stone Pile",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading.value) {
                CircularProgressIndicator()
            } else if (canTriggerToday.value) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            try {
                                val response: TriggerStonePileResponse = eventApiService.triggerStonePile(
                                    TriggerStonePileRequest(userId)
                                )
                                if (response.success) {
                                    snackbarHostState.showSnackbar(response.message)
                                    fetchInitialData()
                                } else {
                                    snackbarHostState.showSnackbar(response.message)
                                }
                            } catch (e: Exception) {
                                Log.e("StonePileUI", "搬開石頭失敗", e)
                                snackbarHostState.showSnackbar("網路錯誤，無法觸發事件。")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "搬開石頭")
                }
            } else {
                Text(
                    text = "你已經獲得了今天的獎勵，請明天再來。",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = { onEventCompleted("你選擇了離開") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "離開")
                }
            }
        }
    }
}

// 已將這兩個 data class 從此檔案移除，並在頂部進行了 import
// data class GetStonePileStatusResponse(val hasTriggeredToday: Boolean)
// data class TriggerStonePileRequest(val userId: String)
// data class TriggerStonePileResponse(val success: Boolean, val message: String)

@Preview(showBackground = true)
@Composable
fun PreviewStonePileUI() {
    StonePileUI(onEventCompleted = {})
}