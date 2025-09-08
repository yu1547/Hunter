package com.ntou01157.hunter.ui

import android.annotation.SuppressLint
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntou01157.hunter.api.SupplyApi
import com.ntou01157.hunter.handlers.MissionHandler // 確保已正確導入
import com.ntou01157.hunter.models.Supply
import com.ntou01157.hunter.models.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 補給站地圖上的圖標
@SuppressLint("UnrememberedMutableState")
@Composable
fun SupplyMarker(supply: Supply, onClick: (Supply) -> Unit) {
    Marker(
            state = MarkerState(position = LatLng(supply.latitude, supply.longitude)),
            // 顯示文字："-補給站-補給站的name"
            title = "-補給站-${supply.name}",
            snippet = supply.supplyId,
            onClick = {
                onClick(supply)
                true
            }
    )
}

// 對話框
@Composable
fun SupplyDialog(
        supply: Supply,
        onDismiss: () -> Unit,
        onCollect: () -> Unit,
        onDailyEvent: () -> Unit, // 新增：每日事件按鈕的點擊事件
        isCooldown: Boolean,
        cooldownText: String,
        hasDailyEvent: Boolean, // 新增：是否顯示每日事件按鈕的狀態
) {
    AlertDialog(
            onDismissRequest = onDismiss,
            confirmButton = {},
            title = {
                Box(modifier = Modifier.fillMaxWidth()) {
                    Text("-補給站-${supply.name}", modifier = Modifier.align(Alignment.Center))
                    IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                        Icon(Icons.Default.Close, contentDescription = "關閉")
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(onClick = onCollect) { Text("領取資源") }
                    if (isCooldown) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("冷卻中，剩餘 $cooldownText")
                    }
                    // 如果有每日事件，顯示這個按鈕
                    if (hasDailyEvent) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onDailyEvent) { Text("執行任務") }
                    }
                }
            }
    )
}

// 補給站冷卻時間更新
private fun formatMs(ms: Long): String {
    val sec = (ms / 1000).coerceAtLeast(0)
    val m = sec / 60
    val s = sec % 60
    return "%02d:%02d".format(m, s)
}

@Composable
fun SupplyHandlerDialog(supply: Supply, user: User, onDismiss: () -> Unit) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 冷卻目標時間（UTC millis），僅在後端回傳 COOLDOWN 後啟動
    var cooldownUntil by remember { mutableStateOf<Long?>(null) }
    var cooldownText by remember { mutableStateOf("") }
    val isCooldown = cooldownUntil?.let { System.currentTimeMillis() < it } ?: false

    // 新增狀態來追蹤每日事件
    var dailyEventName by remember { mutableStateOf<String?>(null) }
    val hasDailyEvent = dailyEventName != null

    // 每次對話框開啟時，檢查是否有每日事件
    LaunchedEffect(supply) {
        val res = withContext(Dispatchers.IO) { SupplyApi.getDailyEventForSpot(supply.supplyId) }
        if (res.success && res.hasEvent) {
            dailyEventName = res.eventName
        } else {
            dailyEventName = null
        }
    }

    // 每秒更新倒數
    LaunchedEffect(cooldownUntil) {
        while (cooldownUntil != null && System.currentTimeMillis() < cooldownUntil!!) {
            val left = cooldownUntil!! - System.currentTimeMillis()
            cooldownText = formatMs(left)
            delay(1000)
        }
        if (cooldownUntil != null && System.currentTimeMillis() >= cooldownUntil!!) {
            cooldownUntil = null
            cooldownText = ""
        }
    }

    SupplyDialog(
            supply = supply,
            isCooldown = isCooldown,
            cooldownText = cooldownText,
            onDismiss = onDismiss,
            onCollect = {
                scope.launch {
                    val res =
                            withContext(Dispatchers.IO) {
                                SupplyApi.claim(user.uid, supply.supplyId)
                            }
                    if (res.success) {
                        Toast.makeText(context, "領取成功", Toast.LENGTH_SHORT).show()
                        val until = SupplyApi.parseUtcMillis(res.nextClaimTime)
                        if (until != null) {
                            cooldownUntil = until
                            val left = (until - System.currentTimeMillis()).coerceAtLeast(0)
                            cooldownText = formatMs(left)
                        }
                        onDismiss()
                    } else if (res.reason == "COOLDOWN" && res.nextClaimTime != null) {
                        val until = SupplyApi.parseUtcMillis(res.nextClaimTime)
                        if (until != null) {
                            cooldownUntil = until
                            val left = (until - System.currentTimeMillis()).coerceAtLeast(0)
                            cooldownText = formatMs(left)
                        } else {
                            Toast.makeText(context, "冷卻中", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "領取失敗：${res.reason ?: "未知錯誤"}", Toast.LENGTH_SHORT)
                                .show()
                    }
                }
            },
            hasDailyEvent = hasDailyEvent, // 傳遞每日事件狀態
            onDailyEvent = {
                scope.launch {
                    try {
                        val missionCheckRes =
                                withContext(Dispatchers.IO) {
                                    MissionHandler.checkSpotMission(user.uid, supply.supplyId)
                                }

                        if (missionCheckRes.eventName == "打扁史萊姆") {
                            navController.navigate("slimeAttack")
                        } else if (missionCheckRes.eventName == "神秘商人") {
                            navController.navigate("merchant")
                        }
                        if (missionCheckRes.isMissionCompleted) {
                            Toast.makeText(context, "恭喜，您已完成一個任務！", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(context, "任務地點已標記完成！", Toast.LENGTH_LONG).show()
                        }
                        onDismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "任務執行失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
    )
}
