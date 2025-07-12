package com.example.kotlin_test1

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun MissionsScreen(navController: NavHostController) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("任務版")
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = {
                navController.popBackStack() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFbc8f8f), // 按鈕背景顏色
                contentColor = Color.White         // 按鈕文字顏色
            )
        ) {
            Text("返回")
        }
    }
}
