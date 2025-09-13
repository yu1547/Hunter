package com.ntou01157.hunter

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
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

            Image(
                painter = painterResource(id = R.drawable.login_subtitle),
                contentDescription = "遊戲名稱圖片",
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f) // 只佔 60% 高度，按鈕會顯示
                    .offset(y = (-110).dp) // 往上移動
            )

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