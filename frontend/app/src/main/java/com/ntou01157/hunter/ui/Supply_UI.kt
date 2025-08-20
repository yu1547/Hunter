package com.ntou01157.hunter.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntou01157.hunter.models.Supply
import com.ntou01157.hunter.models.User
import kotlinx.coroutines.delay
import android.widget.Toast
import com.ntou01157.hunter.api.SupplyApi
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 補給站地圖上的圖標
@SuppressLint("UnrememberedMutableState")
@Composable
fun SupplyMarker(
    supply: Supply,
    onClick: (Supply) -> Unit
) {
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
    isCooldown: Boolean,
    cooldownText: String
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
fun SupplyHandlerDialog(
    supply: Supply,
    user: User,
    onDismiss: () -> Unit
) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    // 冷卻目標時間（UTC millis），僅在後端回傳 COOLDOWN 後啟動
    var cooldownUntil by remember { mutableStateOf<Long?>(null) }
    var cooldownText by remember { mutableStateOf("") }
    val isCooldown = cooldownUntil?.let { System.currentTimeMillis() < it } ?: false

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
            // 呼叫後端 /api/supplies/{userId}/{supplyId}/claim（移出主執行緒）
            scope.launch {
                val res = withContext(Dispatchers.IO) {
                    SupplyApi.claim(user.uid, supply.supplyId)
                }
                if (res.success) {
                    Toast.makeText(context, "領取成功", Toast.LENGTH_SHORT).show()
                    // 成功後後端也會回傳下一次可領取時間，啟動冷卻顯示
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
                    Toast.makeText(context, "領取失敗：${res.reason ?: "未知錯誤"}", Toast.LENGTH_SHORT).show()
                }
            }
        }


    )
}