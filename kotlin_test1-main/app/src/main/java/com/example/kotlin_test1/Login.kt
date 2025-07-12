package com.example.kotlin_test1

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.example.kotlin_test1.R

@Composable
fun LoginScreen(navController: NavHostController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3DCDC))
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(60.dp))

        // Logo 或遊戲名稱圖片
        Box(
            modifier = Modifier
                .size(width = 320.dp, height = 160.dp)
                .background(Color(0xFFDADADA)), // 模擬圖片背景
            contentAlignment = Alignment.Center
        ) {
            Text(text = "（遊戲名稱）", color = Color.Black, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.weight(1f))

        // 登入按鈕
        Button(
            onClick = { navController.navigate("main") }, // 點擊跳轉背包頁面
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 16.dp)
        ) {
            Text("登入", color = Color.Black)
        }

        // 註冊按鈕
        Button(
            onClick = { /* 加入註冊邏輯 */ },
            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
            modifier = Modifier
                .width(200.dp)
                .padding(bottom = 24.dp)
        ) {
            Text("註冊", color = Color.Black)
        }

        // 分隔線與文字
        Text(
            text = "---------- 或以下登入 ----------",
            color = Color.Black,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Google 登入圖示
        Image(
            painter = painterResource(id = R.drawable.ic_google_logo),
            contentDescription = "Google 登入圖示",
            modifier = Modifier.size(64.dp)
        )
    }
}
