package com.ntou01157.hunter

import android.os.Bundle
import android.util.Log
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
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.models.model_api.UserItem
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import com.ntou01157.hunter.Backpack.data.craftItem
import com.ntou01157.hunter.api.RetrofitClient
import kotlinx.coroutines.launch

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

@Composable
fun BagScreen(navController: NavHostController) {
    var userIdState by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val email = FirebaseAuth.getInstance().currentUser?.email
                ?: run {
                    Log.e("BagScreen", "尚未登入，無法取得 email")
                    return@LaunchedEffect
                }
            val user = RetrofitClient.apiService.getUserByEmail(email) // 回傳單一 User（若你是 List 就改 firstOrNull）
            userIdState = user.id
            Log.d("BagScreen", "取得 userId=${userIdState}")
        } catch (e: Exception) {
            Log.e("BagScreen", "以 email 取得 userId 失敗：${e.message}", e)
        }
    }

    // 物品列表與 UI 狀態
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedItem by remember { mutableStateOf<UserItem?>(null) }
    var filterState by remember { mutableStateOf(0) }
    var showCraftDialog by remember { mutableStateOf(false) }

    // 3) 有了 userId 才去抓背包
    LaunchedEffect(userIdState) {
        val userId = userIdState ?: return@LaunchedEffect
        isLoading.value = true
        hasError.value = false
        try {
            Log.d("BagScreen", "開始獲取用戶物品，用戶ID: $userId")
            val items = fetchUserItems(userId)
            allItems.clear()
            allItems.addAll(items)
            Log.d("BagScreen", "成功獲取物品，物品數量: ${items.size}")
            if (items.isEmpty()) {
                errorMessage.value = "背包中沒有物品"
            }
        } catch (e: Exception) {
            Log.e("BagScreen", "獲取物品失敗", e)
            hasError.value = true
            errorMessage.value = "無法取得背包資料: ${e.message}"
        } finally {
            isLoading.value = false
        }
    }

    val filteredItems = when (filterState) {
        1 -> allItems.filter { it.item.itemType == 0 && it.count.value > 0 }
        2 -> allItems.filter { it.item.itemType == 1 && it.count.value > 0 }
        else -> allItems.filter { it.count.value > 0 }
    }

    // 如果選中的是素材，找出可合成的結果物品
    val resultItem = remember(selectedItem) {
        if (selectedItem?.item?.itemType == 0 && selectedItem?.item?.resultId != null) {
            allItems.find { it.item.itemId == selectedItem?.item?.resultId }
        } else null
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
                Text("(全部)", modifier = Modifier.clickable { filterState = 0 },
                    color = if (filterState == 0) Color.Black else Color.Gray)
                Text("(碎片)", modifier = Modifier.clickable { filterState = 1 },
                    color = if (filterState == 1) Color.Black else Color.Gray)
                Text("(道具)", modifier = Modifier.clickable { filterState = 2 },
                    color = if (filterState == 2) Color.Black else Color.Gray)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                when {
                    userIdState == null -> {
                        // 還在用 email 取得 userId
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("正在取得使用者資訊…")
                        }
                    }
                    isLoading.value -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("正在載入背包資料...")
                        }
                    }
                    hasError.value -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("發生錯誤", color = Color.Red)
                            Text(errorMessage.value, color = Color.Red)
                            Button(onClick = {
                                coroutineScope.launch {
                                    val userId = userIdState ?: return@launch
                                    isLoading.value = true
                                    hasError.value = false
                                    try {
                                        val items = fetchUserItems(userId)
                                        allItems.clear()
                                        allItems.addAll(items)
                                        if (items.isEmpty()) errorMessage.value = "背包中沒有物品"
                                    } catch (e: Exception) {
                                        hasError.value = true
                                        errorMessage.value = "重試失敗: ${e.message}"
                                    } finally {
                                        isLoading.value = false
                                    }
                                }
                            }) { Text("重試") }
                        }
                    }
                    filteredItems.isEmpty() -> {
                        Text("背包中沒有物品", color = Color.Gray)
                    }
                    else -> {
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
                                items(filteredItems) { userItem ->
                                    Box(
                                        modifier = Modifier
                                            .size(80.dp)
                                            .background(Color.White)
                                            .clickable { selectedItem = userItem },
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.default_itempic),
                                            contentDescription = userItem.item.itemName,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text(
                                            "${userItem.count.value}",
                                            color = Color.Black,
                                            modifier = Modifier.padding(4.dp),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 合成彈窗
            if (showCraftDialog && resultItem != null) {
                AlertDialog(
                    onDismissRequest = { showCraftDialog = false },
                    title = {
                        Box(Modifier.fillMaxWidth()) {
                            Text("合成物", modifier = Modifier.align(Alignment.Center))
                            Text("✕", modifier = Modifier.align(Alignment.TopEnd).clickable {
                                showCraftDialog = false
                            }, fontSize = 24.sp)
                        }
                    },
                    text = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Image(
                                painter = painterResource(id = R.drawable.default_itempic),
                                contentDescription = resultItem.item.itemName,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                selectedItem?.let { material ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painter = painterResource(id = R.drawable.default_itempic),
                                            contentDescription = material.item.itemName,
                                            modifier = Modifier.size(50.dp)
                                        )
                                        Text("x1")
                                    }
                                }
                                // 顯示其它需要的素材（同 resultId 且不同於當前素材）
                                val others = allItems.filter {
                                    it.item.itemType == 0 &&
                                            it.item.resultId == resultItem.item.itemId &&
                                            it.item.itemId != selectedItem?.item?.itemId
                                }
                                others.forEach { material ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Image(
                                            painter = painterResource(id = R.drawable.default_itempic),
                                            contentDescription = material.item.itemName,
                                            modifier = Modifier.size(50.dp)
                                        )
                                        Text("x1")
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(onClick = {
                            coroutineScope.launch {
                                val uid = userIdState ?: run {
                                    snackbarHostState.showSnackbar("尚未取得使用者 ID，無法合成")
                                    return@launch
                                }
                                val requiredMaterials = allItems.filter {
                                    it.item.itemType == 0 && it.item.resultId == resultItem.item.itemId
                                }
                                val hasEnough = requiredMaterials.all { material ->
                                    allItems.any { ui -> ui.item.itemId == material.item.itemId && ui.count.value >= 1 }
                                }
                                if (hasEnough) {
                                    try {
                                        var latest: List<UserItem>? = null
                                        requiredMaterials.forEach { material ->
                                            latest = craftItem(uid, material.item.itemId)
                                        }
                                        latest?.let {
                                            allItems.clear()
                                            allItems.addAll(it)
                                        }
                                        snackbarHostState.showSnackbar("合成成功！")
                                        showCraftDialog = false
                                        selectedItem = null
                                    } catch (e: Exception) {
                                        snackbarHostState.showSnackbar("合成失敗: ${e.message}")
                                    }
                                } else {
                                    snackbarHostState.showSnackbar("材料不足，無法合成")
                                }
                            }
                        }) { Text("合成") }
                    },
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }

        // 物品詳細資訊對話框
        selectedItem?.let { userItem ->
            AlertDialog(
                onDismissRequest = { selectedItem = null },
                confirmButton = {},
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Text(" ")
                        Text("✕", modifier = Modifier.align(Alignment.TopEnd).clickable {
                            selectedItem = null
                        }, fontSize = 24.sp)
                    }
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.default_itempic),
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("稀有度：${userItem.item.itemRarity}")
                            Text("擁有 ${userItem.count.value} 件")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("物品介紹：", fontSize = 16.sp)
                            Text(text = userItem.item.itemEffect, modifier = Modifier.padding(top = 4.dp))
                        }
                        if (userItem.item.itemType == 0) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCraftDialog = true },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                            ) { Text("前往合成") }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}