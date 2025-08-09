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
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import kotlinx.coroutines.launch
import android.util.Log
import com.ntou01157.hunter.models.model_api.Item // 假設有 Item 模型來新增物品
import androidx.compose.runtime.mutableStateListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MerchantUI() {
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // 啟動時加載玩家背包物品
    LaunchedEffect(key1 = userId) {
        isLoading.value = true
        hasError.value = false
        try {
            val items = fetchUserItems(userId)
            allItems.clear()
            allItems.addAll(items)
        } catch (e: Exception) {
            hasError.value = true
            Log.e("MerchantUI", "獲取物品失敗", e)
        } finally {
            isLoading.value = false
        }
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
                text = "神秘商人的試煉",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "一位背著大袋子的神秘商人出現在你面前。他看起來像是走遍世界的收藏家。「你願意用你手上的鑰匙碎片與我交易嗎？我有更值得的東西給你。」他咧嘴一笑。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // 顯示目前擁有的鑰匙碎片數量
            KeyFragmentInventoryDisplay(allItems)
            Spacer(modifier = Modifier.height(24.dp))

            TradeOption(
                title = "交換銀鑰匙碎片",
                description = "交出 銅鑰匙碎片 x3 → 獲得 銀鑰匙碎片 x1",
                onTradeClick = {
                    coroutineScope.launch {
                        val bronzeFragment = allItems.find { it.item.itemName == "銅鑰匙碎片" }
                        val silverFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }
                        if (bronzeFragment != null && bronzeFragment.count.value >= 3) {
                            // 扣除材料，並給予獎勵
                            bronzeFragment.count.value -= 3
                            if (silverFragment != null) {
                                silverFragment.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個 UserItem
                                // TODO: 實際應用中需從後端取得物品完整資訊
                                snackbarHostState.showSnackbar("銀鑰匙碎片不存在，無法新增。")
                            }
                            snackbarHostState.showSnackbar("交易成功！你獲得了銀鑰匙碎片 x1！")
                        } else {
                            snackbarHostState.showSnackbar("銅鑰匙碎片不足，無法交易。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TradeOption(
                title = "交換金鑰匙碎片",
                description = "交出 銀鑰匙碎片 x3 → 獲得 金鑰匙碎片 x1",
                onTradeClick = {
                    coroutineScope.launch {
                        val silverFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }
                        val goldFragment = allItems.find { it.item.itemName == "金鑰匙碎片" }
                        if (silverFragment != null && silverFragment.count.value >= 3) {
                            // 扣除材料，並給予獎勵
                            silverFragment.count.value -= 3
                            if (goldFragment != null) {
                                goldFragment.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個 UserItem
                                // TODO: 實際應用中需從後端取得物品完整資訊
                                snackbarHostState.showSnackbar("金鑰匙碎片不存在，無法新增。")
                            }
                            snackbarHostState.showSnackbar("交易成功！你獲得了金鑰匙碎片 x1！")
                        } else {
                            snackbarHostState.showSnackbar("銀鑰匙碎片不足，無法交易。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("你選擇什麼都不做，神秘商人咧嘴一笑，轉身離去。")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "什麼都不做")
            }
        }
    }
}

@Composable
fun TradeOption(title: String, description: String, onTradeClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(text = description, style = MaterialTheme.typography.bodyMedium)
            Button(
                onClick = onTradeClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text(text = "交易")
            }
        }
    }
}

@Composable
fun KeyFragmentInventoryDisplay(allItems: List<UserItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "你的鑰匙碎片", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            val bronzeFragment = allItems.find { it.item.itemName == "銅鑰匙碎片" }
            val silverFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }
            val goldFragment = allItems.find { it.item.itemName == "金鑰匙碎片" }
            Text(text = "銅鑰匙碎片: ${bronzeFragment?.count?.value ?: 0}")
            Text(text = "銀鑰匙碎片: ${silverFragment?.count?.value ?: 0}")
            Text(text = "金鑰匙碎片: ${goldFragment?.count?.value ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewMerchantUI() {
    MerchantUI()
}