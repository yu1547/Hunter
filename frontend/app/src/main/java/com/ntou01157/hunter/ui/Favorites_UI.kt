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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
    var scanLog by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }

    LaunchedEffect(Unit) {
        val logs = SpotLogHandler.getSpotLogs(userId)   // 原本的 user.uid -> userId
        scanLog = logs
    }


    var pages by remember { mutableStateOf<List<List<Spot>>>(emptyList()) }

    LaunchedEffect(Unit) {
        pages = SpotLogHandler.getSpotPages()
    }

    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )

    val items = pages.getOrNull(pageIndex).orEmpty()

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
                    .background(Color(0xFFF3DCDC))
                    .padding(horizontal = 16.dp)
            ) {
                // ←←← 後面照你原本的 IconButton、Box、Row 等
            }
        }
    }



    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3DCDC))
            .padding(horizontal = 16.dp)
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

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(370.dp)
                    .fillMaxHeight(0.8f)
                    .background(Color(0xFFDADADA))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            if (index < items.size) {
                                val landmark = items[index]
                                val isUnlocked = scanLog[landmark.spotName.lowercase()] == true


                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(if (isUnlocked) Color.White else Color.LightGray)
                                        .clickable { onSpotClicked(landmark) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        if (isUnlocked) {
                                            Image(
                                                painter = painterResource(id = getSpotImageResId(landmark.spotName)),
                                                contentDescription = landmark.spotName,
                                                modifier = Modifier.size(48.dp)
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        Text(
                                            text = landmark.ChName,
                                            color = if (isUnlocked) Color.Black else Color.Gray
                                        )

                                    }

                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

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

    // 解鎖彈窗
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

    // 鎖定提示彈窗
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
