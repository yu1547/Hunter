package com.ntou01157.hunter.ui.event_ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.CompleteSlimeAttackRequest
import com.ntou01157.hunter.api.CompleteSlimeAttackResponse
import com.ntou01157.hunter.api.RetrofitClient
import kotlin.random.Random
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlimeAttackUI(onEventCompleted: (message: String) -> Unit) {
    val eventApiService = RetrofitClient.apiService
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var damage by remember { mutableStateOf(0) }
    var isAttacking by remember { mutableStateOf(false) }

    var totalDamage by remember { mutableStateOf(0) }
    var gameEnded by remember { mutableStateOf(false) }
    var remainingTime by remember { mutableStateOf(15) }

    // 使用 LaunchedEffect 來啟動計時器
    LaunchedEffect(key1 = Unit) {
        while (remainingTime > 0) {
            delay(1000)
            remainingTime--
        }
        gameEnded = true
        // 時間到自動呼叫 API 進行結算
        coroutineScope.launch {
            try {
                val response: CompleteSlimeAttackResponse =
                        eventApiService.completeSlimeAttack(
                                CompleteSlimeAttackRequest(userId, totalDamage)
                        )
                if (response.success) {
                    snackbarHostState.showSnackbar(response.message)
                } else {
                    snackbarHostState.showSnackbar(response.message)
                }
            } catch (e: Exception) {
                Log.e("SlimeAttackUI", "完成任務失敗", e)
                snackbarHostState.showSnackbar("網路錯誤，無法結算。")
            }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
        ) {
            if (!gameEnded) {
                // 顯示剩餘時間
                Text(
                        text = "剩餘時間: $remainingTime 秒",
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                        text = "一隻史萊姆擋住了你的去路！點擊它來攻擊！",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                        modifier =
                                Modifier.size(200.dp).clickable(enabled = !isAttacking) {
                                    coroutineScope.launch {
                                        isAttacking = true
                                        val newDamage = Random.nextInt(1, 4)
                                        damage = newDamage
                                        totalDamage += newDamage
                                        delay(500)
                                        isAttacking = false
                                        damage = 0 // 傷害數值歸零，隱藏顯示
                                    }
                                },
                        contentAlignment = Alignment.Center
                ) {
                    Image(
                            painter = painterResource(id = R.drawable.slime),
                            contentDescription = "Slime",
                            modifier = Modifier.fillMaxSize()
                    )
                    if (damage > 0) {
                        Text(
                                text = "-$damage",
                                style =
                                        MaterialTheme.typography.headlineMedium.copy(
                                                color = MaterialTheme.colorScheme.error
                                        ),
                                modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(text = "總傷害: $totalDamage", style = MaterialTheme.typography.titleLarge)
            } else {
                Text(
                        text = "你已經成功擊退了史萊姆！",
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                        text = "總傷害: $totalDamage。後端已為你發放了獎勵。",
                        style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                        onClick = { onEventCompleted("史萊姆襲擊事件已結束") },
                        modifier = Modifier.fillMaxWidth()
                ) { Text(text = "離開") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PreviewSlimeAttackUI() {
    SlimeAttackUI(onEventCompleted = {})
}
