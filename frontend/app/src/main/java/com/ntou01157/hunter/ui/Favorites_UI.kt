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
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ntou01157.hunter.R
import com.ntou01157.hunter.handlers.SpotLogHandler
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.models.User
import androidx.compose.ui.platform.LocalConfiguration

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

    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val screenHeight = LocalConfiguration.current.screenHeightDp.dp

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
                    .padding(horizontal = screenWidth * 0.02f) // 2% 螢幕寬度
            ) {
                // 回首頁按鈕
                IconButton(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier.padding(top = screenHeight * 0.04f, bottom = screenHeight * 0.005f)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_home),
                        contentDescription = "回首頁",
                        modifier = Modifier.size(screenWidth * 0.09f)
                    )
                }

                Spacer(modifier = Modifier.height(screenHeight * 0.05f))

                // 地標顯示區域
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.8f) // 整體放大
                            .fillMaxHeight(0.9f) // 整體放大
                            .align(Alignment.Center)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(screenWidth * 0.01f), // 間距再小一點
                            verticalArrangement = Arrangement.spacedBy(screenHeight * 0.15f) // 間距再小一點
                        ) {
                            for (row in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(screenWidth * 0.03f) // 間距再小一點
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
                                                        .fillMaxSize() // 填滿父層，polaroid緊貼邊框
                                                        .aspectRatio(527f / 866f),
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
                                                                .fillMaxWidth(0.7f)
                                                                .align(Alignment.TopCenter)
                                                                .offset(y = screenHeight * 0.07f) // 地標照片下移(數字越大)
                                                        )
                                                    } else {
                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth(0.7f) // 灰底
                                                                .fillMaxHeight(0.57f)
                                                                .align(Alignment.Center)
                                                                .background(Color(0xFFCCCCCC).copy(alpha = 0.7f))
                                                        )
                                                    }
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .align(Alignment.BottomCenter)
                                                            .padding(bottom = screenHeight * 0.018f), // 文字上移
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = landmark.ChName,
                                                            color = if (isUnlocked) Color.Black else Color.Gray,
                                                            fontSize = (screenWidth.value * 0.05f).sp, // 文字放大
                                                            maxLines = 1,
                                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                                            modifier = Modifier.padding(horizontal = screenWidth * 0.01f)
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
                }

                // 分頁按鈕
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = screenHeight * 0.02f),
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
                Dialog(onDismissRequest = onDismissSpotDialog) {
                    val dialogWidth = screenWidth * 0.85f
                    val dialogHeight = screenHeight * 0.55f
                    Box(
                        modifier = Modifier
                            .width(dialogWidth)
                            .height(dialogHeight)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.favorite_dialog),
                            contentDescription = "收藏彈窗背景",
                            modifier = Modifier.fillMaxSize()
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    start = dialogWidth * 0.18f, // 文字再往右
                                    top = dialogHeight * 0.18f,
                                    end = dialogWidth * 0.08f
                                ),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = it.spotName,
                                style = MaterialTheme.typography.titleLarge,
                                color = Color.Black,
                                fontSize = (screenWidth.value * 0.06f).sp // 標題依比例放大
                            )
                            Spacer(modifier = Modifier.height(dialogHeight * 0.03f))
                            Text(
                                text = "內容說明。",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Black,
                                fontSize = (screenWidth.value * 0.045f).sp // 內容依比例放大
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = dialogHeight * 0.001f) // 關閉按鈕再往下
                                .align(Alignment.BottomCenter),
                            contentAlignment = Alignment.Center
                        ) {
                            Button(
                                onClick = onDismissSpotDialog,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFB49865),
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .width(dialogWidth * 0.3f)
                                    .height(dialogHeight * 0.13f)
                            ) {
                                Text("關閉", fontSize = (screenWidth.value * 0.045f).sp)
                            }
                        }
                    }
                }
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
