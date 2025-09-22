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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
import com.ntou01157.hunter.api.ItemApi

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
    // var userIdState by remember { mutableStateOf<String?>("68846d797609912e5e6ba9af") }//測試用
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

    // 可立即使用的道具 ID（排除鑰匙/碎片）
    val USABLE_ITEM_IDS = setOf(
        "6880f3f7d80b975b33f23e36", // 小史萊姆 spawn_slime_small
        "6880f3f7d80b975b33f23e37", // 大史萊姆 spawn_slime_big
        "6880f3f7d80b975b33f23e38", // 寶藏圖 treasure_map_trigger
        "6880f3f7d80b975b33f23e39", // 時間沙漏-加速 hourglass_speed_refresh
        "6880f3f7d80b975b33f23e3a", // 時間沙漏-減速 hourglass_slow_extend
        "6880f3f7d80b975b33f23e3b", // 火把 torch_buff
        "6880f3f7d80b975b33f23e3c"  // 古樹的枝幹 ancient_branch_buff
    )


    // 3) 有了 userId 才去抓背包
    LaunchedEffect(userIdState) {
        val userId = userIdState ?: return@LaunchedEffect
        // val userId = "68846d797609912e5e6ba9af" // 測試用，之後刪掉
        isLoading.value = true
        hasError.value = false
        try {
            Log.d("BagScreen", "開始獲取用戶物品，用戶ID: ${userId}")
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

    val filteredItems =
        when (filterState) {
            1 -> allItems.filter { it.item.itemType == 0 && it.count.value > 0 }
            2 -> allItems.filter { it.item.itemType == 1 && it.count.value > 0 }
            else -> allItems.filter { it.count.value > 0 }
        }

    // 合成結果 ID（不依賴背包是否已有該結果物）
    val resultItemId = selectedItem?.item?.resultId?.takeIf { selectedItem?.item?.itemType == 0 }
    val resultItempic = allItems.find { it.item.itemId == resultItemId }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3DCDC))
                .padding(horizontal = 16.dp)
                .padding(paddingValues)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 25.dp, start = 16.dp)
                    .clickable { navController.navigate("main") }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_icon),
                    contentDescription = "回首頁",
                    modifier = Modifier.size(60.dp)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier =
                    Modifier.fillMaxWidth()
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
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp) // 保留邊距
                        ) {
                            items(filteredItems) { userItem ->
                                Box(
                                    modifier = Modifier
                                        .size(100.dp) // 外框大小可以稍微調整
                                        .clickable { selectedItem = userItem },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.square_button),
                                        contentDescription = "Item Slot",
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // 物品圖片
                                    val context = LocalContext.current
                                    val resId = remember(userItem.item.itemPic) {
                                        context.resources.getIdentifier(userItem.item.itemPic, "drawable", context.packageName)
                                    }

                                    val painter = if (resId != 0) {
                                        painterResource(id = resId)
                                    } else {
                                        painterResource(id = R.drawable.default_itempic)
                                    }

                                    Image(
                                        painter = painter,
                                        contentDescription = userItem.item.itemPic,
                                        modifier = Modifier.size(64.dp) // 放在框內縮小一點
                                    )

                                    // 數量文字
                                    Text(
                                        "${userItem.count.value}",
                                        color = Color.Black,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 物品詳細資訊對話框
        selectedItem?.let { userItem ->
            AlertDialog(
                onDismissRequest = { selectedItem = null },
                confirmButton = {
                    val canUse = userItem.item.itemId in USABLE_ITEM_IDS &&
                            userItem.count.value > 0 &&
                            userIdState != null
                    if (canUse) {
                        Button(onClick = {

                        }) { Text("使用") }
                    }
                },
                title = {
                    Box(Modifier.fillMaxWidth()) {
                        Text(" ")
                        Text("✕", modifier = Modifier.align(Alignment.TopEnd).clickable {
                            selectedItem = null
                        }, fontSize = 24.sp)
                    }
                },
                text = {
                    val context = LocalContext.current

                    Dialog(
                        onDismissRequest = { selectedItem = null },
                        properties = DialogProperties(usePlatformDefaultWidth = false) // 禁用系統預設寬度
                    ) {
                        Box(
                            modifier = Modifier
                                .width(500.dp)
                                .wrapContentHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            // 背景框
                            Image(
                                painter = painterResource(id = R.drawable.dialog_1), // 你的對話框背景圖片
                                contentDescription = "物品框",
                                modifier = Modifier.fillMaxSize()
                            )

                            // 內容
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp, vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {

                                // 關閉按鈕
                                Box(
                                    modifier = Modifier
                                        .width(280.dp) // 限制最大寬度
                                        .wrapContentSize(Alignment.TopEnd) // 在範圍內靠右上
                                ) {
                                    Text(
                                        "✕",
                                        fontSize = 25.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier
                                            .clickable { selectedItem = null }
                                            .padding(4.dp)
                                    )
                                }

                                // 名字
                                Text(
                                    text = userItem.item.itemName,
                                    fontSize = 20.sp,
                                    color = Color.Black,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(Modifier.height(12.dp))

                                // 物品圖片
                                val context = LocalContext.current
                                val resId = context.resources.getIdentifier(
                                    userItem.item.itemPic, "drawable", context.packageName
                                )
                                val painter = if (resId != 0) painterResource(id = resId)
                                else painterResource(id = R.drawable.default_itempic)

                                Image(
                                    painter = painter,
                                    contentDescription = userItem.item.itemPic,
                                    modifier = Modifier.size(64.dp)
                                )

                                Spacer(Modifier.height(12.dp))

                                // 稀有度 + 擁有數
                                Row(
                                    modifier = Modifier.width(220.dp),   // 控制寬度避免超出
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("稀有度：${userItem.item.itemRarity}", textAlign = TextAlign.Start)
                                    Text("擁有 ${userItem.count.value} 件", textAlign = TextAlign.End)
                                }

                                Spacer(Modifier.height(16.dp))

                                // 介紹
                                Column(modifier = Modifier.width(220.dp)) {
                                    Text("物品介紹：", fontSize = 16.sp)
                                    Text(
                                        text = userItem.item.itemEffect,
                                        modifier = Modifier.padding(top = 4.dp),
                                        textAlign = TextAlign.Start
                                    )
                                }

                                Spacer(Modifier.height(24.dp))

                                val canUse = userItem.item.itemId in USABLE_ITEM_IDS &&
                                        userItem.count.value > 0 &&
                                        userIdState != null

                                if (canUse) {
                                    // 使用按鈕
                                    Box(
                                        modifier = Modifier
                                            .width(250.dp)
                                            .height(100.dp)
                                            .clickable {
                                                coroutineScope.launch {
                                                    val uid = userIdState!!
                                                    // 產生本次動作的 requestId（若要重試要重用同一個）
                                                    val reqId = ItemApi.generateRequestId(uid, userItem.item.itemId)
                                                    val resp = ItemApi.useItem(uid, userItem.item.itemId, reqId)

                                                    when {
                                                        resp.success -> {
                                                            // 成功 → 重新抓背包
                                                            val refreshed = fetchUserItems(uid)
                                                            allItems.clear(); allItems.addAll(refreshed)
                                                            selectedItem = null

                                                            // NEW: 依 effects 顯示提示
                                                            val msgs = buildString {
                                                                resp.effects?.let { arr ->
                                                                    for (i in 0 until arr.length()) {
                                                                        val e = arr.getJSONObject(i)

                                                                        // 碎片
                                                                        if (e.has("fragments")) {
                                                                            val f = e.getJSONObject("fragments")
                                                                            val it = f.keys()
                                                                            while (it.hasNext()) {
                                                                                val k = it.next()
                                                                                val name = when (k) {
                                                                                    "copperKeyShard" -> "銅鑰匙碎片"
                                                                                    "silverKeyShard" -> "銀鑰匙碎片"
                                                                                    "goldKeyShard"   -> "金鑰匙碎片"
                                                                                    else -> k
                                                                                }
                                                                                append("獲得 $name x${f.optInt(k)}\n")
                                                                            }
                                                                        }

                                                                        // Buff
                                                                        if (e.has("buffAdded")) {
                                                                            val b = e.getJSONObject("buffAdded")
                                                                            val buffName = when (b.optString("name")) {
                                                                                "torch" -> "火把"
                                                                                "ancient_branch" -> "古樹的枝幹"
                                                                                "treasure_map_once" -> "寶藏圖"
                                                                                else -> b.optString("name")
                                                                            }
                                                                            append("獲得 $buffName Buff\n")
                                                                        }

                                                                        // 任務類
                                                                        if (e.optBoolean("missionsRefreshed", false)) append("已立即刷新任務\n")
                                                                        if (e.has("missionsExtendedMin")) append("限時任務延長 ${e.optInt("missionsExtendedMin")} 分鐘\n")
                                                                    }
                                                                }
                                                            }.trim()

                                                            if (msgs.isNotEmpty()) {
                                                                snackbarHostState.showSnackbar(msgs)
                                                            } else {
                                                                snackbarHostState.showSnackbar("已使用：${userItem.item.itemName}")
                                                            }
                                                        }
                                                        resp.duplicate -> {
                                                            // 重複請求：視為已處理成功（通常第一次已成功）
                                                            val refreshed = fetchUserItems(uid)
                                                            allItems.clear(); allItems.addAll(refreshed)
                                                            selectedItem = null
                                                            snackbarHostState.showSnackbar("已處理（先前的請求已完成）")
                                                        }

                                                        else -> {
                                                            snackbarHostState.showSnackbar("使用失敗，請稍後重試")
                                                        }
                                                    }
                                                }
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(250.dp)
                                                .height(100.dp)
                                                .clickable { coroutineScope.launch {
                                                    val uid = userIdState!!
                                                    // 產生本次動作的 requestId（若要重試要重用同一個）
                                                    val reqId = ItemApi.generateRequestId(uid, userItem.item.itemId)
                                                    val resp = ItemApi.useItem(uid, userItem.item.itemId, reqId)

                                                    when {
                                                        resp.success -> {
                                                            // 成功 → 重新抓背包
                                                            val refreshed = fetchUserItems(uid)
                                                            allItems.clear(); allItems.addAll(refreshed)
                                                            selectedItem = null

                                                            // NEW: 依 effects 顯示提示
                                                            val msgs = buildString {
                                                                resp.effects?.let { arr ->
                                                                    for (i in 0 until arr.length()) {
                                                                        val e = arr.getJSONObject(i)

                                                                        // 碎片
                                                                        if (e.has("fragments")) {
                                                                            val f = e.getJSONObject("fragments")
                                                                            val it = f.keys()
                                                                            while (it.hasNext()) {
                                                                                val k = it.next()
                                                                                val name = when (k) {
                                                                                    "copperKeyShard" -> "銅鑰匙碎片"
                                                                                    "silverKeyShard" -> "銀鑰匙碎片"
                                                                                    "goldKeyShard"   -> "金鑰匙碎片"
                                                                                    else -> k
                                                                                }
                                                                                append("獲得 $name x${f.optInt(k)}\n")
                                                                            }
                                                                        }

                                                                        // Buff
                                                                        if (e.has("buffAdded")) {
                                                                            val b = e.getJSONObject("buffAdded")
                                                                            val buffName = when (b.optString("name")) {
                                                                                "torch" -> "火把"
                                                                                "ancient_branch" -> "古樹的枝幹"
                                                                                "treasure_map_once" -> "寶藏圖"
                                                                                else -> b.optString("name")
                                                                            }
                                                                            append("獲得 $buffName Buff\n")
                                                                        }

                                                                        // 任務類
                                                                        if (e.optBoolean("missionsRefreshed", false)) append("已立即刷新任務\n")
                                                                        if (e.has("missionsExtendedMin")) append("限時任務延長 ${e.optInt("missionsExtendedMin")} 分鐘\n")
                                                                    }
                                                                }
                                                            }.trim()

                                                            if (msgs.isNotEmpty()) {
                                                                snackbarHostState.showSnackbar(msgs)
                                                            } else {
                                                                snackbarHostState.showSnackbar("已使用：${userItem.item.itemName}")
                                                            }
                                                        }
                                                        resp.duplicate -> {
                                                            // 重複請求：視為已處理成功（通常第一次已成功）
                                                            val refreshed = fetchUserItems(uid)
                                                            allItems.clear(); allItems.addAll(refreshed)
                                                            selectedItem = null
                                                            snackbarHostState.showSnackbar("已處理（先前的請求已完成）")
                                                        }

                                                        else -> {
                                                            snackbarHostState.showSnackbar("使用失敗，請稍後重試")
                                                        }
                                                    }
                                                }},
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                painter = painterResource(id = R.drawable.button),
                                                contentDescription = "使用",
                                                modifier = Modifier.fillMaxSize()  // 填滿 Box
                                            )
                                            Text("使用", color = Color.Black, fontSize = 23.sp)
                                        }
                                    }
                                } else if (userItem.item.itemType == 0) {
                                    // 合成按鈕
                                    Box(
                                        modifier = Modifier
                                            .width(250.dp)
                                            .height(100.dp)
                                            .clickable { showCraftDialog = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            painter = painterResource(id = R.drawable.button),
                                            contentDescription = "合成按鈕",
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        Text("前往合成", color = Color.Black, fontSize = 23.sp)
                                    }
                                }

                            }
                        }
                    }
                },
                shape = RoundedCornerShape(16.dp)
            )
        }

        // 合成彈窗（放在最後，確保顯示在最上層）
        if (showCraftDialog && resultItemId != null) {
            Dialog(
                onDismissRequest = { selectedItem = null },
                properties = DialogProperties(usePlatformDefaultWidth = false) // 禁用系統預設寬度
            ) {
                Box(
                    modifier = Modifier
                        .width(500.dp)
                        .wrapContentHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    // 背景框
                    Image(
                        painter = painterResource(id = R.drawable.dialog_1), // 你的對話框背景圖片
                        contentDescription = "物品框",
                        modifier = Modifier.fillMaxSize()
                    )

                    Column(
                        modifier = Modifier
                            .padding(24.dp)
                            .width(260.dp),  // 固定內容寬度，避免字亂跑
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 關閉按鈕
                        Text(
                            "✕",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .align(Alignment.End)
                                .clickable { showCraftDialog = false }
                        )

                        // 物品名稱
                        Text(
                            text = resultItempic?.item?.itemName ?: "合成物",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // 合成結果圖片
                        val context = LocalContext.current
                        val resId = context.resources.getIdentifier(
                            resultItempic?.item?.itemPic ?: "",
                            "drawable",
                            context.packageName
                        )
                        val painter = if (resId != 0) painterResource(id = resId)
                        else painterResource(id = R.drawable.default_itempic)

                        Image(
                            painter = painter,
                            contentDescription = resultItempic?.item?.itemName,
                            modifier = Modifier.size(100.dp)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 中間提示
                        Text(
                            "需消耗",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.DarkGray,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )

                        // 材料區
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            selectedItem?.let { material ->
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val matResId = context.resources.getIdentifier(
                                        material.item.itemPic, "drawable", context.packageName
                                    )
                                    val matPainter = if (matResId != 0) painterResource(id = matResId)
                                    else painterResource(id = R.drawable.default_itempic)

                                    Image(
                                        painter = matPainter,
                                        contentDescription = material.item.itemName,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Text("x3")
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Box(
                            modifier = Modifier
                                .clickable {
                                    coroutineScope.launch {
                                        val uid = userIdState ?: run {
                                            snackbarHostState.showSnackbar("尚未取得使用者 ID，無法合成")
                                            return@launch
                                        }
                                        val requiredMaterials = allItems.filter {
                                            it.item.itemType == 0 && it.item.resultId == resultItemId
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
                                            finally {
                                                showCraftDialog = false
                                                selectedItem = null
                                            }
                                        } else {
                                            snackbarHostState.showSnackbar("材料不足，無法合成")
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.button),
                                contentDescription = "合成按鈕",
                                modifier = Modifier
                                    .width(200.dp)
                                    .height(70.dp)
                            )
                            Text("合成", color = Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

