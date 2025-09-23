package com.ntou01157.hunter.ui.event_ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.GetStonePileStatusResponse
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.TriggerStonePileRequest
import com.ntou01157.hunter.api.TriggerStonePileResponse
import kotlinx.coroutines.launch

val eventApiService = RetrofitClient.apiService
const val userId = "68a48da731f22c76b7a5f52c"

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
                val statusResponse: GetStonePileStatusResponse =
                        eventApiService.getStonePileStatus(userId)
                canTriggerToday.value = !statusResponse.hasTriggeredToday
            } catch (e: Exception) {
                Log.e("StonePileUI", "獲取狀態失敗", e)
                snackbarHostState.showSnackbar("無法連接伺服器，請稍後再試。")
            } finally {
                isLoading.value = false
            }
        }
    }

    LaunchedEffect(key1 = userId) { fetchInitialData() }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Image(
                painter = painterResource(id = R.drawable.stone_background),
                contentDescription = "背景",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
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
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "發現了一堆不尋常的石頭，你決定搬開它。",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.stone_icon),
                    contentDescription = "Stone Pile",
                    modifier = Modifier.size(200.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (isLoading.value) {
                    CircularProgressIndicator(color = Color(0xFF4F7942))
                } else if (canTriggerToday.value) {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    val response: TriggerStonePileResponse =
                                            eventApiService.triggerStonePile(
                                                    TriggerStonePileRequest(userId)
                                            )
                                    if (response.success) {
                                        snackbarHostState.showSnackbar(response.message)
                                        fetchInitialData()
                                    } else {
                                        snackbarHostState.showSnackbar(response.message)
                                    }
                                } catch (e: retrofit2.HttpException) {
                                    Log.e("StonePileUI", "搬開石頭失敗：HTTP 錯誤", e)
                                    if (e.code() == 500) {
                                        snackbarHostState.showSnackbar("伺服器發生了錯誤，請稍後再試。")
                                    } else {
                                        snackbarHostState.showSnackbar("與伺服器通訊時發生了錯誤。")
                                    }
                                } catch (e: Exception) {
                                    Log.e("StonePileUI", "搬開石頭失敗：網路或其他錯誤", e)
                                    snackbarHostState.showSnackbar("網路連線錯誤，請檢查您的連線。")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F7942))
                    ) { Text(text = "搬開石頭", fontWeight = FontWeight.Bold) }
                } else {
                    Text(
                        text = "你已經獲得了今天的獎勵，請明天再來。",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onEventCompleted("你選擇了離開") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F7942))
                    ) { Text(text = "離開", fontWeight = FontWeight.Bold) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStonePileUI() {
    StonePileUI(onEventCompleted = {})
}
