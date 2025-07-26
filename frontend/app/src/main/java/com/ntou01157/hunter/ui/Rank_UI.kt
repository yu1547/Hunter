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
import androidx.compose.ui.platform.LocalContext // 新增 LocalContext 導入
import androidx.lifecycle.viewmodel.compose.viewModel // 導入 ViewModel 相關函式
import com.ntou01157.hunter.MainApplication // 導入您的 Application 類
import com.ntou01157.hunter.utils.NetworkResult // 導入 NetworkResult
import com.ntou01157.hunter.models.model_api.RankItem // 確保導入正確的 RankItem
import com.ntou01157.hunter.models.model_api.UserRank

fun convertRankItemToUserRanking(rankItem: RankItem): UserRanking {
    return UserRanking(
        rank = 0, // 這裡的 rank 會在 RankingScreen 中根據索引重新計算或由後端提供
        userId = rankItem.userId,
        username = rankItem.username,
        userImg = rankItem.userImg ?: "", // 處理可能的 null
        score = rankItem.score
    )
}

@Composable
fun RankingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as MainApplication
    val rankingViewModel: RankingViewModel = viewModel(
        factory = RankingViewModelFactory(application)
    )

    // 觀察 ViewModel 中的 rankData 狀態
    val rankDataState by rankingViewModel.rankData.collectAsState()

//    val rankList = rankResponse.rankList
//    val currentUser = rankResponse.userRank

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
        )
    {
        // 根據數據狀態顯示不同的 UI
        when (rankDataState) {
            is NetworkResult.Loading -> {
                // 數據載入中，顯示進度指示器或載入訊息
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
                        // 檢查 rankList 是否為空
                        if (rankList.isNotEmpty()) {
                            rankList.forEachIndexed { index, item ->
                                // RankingItem 期望 UserRanking 類型，但這裡的 rankList 包含 RankItem
                                // 需要將 RankItem 轉換為 UserRanking，或修改 RankingItem 參數類型
                                // 這裡我假設 RankingItem 也能直接處理 RankItem 或進行轉換
                                RankingItem(rank = index + 1, rankItem = item)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        } else {
                            Text(
                                text = "目前排行榜沒有數據。",
                                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 20.dp),
                                color = Color.Gray
                            )
                        }
                    }

                    // 顯示當前使用者排名 (如果存在)
                    currentUser?.let {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                            shape = RoundedCornerShape(0.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            // UserRanking 模型的 rank 屬性是 Int，這裡直接使用
//                            RankingItem(user = it, rankText = "${it.rank}")
                            RankingItem(userRank = it, rankText = "${it.rank}")
                        }
                    }
                } else {
                    // 數據為空，但狀態是成功（例如，後端返回空列表）
                    Text(
                        text = "未找到排行榜數據。",
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
            is NetworkResult.Error -> {
                val errorMessage = (rankDataState as NetworkResult.Error).message
                // 數據加載失敗，顯示錯誤訊息
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("載入排行榜失敗: $errorMessage", color = Color.Red, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))
                    // 可以添加一個重試按鈕
                    Button(onClick = { rankingViewModel.fetchRankData() }) {
                        Text("重試")
                    }
                }
            }
        }

        // 回首頁按鈕，位置保持不變
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
    }
//    {
//        Column(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(top = 64.dp, bottom = 96.dp, start = 16.dp, end = 16.dp)
//                .background(Color(0xFFDDDDDD), shape = RoundedCornerShape(12.dp))
//                .padding(12.dp)
//        ) {
//            rankList.forEach {
//                RankingItem(rank = it.rank, user = it)
//                Spacer(modifier = Modifier.height(8.dp))
//            }
//        }
//
//        IconButton(
//            onClick = { navController.navigate("main") },
//            modifier = Modifier.padding(top = 25.dp, bottom = 4.dp)
//        ) {
//            Image(
//                painter = painterResource(id = R.drawable.ic_home),
//                contentDescription = "回首頁",
//                modifier = Modifier.size(40.dp)
//            )
//        }
//
//        Card(
//            modifier = Modifier
//                .fillMaxWidth()
//                .align(Alignment.BottomCenter)
//                .padding(8.dp),
//            shape = RoundedCornerShape(0.dp),
//            colors = CardDefaults.cardColors(containerColor = Color.White)
//        ) {
//            RankingItem(user = currentUser, rankText = "${currentUser.rank}")
//        }
//    }
}

// 輔助函數：將 RankItem 轉換為 UserRanking
// 您原有的 UserRanking 定義在 models/rank.kt 中，而 RankItem 在 models/model_api 中。
// 這裡假設 RankItem 的結構可以與 UserRanking 兼容，或者您可以調整 UserRanking 的定義
// 或者修改 RankingItem Composable，讓它直接接收 RankItem。
// 為了不改動 RankingItem 的簽名，我們提供一個轉換函數


@Composable
fun RankingItem(
    rank: Int? = null, // 用於排行榜列表項目的動態排名
    rankItem: RankItem? = null, // 用於排行榜列表中的一般用戶
    userRank: UserRank? = null, // 用於當前用戶的排名 (userRank)
    rankText: String = "" // 如果 rankItem 或 userRank 中的 rank 需要特別顯示
) {    // 根據傳入的參數決定顯示哪種數據
    val userId = rankItem?.userId ?: userRank?.userId ?: ""
    val username = rankItem?.username ?: userRank?.username ?: "未知用戶"
    val userImg = rankItem?.userImg ?: userRank?.userImg
    val score = rankItem?.score ?: userRank?.score ?: 0

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (rank != null) {
            Text(
                text = "$rank",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.LightGray, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = username, fontSize = 18.sp)
            Text(text = "積分：$score", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (rankText.isNotEmpty()) {
            Text(text = rankText, fontSize = 18.sp)
        }
    }
}