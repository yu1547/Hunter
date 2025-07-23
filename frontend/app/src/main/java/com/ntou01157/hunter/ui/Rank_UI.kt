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
import com.ntou01157.hunter.mock.FakeUser
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.compose.rememberNavController

data class User(
    val rank: Int
)

@Composable
fun RankingScreen(userRankings: List<User>, currentUser: User, navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
    ) {
        // 排行榜主體
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 96.dp, start = 16.dp, end = 16.dp)
                .background(Color(0xFFDDDDDD), shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            userRankings.forEachIndexed { index, user ->
                RankingItem(rank = index + 1, user = user)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // 左上首頁按鈕
        IconButton(
            onClick = { navController.navigate("main") },
            modifier = Modifier.padding(top = 18.dp, bottom = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "回首頁",
                modifier = Modifier.size(40.dp)
            )
        }

        // 最底部顯示自己的排名卡
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            RankingItem(user = currentUser, rankText = "(${currentUser.rank})")
        }
    }
}

@Composable
fun RankingItem(rank: Int? = null, user: User, rankText: String = "") {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 顯示排名數字
        if (rank != null) {
            Text(
                text = "$rank",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(32.dp)
            )
        }

        // 圓形頭像 Placeholder
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color.LightGray, shape = CircleShape)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(text = FakeUser.displayName, fontSize = 18.sp)
            Text(text = "積分：${FakeUser.score.toInt()}", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 顯示自己的名次標籤
        if (rankText.isNotEmpty()) {
            Text(text = rankText, fontSize = 18.sp)
        }
    }
}

@Preview(showBackground = true) //預覽
@Composable
fun RankingScreenPreview() {
    val fakeUsers = List(10) { User(rank = it + 1) }
    val myUser = User(rank = 28)
    val navController = rememberNavController()

    RankingScreen(
        userRankings = fakeUsers,
        currentUser = myUser,
        navController = navController
    )
}
