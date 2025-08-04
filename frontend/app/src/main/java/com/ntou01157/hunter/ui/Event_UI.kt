// ui/Event_UI.kt
package com.ntou01157.hunter.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.RetrofitClient.apiService
import com.ntou01157.hunter.data.EventRepository
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.utils.NetworkResult
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun EventScreen(event: EventModel, onEventCompleted: (resultMessage: String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .background(MaterialTheme.colorScheme.surface),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = event.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = event.description,
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))

            when (event.type) {
                // ... 其他事件類型 ...
                "daily", "bless" -> OptionsUI(event, onEventCompleted)
                // ...
            }
        }
    }
}

/**
 * 神秘商人與古樹的祝福，提供選項讓使用者選擇。
 */
@Composable
fun OptionsUI(event: EventModel, onEventCompleted: (resultMessage: String) -> Unit) {
    val coroutineScope = rememberCoroutineScope()
    var isEventProcessing by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 使用新的資料結構：event.mechanics.additionalInfo?.exchangeOptions
        event.mechanics.additionalInfo?.exchangeOptions?.forEach { exchangeOption ->
            Button(
                onClick = {
                    if (!isEventProcessing) {
                        isEventProcessing = true
                        coroutineScope.launch {
                            val userId = "使用者ID" // TODO: 從 ViewModel 或使用者 Session 中取得

                            // 這裡的 completeEvent 呼叫需要注意，後端可能需要更多參數
                            val result = EventRepository(apiService).completeEvent(
                                event.id,
                                userId,
                                selectedOption = exchangeOption.option // 使用 exchangeOption.option
                            )

                            // 根據 API 回應結果，處理不同情況
                            val message = when (result) {
                                is NetworkResult.Success -> {
                                    val apiMessage = result.data?.message ?: "事件完成！"
                                    "你選擇了'${exchangeOption.option}'。\n$apiMessage"
                                }
                                is NetworkResult.Error -> {
                                    val errorMsg = result.message ?: "發生未知錯誤。"
                                    "操作失敗：$errorMsg"
                                }
                                is NetworkResult.Loading -> "處理中..."
                            }
                            onEventCompleted(message)
                            isEventProcessing = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isEventProcessing // 處理中時禁用按鈕
            ) {
                Text(text = exchangeOption.option) // 使用 exchangeOption.option
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEventProcessing) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}

// ... SlimeGameUI, PuzzleUI, ChestUI 函式保持不變