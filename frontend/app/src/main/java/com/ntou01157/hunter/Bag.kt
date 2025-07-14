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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.features.json.serializer.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import androidx.compose.runtime.toMutableStateList

val client = HttpClient(CIO) {
    install(JsonFeature) {
        serializer = KotlinxSerializer(
            Json {
                ignoreUnknownKeys = true // 避免多餘欄位造成錯誤
            }
        )
    }
}

@Serializable
data class ItemDto(
    val _id: String,
    val itemFunc: String,
    val itemName: String,
    val itemType: Int,
    val itemEffect: String,
    val maxStack: Int,
    val itemMethod: String,
    val itemRarity: Int,
    val isBlend: Boolean,
    val itemIcon: String // 假設是圖片 URL 或名稱
)

@Serializable
data class InventoryItemDto(
    val itemId: String,
    val quantity: Int,
    val item: ItemDto? = null
)

data class Item(
    val itemid: String,
    val itemFunc: String,
    val itemName: String,
    val itemType: Int,
    val itemEffect: String,
    initialCount: Int,
    val itemMethod: String,
    val itemRarity: String,
    val isblend: Boolean,
    val itemIconUrl: String? = null // 直接用 URL 方式
) {
    var count: MutableState<Int> = mutableStateOf(initialCount)
}

suspend fun fetchItemsAndInventory(userId: String): MutableList<Item> {
    val itemDtos = client.get<List<ItemDto>>("https://yourbackend.com/api/items")
    val inventoryDtos = client.get<List<InventoryItemDto>>("https://yourbackend.com/api/$userId/inventory")

    val items = itemDtos.map { dto ->
        Item(
            itemid = dto._id,
            itemFunc = dto.itemFunc,
            itemName = dto.itemName,
            itemType = dto.itemType,
            itemEffect = dto.itemEffect,
            initialCount = 0,
            itemMethod = dto.itemMethod,
            itemRarity = dto.itemRarity.toString(),
            isblend = dto.isBlend,
            itemIconUrl = dto.itemIcon
        )
    }.toMutableList()

    inventoryDtos.forEach { inv ->
        val item = items.find { it.itemid == inv.itemId }
        if (item != null) {
            item.count.value = inv.quantity
        }
    }

    return items.toMutableStateList()
}

class BagActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val userId = "PUT_YOUR_USER_ID_HERE" // 這裡請換成實際 userId
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "bag") {
                composable("bag") { BagScreen(navController, userId) }
            }
        }
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun BagScreen(navController: NavHostController, userId: String) {
    var allItems by remember { mutableStateOf(mutableStateListOf<Item>()) }
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var filterState by remember { mutableStateOf(0) }
    var showCraftDialog by remember { mutableStateOf(false) }
    var currentRecipeIndex by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }

    // 預設 recipes，要改成你自己的 recipe list
    val recipes = remember { listOf<CraftingRecipe>() }

    // 由 selectedItem 和 recipes 計算出 matchingRecipes，放在 remember 裡，避免重複計算與狀態問題
    val matchingRecipes = remember(selectedItem) {
        recipes.filter { it.requiredItems.containsKey(selectedItem?.itemid) }
    }
    val currentRecipe = matchingRecipes.getOrNull(currentRecipeIndex)

    LaunchedEffect(Unit) {
        try {
            allItems = fetchItemsAndInventory(userId)
        } catch (e: Exception) {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("載入資料失敗: ${e.message}")
            }
        } finally {
            loading = false
        }
    }

    if (loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
    } else {
        val filteredItems = when (filterState) {
            1 -> allItems.filter { it.itemType == 0 && it.count.value > 0 }
            2 -> allItems.filter { it.itemType == 1 && it.count.value > 0 }
            else -> allItems.filter { it.count.value > 0 }
        }

        Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3DCDC))
                    .padding(horizontal = 16.dp)
                    .padding(paddingValues)
            ) {
                IconButton(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.padding(top = 25.dp, bottom = 4.dp)
                ) {
                    // 假設本地有 ic_home 資源
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
                                    // 使用 Coil 載入遠端圖片
                                    AsyncImage(
                                        model = item.itemIconUrl ?: "",
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
                                        AsyncImage(
                                            model = item.itemIconUrl ?: "",
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(200.dp)
                                                .padding(bottom = 8.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
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
                                            Button(onClick = {
                                                coroutineScope.launch {
                                                    try {
                                                        client.post<String>("https://yourbackend.com/api/$userId/craft") {
                                                            contentType(ContentType.Application.Json)
                                                            body = mapOf("resultItemId" to (currentRecipe?.resultItemId ?: ""))
                                                        }
                                                        allItems = fetchItemsAndInventory(userId)
                                                        snackbarHostState.showSnackbar("合成成功！")
                                                        selectedItem = null
                                                        showCraftDialog = false
                                                        currentRecipeIndex = 0
                                                    } catch (e: Exception) {
                                                        snackbarHostState.showSnackbar("合成失敗：${e.message}")
                                                    }
                                                }
                                            }) {
                                                Text("合成")
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
                            Text(
                                "✕",
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .clickable {
                                        showCraftDialog = false
                                        currentRecipeIndex = 0
                                    },
                                fontSize = 24.sp
                            )
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            resultItem?.let {
                                AsyncImage(
                                    model = it.itemIconUrl ?: "",
                                    contentDescription = it.itemName,
                                    modifier = Modifier.size(100.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (matchingRecipes.size > 1) {
                                    Text(
                                        "<",
                                        fontSize = 24.sp,
                                        modifier = Modifier
                                            .clickable {
                                                currentRecipeIndex =
                                                    (currentRecipeIndex - 1 + matchingRecipes.size) % matchingRecipes.size
                                            }
                                            .padding(8.dp)
                                    )
                                }

                                currentRecipe.requiredItems.forEach { (id, count) ->
                                    val material = allItems.find { it.itemid == id }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        material?.let {
                                            AsyncImage(
                                                model = it.itemIconUrl ?: "",
                                                contentDescription = it.itemName,
                                                modifier = Modifier.size(50.dp)
                                            )
                                            Text("x$count")
                                        }
                                    }
                                }

                                if (matchingRecipes.size > 1) {
                                    Text(
                                        ">",
                                        fontSize = 24.sp,
                                        modifier = Modifier
                                            .clickable {
                                                currentRecipeIndex =
                                                    (currentRecipeIndex + 1) % matchingRecipes.size
                                            }
                                            .padding(8.dp)
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch {
                                // 這裡你原本有本地 CraftingSystem 的合成邏輯，
                                // 若用後端合成，這段可省略，改用呼叫 API 並重新拉資料。
                                // 暫時呼叫成功後清除狀態
                                selectedItem = null
                                showCraftDialog = false
                                currentRecipeIndex = 0
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
}

// 請自行定義 CraftingRecipe 與 CraftingSystem
data class CraftingRecipe(
    val resultItemId: String,
    val requiredItems: Map<String, Int>
)

// (CraftingSystem 可以是你本地合成判斷的工具類別，視你需求決定要不要用)
object CraftingSystem {
    fun craftItem(items: List<Item>, recipe: CraftingRecipe): Boolean {
        // 判斷材料是否足夠（簡化版）
        recipe.requiredItems.forEach { (id, count) ->
            val item = items.find { it.itemid == id }
            if (item == null || item.count.value < count) return false
        }
        // 扣除材料
        recipe.requiredItems.forEach { (id, count) ->
            val item = items.find { it.itemid == id }
            if (item != null) {
                item.count.value -= count
            }
        }
        // 增加合成品數量（要自行處理）
        val resultItem = items.find { it.itemid == recipe.resultItemId }
        if (resultItem != null) {
            resultItem.count.value += 1
        }
        return true
    }
}
