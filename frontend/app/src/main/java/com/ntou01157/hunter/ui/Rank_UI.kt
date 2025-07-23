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

@Composable
fun RankingScreen(rankResponse: RankResponse, navController: NavController) {
    val rankList = rankResponse.rankList
    val currentUser = rankResponse.userRank

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF2E3E3))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 64.dp, bottom = 96.dp, start = 16.dp, end = 16.dp)
                .background(Color(0xFFDDDDDD), shape = RoundedCornerShape(12.dp))
                .padding(12.dp)
        ) {
            rankList.forEach {
                RankingItem(rank = it.rank, user = it)
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

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

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(8.dp),
            shape = RoundedCornerShape(0.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            RankingItem(user = currentUser, rankText = "${currentUser.rank}")
        }
    }
}

@Composable
fun RankingItem(rank: Int? = null, user: UserRanking, rankText: String = "") {
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
            Text(text = user.username, fontSize = 18.sp)
            Text(text = "積分：${user.score}", fontSize = 14.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        if (rankText.isNotEmpty()) {
            Text(text = rankText, fontSize = 18.sp)
        }
    }
}

