package com.ntou01157.hunter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ntou01157.hunter.R
import com.ntou01157.hunter.handlers.SpotLogHandler
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.models.User

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    userId: String,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    onSpotClicked: (Spot) -> Unit,
    selectedSpot: Spot?,
    onDismissSpotDialog: () -> Unit,
    showLockedDialog: Boolean,
    onDismissLockedDialog: () -> Unit
) {
    // 地標解鎖紀錄
    var scanLog by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    // 載入地標解鎖紀錄
    LaunchedEffect(Unit) {
        val logs = SpotLogHandler.getSpotLogs(userId)   // 原本的 user.uid -> userId
        scanLog = logs
    }

    // 分頁地標資料
    var pages by remember { mutableStateOf<List<List<Spot>>>(emptyList()) }

    // 載入地標分頁資料
    LaunchedEffect(Unit) {
        pages = SpotLogHandler.getSpotPages()
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFB49865),
        contentColor = Color.White
    )

    // 取得當前頁地標
    val items = pages.getOrNull(pageIndex).orEmpty()

    // 外層 Box，負責 loading 狀態顯示
    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        if (items.isEmpty()) {
            // 資料還沒載入
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text("正在載入地標資料...", color = Color.Gray)
            }
        } else {
            // 原本的畫面（把你的收藏冊 UI 包進這裡）
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF2EFDE))
                    .padding(horizontal = 16.dp)
            ) {
                // 回首頁按鈕
                IconButton(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.padding(top = 30.dp, bottom = 4.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "回首頁",
                        modifier = Modifier.size(45.dp)
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))

                // 地標顯示區域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // 地標區域填滿剩餘空間
                    contentAlignment = Alignment.Center // 讓地標區域內容在整個 Box 置中
                ) {
                    Column(
                        modifier = Modifier
                            .width(900.dp) // 調整地標區域寬度
                            .fillMaxHeight(1.0f) // 填滿父容器高度
                            .padding(5.dp), // 外圍留白
                        verticalArrangement = Arrangement.spacedBy(70.dp) // 每一列之間的間距
                    ) {
                        for (row in 0..1) {
                            Row(
                                modifier = Modifier.fillMaxWidth(), // 每列填滿寬度
                                horizontalArrangement = Arrangement.spacedBy(10.dp) // 每個地標之間的間距
                            ) {
                                for (col in 0..1) {
                                    val index = row * 2 + col
                                    if (index < items.size) {
                                        val landmark = items[index]
                                        val isUnlocked = scanLog[landmark.spotName.lowercase()] == true

                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .aspectRatio(1.0f)
                                                .clickable { onSpotClicked(landmark) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            // polaroid和地標照片包在一起
                                            Box(
                                                modifier = Modifier
                                                    .width(130.dp)
                                                    .aspectRatio(527f / 866f), // polaroid比例
                                                contentAlignment = Alignment.TopCenter
                                            ) {
                                                Image(
                                                    painter = painterResource(id = R.drawable.polaroid),
                                                    contentDescription = "polaroid",
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                                if (isUnlocked) {
                                                    Image(
                                                        painter = painterResource(id = getSpotImageResId(landmark.spotName)),
                                                        contentDescription = landmark.spotName,
                                                        modifier = Modifier
                                                            .size(120.dp)
                                                            .align(Alignment.TopCenter)
                                                            .offset(y = 45.dp) //數值越小，下移越少
                                                    )
                                                }else{
                                                    // 灰底，置中顯示
                                                    Box(
                                                        modifier = Modifier
                                                            .size(90.dp, 118.dp)
                                                            .align(Alignment.Center)
                                                            .background(Color(0xFFCCCCCC).copy(alpha = 0.7f))
                                                    )
                                                }
                                                // 文字覆蓋在 polaroid 下方白色區域
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .align(Alignment.BottomCenter)
                                                        .padding(bottom = 15.dp), // 微調文字在 polaroid上的位置
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = landmark.ChName,
                                                        color = if (isUnlocked) Color.Black else Color.Gray,
                                                        fontSize = 16.sp,
                                                        maxLines = 1,
                                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                        modifier = Modifier.padding(horizontal = 8.dp)
                                                    )
                                                }
                                            }
                                        }
                                    } else {
                                        Spacer(modifier = Modifier.weight(1f)) // 空格填滿
                                    }
                                }
                            }
                        }
                    }
                }

                // 分頁按鈕
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onPageChange(pageIndex - 1) },
                        colors = buttonColors,
                        enabled = pageIndex > 0
                    ) {
                        Text("<")
                    }
                    Button(
                        onClick = { onPageChange(pageIndex + 1) },
                        colors = buttonColors,
                        enabled = pageIndex < pages.size - 1
                    ) {
                        Text(">")
                    }
                }
            }

            // 地標解鎖彈窗
            selectedSpot?.let {
                AlertDialog(
                    onDismissRequest = onDismissSpotDialog,
                    confirmButton = {
                        TextButton(onClick = onDismissSpotDialog) {
                            Text("關閉")
                        }
                    },
                    title = { Text(it.spotName) },
                    text = { Text("內容說明。") }
                )
            }

            // 地標鎖定提示彈窗
            if (showLockedDialog) {
                AlertDialog(
                    onDismissRequest = onDismissLockedDialog,
                    confirmButton = {
                        TextButton(onClick = onDismissLockedDialog) {
                            Text("確定")
                        }
                    },
                    text = { Text("此地標尚未解鎖，請先前往現場打卡！") }
                )
            }
        }
    }
}


@Composable
fun getSpotImageResId(spotName: String): Int {
    return when (spotName.lowercase()) {
        "moai" -> R.drawable.moai
        "vending" -> R.drawable.vending
        "anchor" -> R.drawable.anchor
        "ball" -> R.drawable.ball
        "eagle" -> R.drawable.eagle
        "lovechair" -> R.drawable.lovechair
        "book" -> R.drawable.book
        "bookcase" -> R.drawable.bookcase
        "freedomship" -> R.drawable.freedomship
        "fountain" -> R.drawable.fountain
        else -> R.drawable.placeholder // 建議準備一張 default 圖片
    }
}
