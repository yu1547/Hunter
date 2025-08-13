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
// 修正 1: 引入正確的 UserItem 模型，而不是 UserItemModel
import com.ntou01157.hunter.models.model_api.UserItem
import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.RetrofitClient
// 修正 2: 引入正確的 API 請求和回應模型
import com.ntou01157.hunter.api.OpenTreasureBoxRequest
import com.ntou01157.hunter.api.OpenTreasureBoxResponse
import kotlinx.coroutines.launch
import android.util.Log



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureBoxUI(onEventCompleted: (message: String) -> Unit) {
    val eventApiService = RetrofitClient.apiService
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    // 修正 1: 將 allItems 的類型從 UserItemModel 改為 UserItem，與 API 回傳類型一致
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchItems() {
        coroutineScope.launch {
            isLoading.value = true
            try {
                // 這裡的 fetchedItems 已經是正確的 List<UserItem> 類型
                val fetchedItems: List<UserItem> = eventApiService.fetchUserItems(userId)
                allItems.clear()
                // 修正 1: allItems 現在是 List<UserItem>，可以直接添加
                allItems.addAll(fetchedItems)
            } catch (e: Exception) {
                Log.e("TreasureBoxUI", "獲取物品失敗", e)
                snackbarHostState.showSnackbar("無法連接伺服器，請稍後再試。")
            } finally {
                isLoading.value = false
            }
        }
    }

    LaunchedEffect(key1 = userId) {
        fetchItems()
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
                text = "偶遇寶箱",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "偶遇寶箱，你要打開他嗎？",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.chest),
                contentDescription = "Treasure Box",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (isLoading.value) {
                CircularProgressIndicator()
            } else {
                // 修正 1: 傳遞正確的 allItems 類型
                KeyInventoryDisplay(allItems)
            }

            Spacer(modifier = Modifier.height(24.dp))

            TreasureBoxOption(
                keyName = "銅鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        try {
                            val response = eventApiService.openTreasureBox(
                                // 修正 2: 使用從 api 套件引入的 OpenTreasureBoxRequest
                                OpenTreasureBoxRequest(userId, "bronze")
                            )
                            if (response.success) {
                                fetchItems()
                                snackbarHostState.showSnackbar("你用銅鑰匙打開了寶箱，獲得道具: ${response.drops.joinToString()}")
                                onEventCompleted("寶箱事件已完成")
                            } else {
                                snackbarHostState.showSnackbar(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e("TreasureBoxUI", "開啟寶箱失敗", e)
                            snackbarHostState.showSnackbar("網路錯誤，無法開啟寶箱。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TreasureBoxOption(
                keyName = "銀鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        try {
                            val response = eventApiService.openTreasureBox(
                                // 修正 2: 使用從 api 套件引入的 OpenTreasureBoxRequest
                                OpenTreasureBoxRequest(userId, "silver")
                            )
                            if (response.success) {
                                fetchItems()
                                snackbarHostState.showSnackbar("你用銀鑰匙打開了寶箱，獲得道具: ${response.drops.joinToString()}")
                                onEventCompleted("寶箱事件已完成")
                            } else {
                                snackbarHostState.showSnackbar(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e("TreasureBoxUI", "開啟寶箱失敗", e)
                            snackbarHostState.showSnackbar("網路錯誤，無法開啟寶箱。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TreasureBoxOption(
                keyName = "金鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        try {
                            val response = eventApiService.openTreasureBox(
                                // 修正 2: 使用從 api 套件引入的 OpenTreasureBoxRequest
                                OpenTreasureBoxRequest(userId, "gold")
                            )
                            if (response.success) {
                                fetchItems()
                                snackbarHostState.showSnackbar("你用金鑰匙打開了寶箱，獲得道具: ${response.drops.joinToString()}")
                                onEventCompleted("寶箱事件已完成")
                            } else {
                                snackbarHostState.showSnackbar(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e("TreasureBoxUI", "開啟寶箱失敗", e)
                            snackbarHostState.showSnackbar("網路錯誤，無法開啟寶箱。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("你選擇了離開，寶箱依然靜靜地躺在那裡。")
                    }
                    onEventCompleted("寶箱事件已結束")
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "離開")
            }
        }
    }
}

// 修正 2: 已將這兩個 data class 從此檔案移除，並在頂部進行了 import
// data class OpenTreasureBoxRequest(val userId: String, val keyType: String)
// data class OpenTreasureBoxResponse(val success: Boolean, val message: String, val drops: List<String>)

@Composable
fun TreasureBoxOption(keyName: String, onOpenClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "使用 $keyName 開啟寶箱",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(onClick = onOpenClick) {
                Text(text = "開啟")
            }
        }
    }
}

@Composable
// 修正 1: 將參數類型從 List<UserItemModel> 改為 List<UserItem>
fun KeyInventoryDisplay(allItems: List<UserItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "你的鑰匙", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // 修正 3: UserItem 類別中直接有 item 屬性，其內有 itemName 屬性
            val bronzeKey = allItems.find { it.item.itemName == "銅鑰匙" }
            val silverKey = allItems.find { it.item.itemName == "銀鑰匙" }
            val goldKey = allItems.find { it.item.itemName == "金鑰匙" }
            // 修正 3: count 屬性直接就是 Int，不需要 .value
            Text(text = "銅鑰匙: ${bronzeKey?.count ?: 0}")
            Text(text = "銀鑰匙: ${silverKey?.count ?: 0}")
            Text(text = "金鑰匙: ${goldKey?.count ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTreasureBoxUI() {
    TreasureBoxUI(onEventCompleted = {})
}