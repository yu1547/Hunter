package com.ntou01157.hunter

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope


// 所有可用的物品模板清單，預設初始數量為 0
val allItemsTemplate = listOf(
    Item(1, "用於開啟寶箱", "金鑰匙", 1, "開啟寶箱", 0, "由銀鑰匙合成", "UR", false, R.drawable.item1),
    Item(2, "讓我充數一下", "史萊姆", 1, "就很可愛讓你觀賞", 0, "路邊撿到的", "S", false, R.drawable.item2),
    Item(3, "用來合成出史萊姆的素材", "史萊姆球", 0, "史萊姆球是史萊姆身體的一部分", 0, "做任務獲得", "R", true, R.drawable.item3),
    Item(4, "就是水", "水滴", 0, "可以用來合成各種素材", 0, "做任務得到", "R", true, R.drawable.item4),
    Item(5, "黃金碎片", "金鑰匙碎片", 0, "可以用來合成金鑰匙", 0, "做任務得到", "R", true, R.drawable.item5),
)

data class Item(
    val itemid: Int,
    val itemFunc: String,
    val itemName: String,
    val itemType: Int,
    val itemEffect: String,
    val initialCount: Int,
    val itemMethod: String,
    val itemRarity: String,
    val isblend: Boolean,
    val imageResId: Int
) {
    var count: MutableState<Int> = mutableStateOf(initialCount)
}

class BagActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "bag") {
                composable("bag") { BagScreen(navController) }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun BagScreen(navController: NavHostController) {
    // 初始化物品列表(之後要改成從資料庫抓)
    val allItems = remember {
        mutableStateListOf(
            Item(1, "用於開啟寶箱", "金鑰匙", 1, "開啟寶箱", 2, "由銀鑰匙合成", "UR", false, R.drawable.item1),
            Item(2, "讓我充數一下", "史萊姆", 1, "就很可愛讓你觀賞", 4, "路邊撿到的", "S", false, R.drawable.item2),
            Item(3, "用來合成出史萊姆的素材", "史萊姆球", 0, "史萊姆球是史萊姆身體的一部分", 4, "做任務獲得", "R", true, R.drawable.item3),
            Item(4, "就是水", "水滴", 0, "可以用來合成各種素材", 3, "做任務得到", "R", true, R.drawable.item4),
            Item(5, "黃金碎片", "金鑰匙碎片", 0, "可以用來合成金鑰匙", 3, "做任務得到", "R", true, R.drawable.item5),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var filterState by remember { mutableStateOf(0) }
    var showCraftDialog by remember { mutableStateOf(false) }
    var currentRecipeIndex by remember { mutableStateOf(0) }

    val filteredItems = when (filterState) {
        1 -> allItems.filter { it.itemType == 0 && it.count.value > 0 }
        2 -> allItems.filter { it.itemType == 1 && it.count.value > 0 }
        else -> allItems.filter { it.count.value > 0 }
    }

    // 取得目前選中的物品能觸發的合成配方列表
    val matchingRecipes by derivedStateOf {
        recipes.filter { it.requiredItems.containsKey(selectedItem?.itemid) }
    }
    val currentRecipe = matchingRecipes.getOrNull(currentRecipeIndex)

    //頁面設計區
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3DCDC))
                .padding(horizontal = 16.dp)
                .padding(paddingValues) // ⬅️ Scaffold padding
        ) {
            IconButton(
                onClick = { navController.navigate("main") },
                modifier = Modifier.padding(top = 25.dp, bottom = 4.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_home),
                    contentDescription = "回首頁",
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFEFEFEF))
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    "(全部)",
                    modifier = Modifier.clickable { filterState = 0 },
                    color = if (filterState == 0) Color.Black else Color.Gray
                )
                Text(
                    "(碎片)",
                    modifier = Modifier.clickable { filterState = 1 },
                    color = if (filterState == 1) Color.Black else Color.Gray
                )
                Text(
                    "(道具)",
                    modifier = Modifier.clickable { filterState = 2 },
                    color = if (filterState == 2) Color.Black else Color.Gray
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .width(320.dp)
                        .fillMaxHeight(0.75f)
                        .background(Color(0xFFDADADA))
                        .padding(16.dp)
                ) {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredItems) { item ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .background(Color.White)
                                    .clickable { selectedItem = item },
                                contentAlignment = Alignment.BottomEnd
                            ) {
                                Image(
                                    painter = painterResource(id = item.imageResId),
                                    contentDescription = item.itemName,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Text(
                                    "${item.count.value}",
                                    color = Color.Black,
                                    modifier = Modifier.padding(4.dp),
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }

                    selectedItem?.let { item ->
                        AlertDialog(
                            onDismissRequest = { selectedItem = null },
                            confirmButton = {},
                            title = {
                                Box(Modifier.fillMaxWidth()) {
                                    Text(" ")
                                    Text(
                                        "✕",
                                        modifier = Modifier.align(Alignment.TopEnd)
                                            .clickable { selectedItem = null },
                                        fontSize = 24.sp
                                    )
                                }
                            },
                            text = {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Image(
                                        painter = painterResource(id = item.imageResId),
                                        contentDescription = null,
                                        modifier = Modifier.size(200.dp).padding(bottom = 8.dp)
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text("稀有度：${item.itemRarity}")
                                        Text("擁有 ${item.count.value} 件")
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Text("物品介紹：", fontSize = 16.sp)
                                        Text(
                                            text = item.itemEffect,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                    }
                                    if (item.isblend) {
                                        Spacer(modifier = Modifier.height(20.dp))
                                        Button(
                                            onClick = {
                                                currentRecipeIndex = 0
                                                showCraftDialog = true
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                                .padding(horizontal = 32.dp)
                                        ) {
                                            Text("前往合成")
                                        }
                                    }

                                }

                            },
                            shape = RoundedCornerShape(16.dp)
                        )
                    }
                }
            }
        }

        // 合成彈窗
        // 找出所有符合目前選擇 item 的 recipe
        val matchingRecipes = remember(selectedItem) {
            recipes.filter { it.requiredItems.containsKey(selectedItem?.itemid) }
        }
        var currentRecipeIndex by remember { mutableStateOf(0) }

        if (showCraftDialog && currentRecipe != null) {
            val resultItem = allItems.find { it.itemid == currentRecipe.resultItemId }

            AlertDialog(
                onDismissRequest = {
                    showCraftDialog = false
                    currentRecipeIndex = 0
                },
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Text("合成物", modifier = Modifier.align(Alignment.Center))
                        Text("✕", modifier = Modifier.align(Alignment.TopEnd).clickable {
                            showCraftDialog = false
                            currentRecipeIndex = 0
                        }, fontSize = 24.sp)
                    }
                },
                text = {
                    // 顯示合成後物品
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        resultItem?.let {
                            Image(
                                painter = painterResource(id = it.imageResId),
                                contentDescription = it.itemName,
                                modifier = Modifier.size(100.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        // 顯示需要的材料
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (matchingRecipes.size > 1) {
                                Text("<", fontSize = 24.sp, modifier = Modifier.clickable {
                                    currentRecipeIndex = (currentRecipeIndex - 1 + matchingRecipes.size) % matchingRecipes.size
                                }.padding(8.dp))
                            }

                            currentRecipe.requiredItems.forEach { (id, count) ->
                                val material = allItems.find { it.itemid == id }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    material?.let {
                                        Image(
                                            painter = painterResource(id = it.imageResId),
                                            contentDescription = it.itemName,
                                            modifier = Modifier.size(50.dp)
                                        )
                                        Text("x$count")
                                    }
                                }
                            }

                            if (matchingRecipes.size > 1) {
                                Text(">", fontSize = 24.sp, modifier = Modifier.clickable {
                                    currentRecipeIndex = (currentRecipeIndex + 1) % matchingRecipes.size
                                }.padding(8.dp))
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = {
                        currentRecipe?.let { recipe ->
                            val crafted = CraftingSystem.craftItem(allItems, recipe)
                            // 顯示材料不足的訊息
                            if (!crafted) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("材料不足，無法合成")
                                }
                            } else {
                                // 合成成功後清除選擇與對話框
                                selectedItem = null
                                showCraftDialog = false
                                currentRecipeIndex = 0
                            }
                        }
                    }) {
                        Text("合成")
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}