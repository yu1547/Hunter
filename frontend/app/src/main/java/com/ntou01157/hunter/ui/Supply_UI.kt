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
import com.ntou01157.hunter.isSupplyAvailable
import com.ntou01157.hunter.formattedRemainingCooldown
import com.google.firebase.Timestamp
import com.ntou01157.hunter.handlers.DropHandler
import kotlinx.coroutines.delay

// 補給站地圖上的圖標
@SuppressLint("UnrememberedMutableState")
@Composable
fun SupplyMarker(
    supply: Supply,
    onClick: (Supply) -> Unit
) {
    Marker(
        state = MarkerState(position = LatLng(supply.latitude, supply.longitude)),
        title = supply.name,
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
    isAvailable: Boolean,
    remainingTimeFormatted: () -> String
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("-補給站-", modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "關閉")
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isAvailable) {
                    Text("點擊按鈕領取資源！")
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = onCollect) {
                        Text("領取資源")
                    }
                } else {
                    Text("還需等待 ${remainingTimeFormatted()} 才能再次領取資源。")
                }
            }
        }
    )
}

// 補給站冷卻時間更新
fun Timestamp.plusSeconds(seconds: Long): Timestamp {
    return Timestamp(this.seconds + seconds, this.nanoseconds)
}

fun collectSupply(user: User, supplyId: String) {
    val cooldownSeconds = 15 * 60L
    val nextClaim = Timestamp.now().plusSeconds(cooldownSeconds)
    user.supplyScanLogs[supplyId] = nextClaim
}

@Composable
fun SupplyHandlerDialog(
    supply: Supply,
    user: User,
    onDismiss: () -> Unit
) {
    val nextClaimTime = user.supplyScanLogs[supply.supplyId]
    var isAvailable by remember { mutableStateOf(isSupplyAvailable(nextClaimTime)) }
    var cooldownText by remember { mutableStateOf(formattedRemainingCooldown(nextClaimTime)) }

    LaunchedEffect(nextClaimTime) {
        while (!isAvailable) {
            cooldownText = formattedRemainingCooldown(user.supplyScanLogs[supply.supplyId])
            isAvailable = isSupplyAvailable(user.supplyScanLogs[supply.supplyId])
            delay(1000)
        }
    }
    val context = LocalContext.current
    SupplyDialog(
        supply = supply,
        isAvailable = isAvailable,
        remainingTimeFormatted = { cooldownText },
        onDismiss = onDismiss,
        onCollect = {
            collectSupply(user, supply.supplyId)
            DropHandler.collectDrop(context = context, user = user, difficulty = 1)
            onDismiss()
        }


    )
}