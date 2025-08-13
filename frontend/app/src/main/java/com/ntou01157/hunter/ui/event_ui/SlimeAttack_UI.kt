package com.ntou01157.hunter.ui.event_ui

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
import com.ntou01157.hunter.api.ApiService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.api.CompleteSlimeAttackRequest
import com.ntou01157.hunter.api.CompleteSlimeAttackResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.util.Log
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlimeAttackUI(onEventCompleted: (message: String) -> Unit) {
    val eventApiService = RetrofitClient.apiService
    val userId = "6880f31469ff254ed2fb0cc1"
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var damage by remember { mutableIntStateOf(0) }
    var totalDamage by remember { mutableIntStateOf(0) }
    var isAttacking by remember { mutableStateOf(false) }
    var isBattleOver by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (!isBattleOver) {
                Text(
                    text = "史萊姆襲擊",
                    style = MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Text(
                    text = "一隻史萊姆擋住了你的去路！點擊它來攻擊！",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .clickable(enabled = !isAttacking) {
                            coroutineScope.launch {
                                isAttacking = true
                                val newDamage = Random.nextInt(1, 4)
                                damage = newDamage
                                totalDamage += newDamage
                                delay(500)
                                isAttacking = false
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
                            style = MaterialTheme.typography.headlineMedium.copy(color = MaterialTheme.colorScheme.error),
                            modifier = Modifier.align(Alignment.BottomCenter)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "總傷害: $totalDamage",
                    style = MaterialTheme.typography.titleLarge
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = {
                        isBattleOver = true
                        coroutineScope.launch {
                            try {
                                val response: CompleteSlimeAttackResponse = eventApiService.completeSlimeAttack(
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
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "結束戰鬥")
                }
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
                ) {
                    Text(text = "離開")
                }
            }
        }
    }
}

// 已將這兩個 data class 從此檔案移除，並在頂部進行了 import
// data class CompleteSlimeAttackRequest(val userId: String, val totalDamage: Int)
// data class CompleteSlimeAttackResponse(val success: Boolean, val message: String, val rewards: List<String>)

@Preview(showBackground = true)
@Composable
fun PreviewSlimeAttackUI() {
    SlimeAttackUI(onEventCompleted = {})
}