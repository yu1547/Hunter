package com.ntou01157.hunter.ui.event_ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import com.ntou01157.hunter.models.model_api.UserItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import kotlinx.coroutines.CoroutineScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlimeAttackUI() {
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    var damage by remember { mutableIntStateOf(0) }
    var damagePerClick by remember { mutableIntStateOf(1) }
    var timeRemaining by remember { mutableIntStateOf(15) }
    var isAttackPhase by remember { mutableStateOf(false) }
    var isGameOver by remember { mutableStateOf(false) }
    var isTorchUsed by remember { mutableStateOf(false) }

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
            Log.e("SlimeAttackUI", "獲取物品失敗", e)
        } finally {
            isLoading.value = false
        }
    }

    // 計時器邏輯
    LaunchedEffect(isAttackPhase) {
        if (isAttackPhase) {
            while (timeRemaining > 0) {
                delay(1000)
                timeRemaining--
            }
            isAttackPhase = false
            isGameOver = true
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
                text = "打扁史萊姆",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text(
                text = "路上撞見了一隻史萊姆，他想要搶奪你的財寶，快攻擊他避免寶物被搶走！",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            if (isAttackPhase) {
                Text(text = "剩餘時間: $timeRemaining 秒", style = MaterialTheme.typography.titleMedium)
                Text(text = "傷害: $damage", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                // 史萊姆攻擊區域
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(200.dp)
                        .clickable { damage += damagePerClick }
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.item2),
                        contentDescription = "Slime",
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else if (isGameOver) {
                // 遊戲結束畫面
                SlimeGameOverUI(
                    totalDamage = damage,
                    allItems = allItems,
                    onStartAgain = {
                        isGameOver = false
                        damage = 0
                        timeRemaining = 15
                        damagePerClick = 1
                        isTorchUsed = false
                        isAttackPhase = false
                    },
                    snackbarHostState = snackbarHostState,
                    coroutineScope = coroutineScope
                )
            } else {
                Button(
                    onClick = { isAttackPhase = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "開始攻擊")
                }
                Spacer(modifier = Modifier.height(16.dp))
                val torchItem = allItems.find { it.item.itemName == "火把" }
                val hasTorch = torchItem != null && torchItem.count.value > 0
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (hasTorch && !isTorchUsed) {
                                // 模擬使用火把
                                torchItem!!.count.value--
                                damagePerClick = 5 // 假設火把能大幅提高傷害
                                isTorchUsed = true
                                snackbarHostState.showSnackbar("你使用了火把，傷害大幅提升！")
                            } else if (isTorchUsed) {
                                snackbarHostState.showSnackbar("火把已在使用中。")
                            } else {
                                snackbarHostState.showSnackbar("你沒有火把可以提高傷害。")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasTorch && !isTorchUsed
                ) {
                    Text(text = "使用火把 (提高傷害)")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlimeGameOverUI(
    totalDamage: Int,
    allItems: MutableList<UserItem>,
    onStartAgain: () -> Unit,
    snackbarHostState: SnackbarHostState,
    coroutineScope: CoroutineScope
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "遊戲結束！", style = MaterialTheme.typography.headlineMedium)
        Text(text = "你對史萊姆造成了 $totalDamage 點傷害。", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "你獲得了以下獎勵：", style = MaterialTheme.typography.titleSmall)
        // 獎勵邏輯
        val rewards = mutableListOf<String>()
        if (totalDamage >= 5) {
            val rewardName = "普通的史萊姆黏液"
            rewards.add(rewardName)
            val item = allItems.find { it.item.itemName == rewardName }
            item?.count?.value = (item?.count?.value ?: 0) + 1
        }
        if (totalDamage >= 10) {
            val rewardName = "黏稠的史萊姆黏液"
            rewards.add(rewardName)
            val item = allItems.find { it.item.itemName == rewardName }
            item?.count?.value = item.count.value + 1
        }
        if (totalDamage >= 15) {
            val rewardName = "神秘的史萊姆黏液"
            rewards.add(rewardName)
            val item = allItems.find { it.item.itemName == rewardName }
            item?.count?.value = item.count.value + 1
        }

        if (rewards.isNotEmpty()) {
            rewards.forEach { reward ->
                Text(text = reward, style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            Text(text = "你沒有擊敗史萊姆，沒有獲得獎勵。", style = MaterialTheme.typography.bodyLarge)
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onStartAgain, modifier = Modifier.fillMaxWidth()) {
            Text(text = "再玩一次")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PreviewSlimeAttackUI() {
    SlimeAttackUI()
}