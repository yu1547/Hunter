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
fun StonePileUI() {
    val userId = "6880f31469ff254ed2fb0cc1" // 假定使用者ID
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }

    // 為了範例，我們假設一個簡單的本地狀態來模擬每日觸發
    // 在實際應用中，這個狀態應從後端或本地持久化儲存（如 SharedPreference）獲取
    var hasTriggeredToday by remember { mutableStateOf(false) }

    // 載入玩家背包物品
    LaunchedEffect(key1 = userId) {
        isLoading.value = true
        hasError.value = false
        try {
            val items = fetchUserItems(userId)
            allItems.clear()
            allItems.addAll(items)
        } catch (e: Exception) {
            hasError.value = true
            Log.e("StonePileUI", "獲取物品失敗", e)
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
                text = "石堆下的碎片",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "你發現打卡點旁有一堆亂石，當你搬開其中一顆後，發現底下藏著一枚閃閃發亮的東西。",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.item3), // 假設這是石堆的圖片
                contentDescription = "Stone Pile",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            if (!hasTriggeredToday) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            val bronzeFragmentName = "銅鑰匙碎片"
                            val bronzeFragment = allItems.find { it.item.itemName == bronzeFragmentName }

                            if (bronzeFragment != null) {
                                // 模擬獲得獎勵的邏輯，直接修改本地狀態
                                bronzeFragment.count.value += 2
                                snackbarHostState.showSnackbar("你獲得了銅鑰匙碎片 x2！")
                            } else {
                                snackbarHostState.showSnackbar("背包中沒有銅鑰匙碎片，無法更新數量。")
                            }

                            hasTriggeredToday = true // 標記今日已觸發
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
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewStonePileUI() {
    StonePileUI()
}