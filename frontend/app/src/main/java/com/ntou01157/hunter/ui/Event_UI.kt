package com.ntou01157.hunter.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Image
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

// 導入 Backpack.data 中的函式
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import com.ntou01157.hunter.models.model_api.UserItem
import android.util.Log
import androidx.compose.foundation.background
import kotlin.random.Random

// 根據你的描述，這裡需要定義掉落物池和掉落規則的資料結構
data class DropPool(
    val poolType: String,
    val rarity: Int,
    val itemIds: List<String>
)

data class DropRule(
    val difficulty: Int,
    val dropCountRange: List<Int>,
    val rarityChances: Map<Int, Int>,
    val guaranteedRarity: Int?
)

// 寶箱的掉落物池和規則
val dropPools = listOf(
    DropPool(poolType = "chest_bronze", rarity = 2, itemIds = listOf("item_r2_1", "item_r2_2")),
    DropPool(poolType = "chest_bronze", rarity = 3, itemIds = listOf("item_r3_1", "item_r3_2")),
    DropPool(poolType = "chest_bronze", rarity = 4, itemIds = listOf("item_r4_1")),
    DropPool(poolType = "chest_silver", rarity = 2, itemIds = listOf("item_r2_3", "item_r2_4")),
    DropPool(poolType = "chest_silver", rarity = 3, itemIds = listOf("item_r3_3", "item_r3_4")),
    DropPool(poolType = "chest_silver", rarity = 4, itemIds = listOf("item_r4_2", "item_r4_3")),
    DropPool(poolType = "chest_silver", rarity = 5, itemIds = listOf("item_r5_1")),
    DropPool(poolType = "chest_gold", rarity = 3, itemIds = listOf("item_r3_5", "item_r3_6")),
    DropPool(poolType = "chest_gold", rarity = 4, itemIds = listOf("item_r4_4", "item_r4_5")),
    DropPool(poolType = "chest_gold", rarity = 5, itemIds = listOf("item_r5_2", "item_r5_3"))
)

val dropRules = listOf(
    DropRule(
        difficulty = 3,
        dropCountRange = listOf(3, 5),
        rarityChances = mapOf(2 to 50, 3 to 20, 4 to 10),
        guaranteedRarity = 3
    ),
    DropRule(
        difficulty = 4,
        dropCountRange = listOf(3, 5),
        rarityChances = mapOf(2 to 30, 3 to 30, 4 to 20, 5 to 10),
        guaranteedRarity = 4
    ),
    DropRule(
        difficulty = 5,
        dropCountRange = listOf(3, 5),
        rarityChances = mapOf(3 to 50, 4 to 30, 5 to 20),
        guaranteedRarity = 5
    )
)

// 掉落邏輯函式
fun calculateDrops(difficulty: Int): List<String> {
    val rule = dropRules.find { it.difficulty == difficulty } ?: return emptyList()
    val drops = mutableListOf<String>()

    val totalDropCount = Random.nextInt(rule.dropCountRange[0], rule.dropCountRange[1] + 1)
    var remainingDropCount = totalDropCount

    // 處理固定掉落
    rule.guaranteedRarity?.let { guaranteedRarity ->
        val pool = dropPools.filter { it.rarity == guaranteedRarity }
        if (pool.isNotEmpty()) {
            val randomPool = pool.random()
            drops.add(randomPool.itemIds.random())
            remainingDropCount--
        }
    }

    // 處理剩餘掉落
    while (remainingDropCount > 0) {
        val rarityRoll = Random.nextInt(1, 101)
        var cumulativeChance = 0
        var selectedRarity: Int? = null

        for ((rarity, chance) in rule.rarityChances) {
            cumulativeChance += chance
            if (rarityRoll <= cumulativeChance) {
                selectedRarity = rarity
                break
            }
        }

        selectedRarity?.let { rarity ->
            val pool = dropPools.filter { it.rarity == rarity }
            if (pool.isNotEmpty()) {
                val randomPool = pool.random()
                drops.add(randomPool.itemIds.random())
                remainingDropCount--
            }
        }
    }
    return drops
}

