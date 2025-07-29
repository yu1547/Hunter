package com.ntou01157.hunter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.ntou01157.hunter.R

import androidx.compose.runtime.* // 導入必要的 compose runtime 函式
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext // 新增 LocalContext 導入
import androidx.lifecycle.viewmodel.compose.viewModel // 導入 ViewModel 相關函式
import coil.compose.AsyncImage
import com.ntou01157.hunter.MainApplication // 導入您的 Application 類
import com.ntou01157.hunter.utils.NetworkResult // 導入 NetworkResult
import com.ntou01157.hunter.models.model_api.RankItem // 確保導入正確的 RankItem
import com.ntou01157.hunter.models.model_api.UserRank




@Composable
fun RankingScreen(navController: NavController) {
    val userId = "user789"
    //初始化使用者的資料

    val application = LocalContext.current.applicationContext as MainApplication
    val rankingViewModel: RankingViewModel = viewModel(
        factory = RankingViewModelFactory(application)
    )

    LaunchedEffect(userId) {
        rankingViewModel.fetchRankData(userId)
    }

    val rankDataState by rankingViewModel.rankData.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
    ) {
        // 根據數據狀態顯示不同的 UI
        when (rankDataState) {
            is NetworkResult.Loading -> {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFbc8f8f))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("載入排行榜中...", fontSize = 18.sp, color = Color.Gray)
                }
            }
            is NetworkResult.Success -> {
                val rankResponse = (rankDataState as NetworkResult.Success).data
                if (rankResponse != null) {
                    val rankList = rankResponse.rankList
                    val currentUser = rankResponse.userRank

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 64.dp, bottom = 96.dp, start = 16.dp, end = 16.dp)
                            .background(Color(0xFFDDDDDD), shape = RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = "頂級獵人排行榜",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF8B4513),
                            modifier = Modifier.padding(bottom = 12.dp).align(Alignment.CenterHorizontally)
                        )

                        // 檢查 rankList 是否為空
                        if (rankList.isNotEmpty()) {
                            // 使用 LazyColumn 提高長列表性能
                            // 如果排行榜項目不多，直接使用 Column 也可以
                            rankList.forEachIndexed { index, item ->
                                // 直接傳遞 RankItem，並計算排名
                                RankingListItem(rank = index + 1, rankItem = item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            Text(
                                text = "目前排行榜沒有數據。",
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .padding(top = 20.dp),
                                color = Color.Gray
                            )
                        }
                    }

                    // 顯示當前使用者排名 (如果存在)
                    currentUser?.let { userRank ->
                        // 將 RankingUserItem 直接放在 Box 中並使用 alignment 定位
                        RankingUserItem(
                            userRank = userRank,
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter) // 讓它位於 Box 的底部中央
                                .padding(horizontal = 16.dp, vertical = 16.dp) // 調整 padding
                        )
                    }
                } else {
                    // 數據為空，但狀態是成功（例如，後端返回空列表），這是一種特殊情況
                    Text(
                        text = "未找到排行榜數據。",
                        modifier = Modifier.align(Alignment.Center),
                        color = Color.Gray
                    )
                }
            }
            is NetworkResult.Error -> {
                val errorMessage = (rankDataState as NetworkResult.Error).message
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("載入排行榜失敗: $errorMessage", color = Color.Red, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { rankingViewModel.fetchRankData(userId) }) {
                        Text("重試")
                    }
                }
            }
        }

        // 回首頁按鈕，位置保持不變
        IconButton(
            onClick = { navController.navigate("main") },
            modifier = Modifier
                .align(Alignment.TopStart) // 固定在左上角
                .padding(top = 25.dp, start = 16.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "回首頁",
                modifier = Modifier.size(40.dp)
            )
        }
    }
}

// 用於排行榜列表項目的 Composable (只有排名和 RankItem 數據)
@Composable
fun RankingListItem(rank: Int, rankItem: RankItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$rank",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(32.dp),
            color = Color(0xFF4B0082)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // *** START: 修改這裡的頭像顯示邏輯 ***
        // 暫時用一個純色圓圈作為佔位符
        if (!rankItem.userImg.isNullOrEmpty()) {
            AsyncImage(
                model = rankItem.userImg,
                contentDescription = "${rankItem.username}'s avatar",
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape) // Clip to circle shape
            )
        } else {
            // Fallback to a plain gray circle if no avatar URL is provided
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Gray, shape = CircleShape)
            )
        }
        // *** END: 修改這裡的頭像顯示邏輯 ***

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = rankItem.username, fontSize = 18.sp, fontWeight = FontWeight.Medium)
            Text(text = "積分：${rankItem.score}", fontSize = 14.sp, color = Color.Gray)
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun RankingUserItem(userRank: UserRank, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp), // 給底部卡片一個圓角
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)) // 更顯眼的顏色
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${userRank.rank}",
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFB22222),
                modifier = Modifier.width(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // *** START: 修改這裡的頭像顯示邏輯 ***
            // 暫時用一個純色圓圈作為佔位符
            if (!userRank.userImg.isNullOrEmpty()) {
                AsyncImage(
                    model = userRank.userImg,
                    contentDescription = "${userRank.username}'s avatar",
                    modifier = Modifier
                        .size(56.dp) // Current user's avatar can be slightly larger
                        .clip(CircleShape) // Clip to circle shape
                )
            } else {
                // Fallback to a plain gray circle if no avatar URL is provided
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color.Gray, shape = CircleShape)
                )
            }
            // *** END: 修改這裡的頭像顯示邏輯 ***

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(text = userRank.username, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Text(text = "我的積分：${userRank.score}", fontSize = 16.sp, color = Color.DarkGray)
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(text = " (你)", fontSize = 16.sp, color = Color.Black, fontWeight = FontWeight.SemiBold)
        }
    }
}