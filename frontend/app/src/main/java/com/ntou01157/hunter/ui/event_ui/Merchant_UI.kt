package com.ntou01157.hunter.ui.event_ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.TradeRequest
import com.ntou01157.hunter.models.model_api.UserItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantUI(userId: String, onEventCompleted: (success: Boolean) -> Unit) {
    // 將 API 服務和 userId 的定義移至 Composable 外部，以保持一致性
    val eventApiService = RetrofitClient.apiService
    // val userId = "68a48da731f22c76b7a5f52c"
    val coroutineScope = rememberCoroutineScope()
    // 將 allItems 的類型從 UserItemModel 改為 UserItem，與 API 回傳類型一致
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val snackbarHostState = remember { SnackbarHostState() }

    fun fetchItems() {
        coroutineScope.launch {
            isLoading.value = true
            try {
                // --- START: 使用正確的邏輯替換 ---
                val user = eventApiService.getUser(userId)
                val itemsFromApi =
                        user.backpackItems.mapNotNull { backpackItem ->
                            try {
                                val itemDetails = eventApiService.getItem(backpackItem.itemId)
                                UserItem(item = itemDetails, quantity = backpackItem.quantity)
                            } catch (e: Exception) {
                                Log.e("MerchantUI", "獲取物品 ${backpackItem.itemId} 詳細資料失敗", e)
                                null
                            }
                        }
                allItems.clear()
                allItems.addAll(itemsFromApi)
                // --- END: 使用正確的邏輯替換 ---
            } catch (e: Exception) {
                Log.e("MerchantUI", "獲取物品失敗", e)
                snackbarHostState.showSnackbar("無法連接伺服器，請稍後再試。")
            } finally {
                isLoading.value = false
            }
        }
    }

    LaunchedEffect(key1 = userId) { fetchItems() }

    Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            contentWindowInsets = WindowInsets(0.dp)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                    painter = painterResource(id = R.drawable.merchant_background),
                    contentDescription = "背景",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
            )
            Column(
                    modifier =
                            Modifier.fillMaxSize()
                                    .padding(paddingValues)
                                    .windowInsetsPadding(WindowInsets.safeDrawing)
                                    .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
            ) {
                Text(
                        text = "神秘商人",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                )

                Card(
                        modifier = Modifier.fillMaxWidth().padding(top = 70.dp, bottom = 12.dp),
                        shape = RoundedCornerShape(2.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E3D3))
                ) {
                    Text(
                            text = "用你的物品來交換他珍藏的寶物吧！",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier =
                                    Modifier.padding(vertical = 12.dp, horizontal = 8.dp)
                                            .fillMaxWidth()
                    )
                }

                if (isLoading.value) {
                    CircularProgressIndicator(color = Color(0xFF4F7942))
                } else {
                    // 修正 1: 傳遞正確的 allItems 類型
                    KeyFragmentInventoryDisplay(allItems)
                }

                Spacer(modifier = Modifier.height(24.dp))

                MerchantOption(
                        title = "銅鑰匙碎片 x5 -> 銅鑰匙 x1",
                        description = "使用五個銅鑰匙碎片兌換一個銅鑰匙。",
                        onTradeClick = {
                            coroutineScope.launch {
                                try {
                                    // 修正 2: 這裡不再使用本地的 TradeRequest，而是從 com.ntou01157.hunter.api 引入
                                    val response =
                                            eventApiService.trade(TradeRequest(userId, "bronzeKey"))
                                    if (response.success) {
                                        snackbarHostState.showSnackbar(response.message)
                                        fetchItems()
                                    } else {
                                        snackbarHostState.showSnackbar(response.message)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MerchantUI", "交易失敗", e)
                                    snackbarHostState.showSnackbar("網路錯誤，無法交易。")
                                }
                            }
                        }
                )

                Spacer(modifier = Modifier.height(16.dp))

                MerchantOption(
                        title = "銀鑰匙碎片 x5 -> 銀鑰匙 x1",
                        description = "使用五個銀鑰匙碎片兌換一個銀鑰匙。",
                        onTradeClick = {
                            coroutineScope.launch {
                                try {
                                    // 修正 2: 這裡不再使用本地的 TradeRequest，而是從 com.ntou01157.hunter.api 引入
                                    val response =
                                            eventApiService.trade(TradeRequest(userId, "silverKey"))
                                    if (response.success) {
                                        snackbarHostState.showSnackbar(response.message)
                                        fetchItems()
                                    } else {
                                        snackbarHostState.showSnackbar(response.message)
                                    }
                                } catch (e: Exception) {
                                    Log.e("MerchantUI", "交易失敗", e)
                                    snackbarHostState.showSnackbar("網路錯誤，無法交易。")
                                }
                            }
                        }
                )
                Spacer(modifier = Modifier.height(16.dp))

                Button(
                        onClick = { onEventCompleted(true) }, // <- 修改這裡，離開時傳 true
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F7942))
                ) { Text(text = "離開") }
            }
        }
    }
}

// 刪除此處的資料類別定義，它們應該被放在 com.ntou01157.hunter.api.ApiService.kt 檔案中

@Composable
fun MerchantOption(title: String, description: String, onTradeClick: () -> Unit) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Text(text = description, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                    onClick = onTradeClick,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F7942))
            ) { Text(text = "交換") }
        }
    }
}

@Composable
// 將參數類型從 List<UserItemModel> 改為 List<UserItem>
fun KeyFragmentInventoryDisplay(allItems: List<UserItem>) {
    Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.Start) {
            Text(text = "你的鑰匙碎片", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            // UserItem 類別中直接有 item 屬性，其內有 itemName 屬性
            val bronzeFragment = allItems.find { it.item.itemName == "銅鑰匙碎片" }
            val silverFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }
            // count 屬性直接就是 Int，不需要 .value
            Text(text = "銅鑰匙碎片: ${bronzeFragment?.quantity ?: 0}")
            Text(text = "銀鑰匙碎片: ${silverFragment?.quantity ?: 0}")
        }
    }
}

// @Preview(showBackground = true)
// @Composable
// fun PreviewMerchantUI() {
//     MerchantUI(onEventCompleted = {})
// }
