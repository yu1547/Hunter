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
import com.ntou01157.hunter.models.model_api.UserItem
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import kotlinx.coroutines.launch
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureBoxUI() {
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
            Log.e("TreasureBoxUI", "獲取物品失敗", e)
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

            // 顯示目前擁有的鑰匙數量
            KeyInventoryDisplay(allItems)
            Spacer(modifier = Modifier.height(24.dp))

            TreasureBoxOption(
                keyName = "銅鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        val bronzeKey = allItems.find { it.item.itemName == "銅鑰匙" }
                        if (bronzeKey != null && bronzeKey.count.value > 0) {
                            // 模擬使用鑰匙，本地扣除數量
                            bronzeKey.count.value -= 1
                            snackbarHostState.showSnackbar("你用銅鑰匙打開了寶箱，獲得積分+15和隨機道具！")
                        } else {
                            snackbarHostState.showSnackbar("你沒有銅鑰匙，無法開啟寶箱。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TreasureBoxOption(
                keyName = "銀鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        val silverKey = allItems.find { it.item.itemName == "銀鑰匙" }
                        if (silverKey != null && silverKey.count.value > 0) {
                            // 模擬使用鑰匙，本地扣除數量
                            silverKey.count.value -= 1
                            snackbarHostState.showSnackbar("你用銀鑰匙打開了寶箱，獲得積分+25和隨機道具！")
                        } else {
                            snackbarHostState.showSnackbar("你沒有銀鑰匙，無法開啟寶箱。")
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(16.dp))
            TreasureBoxOption(
                keyName = "金鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        val goldKey = allItems.find { it.item.itemName == "金鑰匙" }
                        if (goldKey != null && goldKey.count.value > 0) {
                            // 模擬使用鑰匙，本地扣除數量
                            goldKey.count.value -= 1
                            snackbarHostState.showSnackbar("你用金鑰匙打開了寶箱，獲得積分+40和隨機道具！")
                        } else {
                            snackbarHostState.showSnackbar("你沒有金鑰匙，無法開啟寶箱。")
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
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "離開")
            }
        }
    }
}

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
            val bronzeKey = allItems.find { it.item.itemName == "銅鑰匙" }
            val silverKey = allItems.find { it.item.itemName == "銀鑰匙" }
            val goldKey = allItems.find { it.item.itemName == "金鑰匙" }
            Text(text = "銅鑰匙: ${bronzeKey?.count?.value ?: 0}")
            Text(text = "銀鑰匙: ${silverKey?.count?.value ?: 0}")
            Text(text = "金鑰匙: ${goldKey?.count?.value ?: 0}")
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewTreasureBoxUI() {
    TreasureBoxUI()
}