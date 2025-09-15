package com.ntou01157.hunter.ui.event_ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ntou01157.hunter.models.model_api.UserItem
import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.BlessTreeRequest
import com.ntou01157.hunter.models.model_api.ItemModel
import kotlinx.coroutines.launch
import android.util.Log



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncientTreeUI(onEventCompleted: (message: String) -> Unit) {
    // 將 API 服務和 userId 的定義移至 Composable 外部，保持一致性
    val eventApiService = RetrofitClient.apiService
    val userId = "68a48da731f22c76b7a5f52c"

    val coroutineScope = rememberCoroutineScope()
    // 修正1: 宣告 allItems 時，使用正確的資料模型 UserItem
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchItems() {
        coroutineScope.launch {
            isLoading.value = true
            try {
                // 這裡的 items 已經是正確的 List<UserItem> 類型
                val items: List<UserItem> = eventApiService.fetchUserItems(userId)
                allItems.clear()
                // 修正1: allItems 現在是 List<UserItem>，可以直接添加
                allItems.addAll(items)
            } catch (e: Exception) {
                Log.e("AncientTreeUI", "獲取物品失敗", e)
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
                text = "古樹的祝福",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "古樹散發著神秘的氣息，用史萊姆黏液來獻祭它，也許能獲得意想不到的祝福。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            if (isLoading.value) {
                CircularProgressIndicator()
            } else {
                // 修正2: 傳遞正確的 allItems 類型
                InventoryDisplay(allItems)
            }

            Spacer(modifier = Modifier.height(24.dp))

            AncientTreeOption(
                title = "普通的史萊姆黏液",
                description = "獻祭一瓶普通的史萊姆黏液，獲得銅鑰匙碎片 x2",
                onBlessClick = {
                    coroutineScope.launch {
                        try {
                            // 修正3: 這裡不再使用本地的 BlessTreeRequest，而是從 com.ntou01157.hunter.api 引入
                            val response = eventApiService.blessTree(BlessTreeRequest(userId, "普通的史萊姆黏液"))
                            if (response.success) {
                                snackbarHostState.showSnackbar(response.message)
                                fetchItems()
                            } else {
                                snackbarHostState.showSnackbar(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e("AncientTreeUI", "獻祭失敗", e)
                            snackbarHostState.showSnackbar("網路錯誤，無法獻祭。")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            AncientTreeOption(
                title = "黏稠的史萊姆黏液",
                description = "獻祭一瓶黏稠的史萊姆黏液，獲得銀鑰匙碎片 x2",
                onBlessClick = {
                    coroutineScope.launch {
                        try {
                            val response = eventApiService.blessTree(BlessTreeRequest(userId, "黏稠的史萊姆黏液"))
                            if (response.success) {
                                snackbarHostState.showSnackbar(response.message)
                                fetchItems()
                            } else {
                                snackbarHostState.showSnackbar(response.message)
                            }
                        } catch (e: Exception) {
                            Log.e("AncientTreeUI", "獻祭失敗", e)
                            snackbarHostState.showSnackbar("網路錯誤，無法獻祭。")
                        }
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = { onEventCompleted("你選擇了離開") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "離開")
            }
        }
    }
}

// 修正3: 刪除此處的資料類別定義，它們應該被放在 com.ntou01157.hunter.api.ApiService.kt 檔案中

@Composable
fun AncientTreeOption(title: String, description: String, onBlessClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onBlessClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(text = "獻祭")
            }
        }
    }
}

@Composable
// 修正2: 將參數類型從 List<UserItemModel> 改為 List<UserItem>
fun InventoryDisplay(allItems: List<UserItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "你的物品", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // 修正4: UserItem 類別中直接有 item 屬性，其內有 itemName 屬性
            val slime1 = allItems.find { it.item.itemName == "普通的史萊姆黏液" }
            val slime2 = allItems.find { it.item.itemName == "黏稠的史萊姆黏液" }
            // 修正4: count 屬性直接就是 Int，不需要 .value
            Text(text = "普通的史萊姆黏液: ${slime1?.count ?: 0}")
            Text(text = "黏稠的史萊姆黏液: ${slime2?.count ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAncientTreeUI() {
    AncientTreeUI(onEventCompleted = {})
}