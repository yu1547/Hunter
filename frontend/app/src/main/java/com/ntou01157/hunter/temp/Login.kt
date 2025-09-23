package com.ntou01157.hunter

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ntou01157.hunter.R
import com.ntou01157.hunter.temp.LoginViewModel

@Composable
fun LoginScreen(navController: NavHostController, loginViewModel: LoginViewModel = viewModel()) {
    val context = LocalContext.current
    val activity = context as Activity
    var showError by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val data: Intent? = result.data
        loginViewModel.handleSignInResult(
            activity = activity,
            data = data,
            onSuccess = { email ->
                navController.navigate("main") {
                    popUpTo("login") { inclusive = true }
                }
            },
            onFailure = { message ->
                showError = message
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Image(
            painter = painterResource(id = R.drawable.login_background),
            contentDescription = "登入背景",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(0.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .offset(y = (-20).dp),
                contentAlignment = Alignment.Center
            ) {
                // 第一顆bling，在 subtitle 下方
                val infiniteTransition1 = rememberInfiniteTransition()
                val alpha1 by infiniteTransition1.animateFloat(
                    initialValue = 0.2f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(550, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Image(
                    painter = painterResource(id = R.drawable.login_blingname),
                    contentDescription = "閃閃效果1",//右上
                    modifier = Modifier
                        .size(140.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 55.dp, y = 8.dp)//x數字越小越往左,y數字越大越往下
                        .graphicsLayer { this.alpha = alpha1 }
                )
                // 第四顆 bling
                val infiniteTransition4 = rememberInfiniteTransition()
                val alpha4 by infiniteTransition4.animateFloat(
                    initialValue = 0.5f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(400, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Image(
                    painter = painterResource(id = R.drawable.login_blingname),
                    contentDescription = "閃閃效果4",//左下
                    modifier = Modifier
                        .size(70.dp)
                        .align(Alignment.BottomStart)
                        .offset(x = (15).dp, y = (-23).dp)//x數字越小越往左,y數字越大越往下
                        .graphicsLayer { this.alpha = alpha4 }
                )


                // 畫 subtitle，會在 bling 上方
                Image(
                    painter = painterResource(id = R.drawable.login_subtitle),
                    contentDescription = "遊戲名稱圖片",
                    modifier = Modifier
                        .fillMaxHeight()
                )

                // 第三顆 bling
                val infiniteTransition3 = rememberInfiniteTransition()
                val alpha3 by infiniteTransition3.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(600, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Image(
                    painter = painterResource(id = R.drawable.login_blingname),
                    contentDescription = "閃閃效果3",//右下
                    modifier = Modifier
                        .size(100.dp)
                        .align(Alignment.BottomEnd)
                        .offset(x = 22.dp, y = (-25).dp)
                        .graphicsLayer { this.alpha = alpha3 }
                )
                // 第二顆 bling
                val infiniteTransition2 = rememberInfiniteTransition()
                val alpha2 by infiniteTransition2.animateFloat(
                    initialValue = 0.4f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(750, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    )
                )
                Image(
                    painter = painterResource(id = R.drawable.login_blingname),
                    contentDescription = "閃閃效果2",//左上
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopStart)
                        .offset(x = (-55).dp, y = 10.dp)
                        .graphicsLayer { this.alpha = alpha2 }
                )

            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate("main") },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.width(200.dp).padding(bottom = 16.dp)
            ) {
                Text("登入", color = Color.Black)
            }

            Button(
                onClick = { /* 註冊邏輯 */ },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                modifier = Modifier.width(200.dp).padding(bottom = 24.dp)
            ) {
                Text("註冊", color = Color.Black)
            }

            Text(
                text = "---------- 或以下登入 ----------",
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.ic_google_logo),
                contentDescription = "Google 登入圖示",
                modifier = Modifier
                    .size(64.dp)
                    .clickable {
                        val signInClient = loginViewModel.getSignInClient(activity)
                        val signInIntent = signInClient.signInIntent

                        // 🔁 先 signOut 以清除登入快取
                        signInClient.signOut().addOnCompleteListener {
                            launcher.launch(signInIntent)
                        }
                    }
            )



            showError?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = it, color = Color.Red)
            }
        }
    }
}