// 模擬更新使用者背包的函式，這在實際應用中應該是呼叫後端 API
fun updateInventory(userId: String, drops: List<String>) {
    // 這裡的實作會呼叫後端 API 更新使用者物品
    Log.d("Event_UI", "為使用者 $userId 更新背包，獲得物品：$drops")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TreasureBoxUI(onEventCompleted: (message: String) -> Unit) {
    val userId = "6880f31469ff254ed2fb0cc1" // 參考 Bag_UI.kt
    val coroutineScope = rememberCoroutineScope()
    val allItems = remember { mutableStateListOf<UserItem>() }
    val snackbarHostState = remember { SnackbarHostState() }

    // 啟動時加載玩家背包物品
    LaunchedEffect(key1 = userId) {
        try {
            val items = fetchUserItems(userId)
            allItems.clear()
            allItems.addAll(items)
        } catch (e: Exception) {
            Log.e("TreasureBoxUI", "獲取物品失敗", e)
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
            Image(
                painter = painterResource(id = R.drawable.chest),
                contentDescription = "Treasure Box",
                modifier = Modifier.size(200.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))
            KeyInventoryDisplay(allItems)
            Spacer(modifier = Modifier.height(24.dp))

            TreasureBoxOption(
                keyName = "銅鑰匙",
                onOpenClick = {
                    coroutineScope.launch {
                        val bronzeKey = allItems.find { it.item.itemName == "銅鑰匙" }
                        if (bronzeKey != null && bronzeKey.count.value > 0) {
                            bronzeKey.count.value -= 1
                            val drops = calculateDrops(difficulty = 3)
                            updateInventory(userId, drops)
                            onEventCompleted("你用銅鑰匙打開了寶箱，獲得積分+15和道具: ${drops.joinToString()}")
                        } else {
                            snackbarHostState.showSnackbar("你沒有銅鑰匙，無法開啟寶箱。")
                        }
                    }
                }
            )
            // ... (其他鑰匙選項，邏輯類似)
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { onEventCompleted("你選擇了離開，寶箱依然靜靜地躺在那裡。") },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "離開")
            }
        }
    }
}

// 由於你未提供，此處為 TreasureBoxOption 和 KeyInventoryDisplay 的模擬實作
@Composable
fun TreasureBoxOption(keyName: String, onOpenClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = "使用 $keyName 開啟寶箱")
            Button(onClick = onOpenClick) {
                Text(text = "開啟")
            }
        }
    }
}

@Composable
fun KeyInventoryDisplay(allItems: List<UserItem>) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "你的鑰匙")
            val bronzeKey = allItems.find { it.item.itemName == "銅鑰匙" }
            val silverKey = allItems.find { it.item.itemName == "銀鑰匙" }
            val goldKey = allItems.find { it.item.itemName == "金鑰匙" }
            Text(text = "銅鑰匙: ${bronzeKey?.count?.value ?: 0}")
            Text(text = "銀鑰匙: ${silverKey?.count?.value ?: 0}")
            Text(text = "金鑰匙: ${goldKey?.count?.value ?: 0}")
        }
    }
}


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
                // 整合寶箱事件
                "chest" -> TreasureBoxUI(onEventCompleted)
                "daily", "bless" -> OptionsUI(event, onEventCompleted)
                else -> {
                    // 處理未知事件類型
                    Text("未知事件類型: ${event.type}")
                    Button(onClick = { onEventCompleted("事件已結束") }) {
                        Text("返回")
                    }
                }
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
    val userId = "6880f31469ff254ed2fb0cc1" // 參考 Bag_UI.kt

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
                            val result = EventRepository(apiService).completeEvent(
                                event.id,
                                userId,
                                selectedOption = exchangeOption.option
                            )
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
                enabled = !isEventProcessing
            ) {
                Text(text = exchangeOption.option)
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isEventProcessing) {
            CircularProgressIndicator(modifier = Modifier.padding(16.dp))
        }
    }
}