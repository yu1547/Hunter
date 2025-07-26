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
import com.ntou01157.hunter.models.*

import androidx.compose.runtime.* // 導入必要的 compose runtime 函式
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext // 新增 LocalContext 導入
import androidx.lifecycle.viewmodel.compose.viewModel // 導入 ViewModel 相關函式
import com.ntou01157.hunter.MainApplication // 導入您的 Application 類
import com.ntou01157.hunter.utils.NetworkResult // 導入 NetworkResult
import com.ntou01157.hunter.models.model_api.RankItem // 確保導入正確的 RankItem
import com.ntou01157.hunter.models.model_api.UserRank

//fun convertRankItemToUserRanking(rankItem: RankItem): UserRanking {
//    return UserRanking(
//        rank = 0, // 這裡的 rank 會在 RankingScreen 中根據索引重新計算或由後端提供
//        userId = rankItem.userId,
//        username = rankItem.username,
//        userImg = rankItem.userImg ?: "", // 處理可能的 null
//        score = rankItem.score
//    )
//}


@Composable
fun RankingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as MainApplication
    val rankingViewModel: RankingViewModel = viewModel(
        factory = RankingViewModelFactory(application)
    )

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
                        // 排行榜列表標題 (可選)
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
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(horizontal = 8.dp, vertical = 4.dp), // 調整 padding
                            shape = RoundedCornerShape(0.dp), // 底部卡片通常不需要圓角
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F5)) // 更顯眼的顏色
                        ) {
                            // 使用另一個 Composable 或在 RankingListItem 中加一個標誌
                            // 這裡我們假設 RankingUserItem 專用於顯示當前用戶排名
                            RankingUserItem(userRank = userRank)
                        }
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
                    Button(onClick = { rankingViewModel.fetchRankData() }) {
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
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.Gray, shape = CircleShape) // 使用灰色圓圈作為預設頭像
        )
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
fun RankingUserItem(userRank: UserRank) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFE0FFFF), RoundedCornerShape(12.dp))
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
        Box(
            modifier = Modifier
                .size(56.dp) // 當前用戶頭像可以大一點
                .background(Color.Gray, shape = CircleShape) // 使用灰色圓圈作為預設頭像
        )
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