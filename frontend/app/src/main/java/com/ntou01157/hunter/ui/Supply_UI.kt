package com.ntou01157.hunter.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntou01157.hunter.models.Supply
import kotlinx.coroutines.delay

// Marker Composable
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

// Dialog Composable
@Composable
fun SupplyDialog(
    supply: Supply,
    onDismiss: () -> Unit,
    onCollect: () -> Unit,
    isAvailable: Boolean,
    remainingTimeFormatted: () -> String
) {
    var cooldownText by remember { mutableStateOf(remainingTimeFormatted()) }

    LaunchedEffect(supply.supplyId) {
        while (!isAvailable) {
            cooldownText = remainingTimeFormatted()
            delay(1000)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        title = {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("補給站", modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "關閉")
                }
            }
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isAvailable) {
                    Text("點擊按鈕領取資源！", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(onClick = {
                        onCollect()
                        onDismiss()
                    }) {
                        Text("領取資源")
                    }
                } else {
                    Text("還需等待 $cooldownText 才能再次領取資源。", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    )
}