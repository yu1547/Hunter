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
import kotlinx.coroutines.launch
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AncientTreeUI() {
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // 啟動時加載玩家背包物品
    LaunchedEffect(key1 = userId) {
        isLoading.value = true
        hasError.value = false
        try {
            val items = fetchUserItems(userId)
            allItems.clear()
            allItems.addAll(items)
            Log.d("AncientTreeUI", "成功獲取物品，物品數量: ${items.size}")
        } catch (e: Exception) {
            Log.e("AncientTreeUI", "獲取物品失敗", e)
            hasError.value = true
            errorMessage.value = "無法取得背包資料: ${e.message}"
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
                text = "古樹的祝福",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "獵人站在一棵高大的古樹前，樹的表面閃爍著微光。「我可以祝福你的道具，但需要一些特殊材料。」樹精的聲音響起。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            InventoryDisplay(allItems)
            Spacer(modifier = Modifier.height(24.dp))

            BlessingOption(
                title = "兌換銅鑰匙碎片",
                description = "交出普通的史萊姆黏液 x10 → 獲得銅鑰匙碎片 x1",
                onBlessClick = {
                    coroutineScope.launch {
                        // 在本地列表中尋找所需物品
                        val ordinarySlimeMucus = allItems.find { it.item.itemName == "普通的史萊姆黏液" }
                        val bronzeKeyFragment = allItems.find { it.item.itemName == "銅鑰匙碎片" }
                        val ancientTreeBranch = allItems.find { it.item.itemName == "古樹的枝幹" }

                        if (ordinarySlimeMucus != null && ordinarySlimeMucus.count.value >= 10) {
                            // 直接在本地狀態中修改數量
                            ordinarySlimeMucus.count.value -= 10

                            // 獲得銅鑰匙碎片
                            if (bronzeKeyFragment != null) {
                                bronzeKeyFragment.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個新的 UserItem
                                // TODO: 這裡需要有物品的完整資訊來新增，此處僅為邏輯示意
                                // allItems.add(UserItem(item = ... , count = mutableStateOf(1)))
                                snackbarHostState.showSnackbar("銅鑰匙碎片不存在，無法新增。")
                            }

                            // 獲得古樹的枝幹
                            if (ancientTreeBranch != null) {
                                ancientTreeBranch.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個新的 UserItem
                                snackbarHostState.showSnackbar("古樹的枝幹不存在，無法新增。")
                            }

                            snackbarHostState.showSnackbar("成功兌換銅鑰匙碎片並獲得古樹的枝幹！")
                        } else {
                            snackbarHostState.showSnackbar("普通的史萊姆黏液不足！")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            BlessingOption(
                title = "兌換銀鑰匙碎片",
                description = "交出黏稠的史萊姆黏液 x10 → 獲得銀鑰匙碎片 x1",
                onBlessClick = {
                    coroutineScope.launch {
                        // 在本地列表中尋找所需物品
                        val stickySlimeMucus = allItems.find { it.item.itemName == "黏稠的史萊姆黏液" }
                        val silverKeyFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }
                        val ancientTreeBranch = allItems.find { it.item.itemName == "古樹的枝幹" }

                        if (stickySlimeMucus != null && stickySlimeMucus.count.value >= 10) {
                            // 直接在本地狀態中修改數量
                            stickySlimeMucus.count.value -= 10

                            // 獲得銀鑰匙碎片
                            if (silverKeyFragment != null) {
                                silverKeyFragment.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個新的 UserItem
                                snackbarHostState.showSnackbar("銀鑰匙碎片不存在，無法新增。")
                            }

                            // 獲得古樹的枝幹
                            if (ancientTreeBranch != null) {
                                ancientTreeBranch.count.value += 1
                            } else {
                                // 如果物品不存在，需要模擬新增一個新的 UserItem
                                snackbarHostState.showSnackbar("古樹的枝幹不存在，無法新增。")
                            }

                            snackbarHostState.showSnackbar("成功兌換銀鑰匙碎片並獲得古樹的枝幹！")
                        } else {
                            snackbarHostState.showSnackbar("黏稠的史萊姆黏液不足！")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    coroutineScope.launch {
                        snackbarHostState.showSnackbar("你選擇不提供任何材料，古樹似乎有些失落。")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "我不想提供你任何的材料。")
            }
        }
    }
}

@Composable
fun BlessingOption(title: String, description: String, onBlessClick: () -> Unit) {
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
                onClick = onBlessClick,
                modifier = Modifier
                    .align(Alignment.End)
                    .padding(top = 8.dp)
            ) {
                Text(text = "提供材料")
            }
        }
    }
}

@Composable
fun InventoryDisplay(allItems: List<UserItem>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Text(text = "你的背包", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))

            val ordinarySlimeMucus = allItems.find { it.item.itemName == "普通的史萊姆黏液" }
            val stickySlimeMucus = allItems.find { it.item.itemName == "黏稠的史萊姆黏液" }
            val bronzeKeyFragment = allItems.find { it.item.itemName == "銅鑰匙碎片" }
            val silverKeyFragment = allItems.find { it.item.itemName == "銀鑰匙碎片" }

            Text(text = "普通的史萊姆黏液: ${ordinarySlimeMucus?.count?.value ?: 0}")
            Text(text = "黏稠的史萊姆黏液: ${stickySlimeMucus?.count?.value ?: 0}")
            Text(text = "銅鑰匙碎片: ${bronzeKeyFragment?.count?.value ?: 0}")
            Text(text = "銀鑰匙碎片: ${silverKeyFragment?.count?.value ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewAncientTreeUI() {
    AncientTreeUI()
}