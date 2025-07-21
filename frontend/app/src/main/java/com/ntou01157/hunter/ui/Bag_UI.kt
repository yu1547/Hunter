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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ntou01157.hunter.model.model_api.UserItem
import com.ntou01157.hunter.Backpack.data.fetchUserItems
import com.ntou01157.hunter.Backpack.data.craftItem
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import android.util.Log

class BagActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "bag") {
                composable("bag") { BagScreen(navController, "6879fdbc125a5443a1d4bade") }
            }
        }
    }
}

@Composable
fun BagScreen(navController: NavHostController, userId: String) {
    // 初始化物品列表(從API取得)
    val coroutineScope = rememberCoroutineScope()
    val allItems = remember { mutableStateListOf<UserItem>() }
    val isLoading = remember { mutableStateOf(true) }
    val hasError = remember { mutableStateOf(false) }
    val errorMessage = remember { mutableStateOf("") }
    
    // 加載數據
    LaunchedEffect(key1 = userId) {
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
    
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<UserItem?>(null) }
    var filterState by remember { mutableStateOf(0) }
    var showCraftDialog by remember { mutableStateOf(false) }

    val filteredItems = when (filterState) {
        1 -> allItems.filter { it.item.itemType == 0 && it.count.value > 0 }
        2 -> allItems.filter { it.item.itemType == 1 && it.count.value > 0 }
        else -> allItems.filter { it.count.value > 0 }
    }

    // 如果選中的是素材(itemType為0)，找出可合成的結果物品
    val resultItem = remember(selectedItem) {
        if (selectedItem?.item?.itemType == 0 && selectedItem?.item?.resultId != null) {
            allItems.find { it.item.itemId == selectedItem?.item?.resultId }
        } else {
            null
        }
    }

    val context = LocalContext.current
    fun getDrawableId(name: String): Int {
        val resourceId = context.resources.getIdentifier(name, "drawable", context.packageName)
        return if (resourceId == 0) R.drawable.ic_placeholder else resourceId
    }

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
                if (isLoading.value) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("正在載入背包資料...")
                    }
                } else if (hasError.value) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("發生錯誤", color = Color.Red)
                        Text(errorMessage.value, color = Color.Red)
                        Button(onClick = {
                            coroutineScope.launch {
                                isLoading.value = true
                                hasError.value = false
                                try {
                                    val items = fetchUserItems(userId)
                                    allItems.clear()
                                    allItems.addAll(items)
                                    if (items.isEmpty()) {
                                        errorMessage.value = "背包中沒有物品"
                                    }
                                } catch (e: Exception) {
                                    hasError.value = true
                                    errorMessage.value = "重試失敗: ${e.message}"
                                } finally {
                                    isLoading.value = false
                                }
                            }
                        }) {
                            Text("重試")
                        }
                    }
                } else if (filteredItems.isEmpty()) {
                    Text("背包中沒有物品", color = Color.Gray)
                } else {
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
                                    // val imageResId = getDrawableId(userItem.item.itemPic)
                                    // if (imageResId == R.drawable.ic_placeholder) {
                                    //     Log.e("BagScreen", "Invalid imageResId for item: ${userItem.item.itemName} (pic: ${userItem.item.itemPic})")
                                    // }
                                    Image(
                                        painter = painterResource(id = R.drawable.default_itempic), // 之後要記得改成imageResId，而且要把上面註解取消
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
                        // 顯示合成後物品
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // val resultImageResId = getDrawableId(resultItem.item.itemPic)
                            // if (resultImageResId == R.drawable.ic_placeholder) {
                            //     Log.e("BagScreen", "Invalid imageResId for resultItem: ${resultItem.item.itemName} (pic: ${resultItem.item.itemPic})")
                            // }
                            Image(
                                painter = painterResource(id = R.drawable.default_itempic),   // 之後要記得改成resultImageResId，而且要把上面註解取消
                                contentDescription = resultItem.item.itemName,
                                modifier = Modifier.size(100.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 顯示需要的材料
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // 顯示當前選中的素材
                                selectedItem?.let { material ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // val materialImageResId = getDrawableId(material.item.itemPic)
                                        // if (materialImageResId == R.drawable.ic_placeholder) {
                                        //     Log.e("BagScreen", "Invalid imageResId for selected material: ${material.item.itemName} (pic: ${material.item.itemPic})")
                                        // }
                                        Image(
                                            painter = painterResource(id = R.drawable.default_itempic), // 之後要記得改成materialImageResId，而且要把上面註解取消
                                            contentDescription = material.item.itemName,
                                            modifier = Modifier.size(50.dp)
                                        )
                                        Text("x1")
                                    }
                                }
                                
                                // 顯示其他需要的素材
                                allItems.filter { 
                                    it.item.itemType == 0 && 
                                    it.item.resultId == resultItem.item.itemId &&
                                    it.item.itemId != selectedItem?.item?.itemId
                                }.forEach { material ->
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        // val materialImageResId = getDrawableId(material.item.itemPic)
                                        // if (materialImageResId == R.drawable.ic_placeholder) {
                                        //     Log.e("BagScreen", "Invalid imageResId for other material: ${material.item.itemName} (pic: ${material.item.itemPic})")
                                        // }
                                        Image(
                                            painter = painterResource(id = R.drawable.default_itempic), // 之後要記得改成materialImageResId，而且要把上面註解取消
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
                                val requiredMaterials = allItems.filter {
                                    it.item.itemType == 0 && it.item.resultId == resultItem.item.itemId
                                }

                                val hasEnoughMaterials = requiredMaterials.all { material ->
                                    allItems.any { userItem ->
                                        userItem.item.itemId == material.item.itemId && userItem.count.value >= 1
                                    }
                                }

                                if (hasEnoughMaterials) {
                                    try {
                                        var latestItems: List<UserItem>? = null
                                        requiredMaterials.forEach { material ->
                                            latestItems = craftItem(userId, material.item.itemId)
                                        }
                                        latestItems?.let {
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
                        }) {
                            Text("合成")
                        }
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
                        // val imageResId = getDrawableId(userItem.item.itemPic)
                        // if (imageResId == R.drawable.ic_placeholder) {
                        //     Log.e("BagScreen", "Invalid imageResId for detail view item: ${userItem.item.itemName} (pic: ${userItem.item.itemPic})")
                        // }
                        Image(
                            painter = painterResource(id = R.drawable.default_itempic), // 之後要記得改成imageResId，而且要把上面註解取消
                            contentDescription = null,
                            modifier = Modifier.size(200.dp).padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("稀有度：${userItem.item.itemRarity}")
                            Text("擁有 ${userItem.count.value} 件")
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("物品介紹：", fontSize = 16.sp)
                            Text(
                                text = userItem.item.itemEffect,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        if (userItem.item.itemType == 0) {
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(
                                onClick = { showCraftDialog = true },
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