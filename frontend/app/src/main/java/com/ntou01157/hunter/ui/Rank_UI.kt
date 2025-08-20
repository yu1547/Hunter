package com.ntou01157.hunter.ui

import kotlinx.coroutines.delay
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.MainApplication
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.RankCreateRequest
import com.ntou01157.hunter.models.model_api.RankItem
import com.ntou01157.hunter.models.model_api.UserRank
import com.ntou01157.hunter.utils.NetworkResult

@Composable
fun RankingScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as MainApplication
    val vm: RankingViewModel = viewModel(factory = RankingViewModelFactory(application))

    var userId by remember { mutableStateOf<String?>(null) }
    var resolving by remember { mutableStateOf(true) }
    var resolveErr by remember { mutableStateOf<String?>(null) }

    // 1) 用 email 找使用者 → 確保有 rank → 再拉排行榜
    LaunchedEffect(Unit) {
        try {
            resolving = true
            val email = FirebaseAuth.getInstance().currentUser?.email
                ?: error("尚未登入，無法取得 Email")

            val user = RetrofitClient.apiService.getUserByEmail(email)
            userId = user.id

            // 確保 ranks 有這個人（沒有就建立 score=0）
            val created = ensureRankForUser(
                userId   = user.id,
                username = if (!user.username.isNullOrBlank()) user.username!! else "玩家",
                photoUrl = user.photoURL
            )

            // 若剛新建，給後端一點時間寫入（或你可改成後端直接回最新資料）
            if (created) delay(300)

            // 抓排行榜
            vm.fetchRankData(user.id)
        } catch (e: Exception) {
            resolveErr = e.message
            Log.e("Ranking", "resolve user failed", e)
        } finally {
            resolving = false
        }
    }

    val state by vm.rankData.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
    ) {
        when {
            resolving -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = Color(0xFFbc8f8f))
                    Spacer(Modifier.height(12.dp))
                    Text("載入中…", color = Color.Gray)
                }
            }
            resolveErr != null -> {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("取得使用者失敗：$resolveErr", color = Color.Red)
                }
            }
            else -> {
                when (state) {
                    is NetworkResult.Loading -> {
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator(color = Color(0xFFbc8f8f))
                            Spacer(Modifier.height(8.dp))
                            Text("載入排行榜中…", color = Color.Gray)
                        }
                    }
                    is NetworkResult.Error -> {
                        val msg = (state as NetworkResult.Error).message
                        Column(
                            Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("載入失敗：$msg", color = Color.Red)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { userId?.let { vm.fetchRankData(it) } }) {
                                Text("重試")
                            }
                        }
                    }
                    is NetworkResult.Success -> {
                        val data = (state as NetworkResult.Success<com.ntou01157.hunter.models.model_api.RankResponse?>).data
                        if (data == null) {
                            Text("未找到排行榜數據。", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                        } else {
                            RankingBoard(
                                rankList = data.rankList,
                                me = data.userRank,
                                onBack = { navController.navigate("main") }
                            )
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = { navController.navigate("main") },
            modifier = Modifier
                .align(Alignment.TopStart)
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

// ---------- UI：排行榜清單 + 我的排名 ----------
@Composable
private fun RankingBoard(
    rankList: List<RankItem>,
    me: UserRank?,
    onBack: () -> Unit
) {
    // 雙保險：過濾掉 score=0 的人（即使後端沒更新也不會出現在清單）
    val listToShow = remember(rankList) { rankList.filter { it.score > 0 } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
    ) {
        // 1) 上半部排行榜（LazyColumn），內容下緣留空，避免被底部卡片遮住
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp, bottom = 16.dp)
                .background(Color(0xFFDDDDDD), RoundedCornerShape(12.dp)),
            contentPadding = PaddingValues(
                top = 16.dp,
                start = 12.dp,
                end = 12.dp,
                bottom = 124.dp   // 預留底部卡片高度 + 內距，避免擋住最後一列
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Text(
                    text = "頂級獵人排行榜",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8B4513),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    textAlign = TextAlign.Center
                )
            }

            if (listToShow.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("目前沒有數據", color = Color.Gray)
                    }
                }
            } else {
                itemsIndexed(listToShow) { index, item ->
                    RankingItemRow(rank = index + 1, item = item)
                }
            }
        }

        // 2) 底部固定「我的排名」卡
        me?.let { mine ->
            val rankLabel = if (mine.rank == null || mine.score == 0) "(未上榜)" else "${mine.rank}"

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Avatar(url = mine.userImg, size = 56.dp)
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(mine.username, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("積分：${mine.score}", fontSize = 16.sp, color = Color.DarkGray)
                    }
                    Text(text = "$rankLabel", fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

// ---------- UI：排行榜單列 ----------
@Composable
private fun RankingItemRow(rank: Int, item: RankItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF222222)
            )
        }

        Spacer(Modifier.width(8.dp))

        // 右側白色卡片：頭像 + 名稱 + 積分
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(url = item.userImg, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(item.username, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                    Text("積分：${item.score}", fontSize = 14.sp, color = Color.Gray)
                }
            }
        }
    }
}

// ---------- 共用：頭像（photoURL 空字串/Null → 顯示灰色圓框；有圖裁切填滿） ----------
@Composable
private fun Avatar(url: String?, size: Dp) {
    val hasImage = !url.isNullOrBlank()
    if (hasImage) {
        AsyncImage(
            model = url,
            contentDescription = "avatar",
            contentScale = ContentScale.Crop,   // 讓圖片符合圓框大小
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(Color(0xFFD9D9D9))
        )
    }
}

private suspend fun ensureRankForUser(
    userId: String,
    username: String,
    photoUrl: String?
): Boolean {
    return try {
        val exists = try {
            val r = RetrofitClient.apiService.getRankByUserId(userId)
            r.userRank?.userId == userId || r.rankList.any { it.userId == userId }
        } catch (_: Exception) {
            false
        }

        if (!exists) {
            RetrofitClient.apiService.createRank(
                RankCreateRequest(
                    userId = userId,
                    username = username,
                    userImg  = photoUrl ?: "",
                    score    = 0
                )
            )
            true  // 剛建立
        } else {
            false // 已存在
        }
    } catch (e: Exception) {
        Log.e("Ranking", "ensureRankForUser failed: ${e.message}", e)
        try {
            RetrofitClient.apiService.createRank(
                RankCreateRequest(
                    userId = userId,
                    username = username,
                    userImg  = photoUrl ?: "",
                    score    = 0
                )
            )
            true
        } catch (e2: Exception) {
            Log.e("Ranking", "ensureRank create failed: ${e2.message}", e2)
            false
        }
    }
}
