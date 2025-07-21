package com.ntou01157.hunter.ui

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.ntou01157.hunter.models.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import androidx.navigation.NavHostController
import coil.compose.rememberAsyncImagePainter
import coil.compose.rememberImagePainter
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.ntou01157.hunter.DailyEvent
import com.ntou01157.hunter.DailyEventDialog
import com.ntou01157.hunter.dailyEvents
import com.ntou01157.hunter.models.Spot

@SuppressLint("UnrememberedMutableState")
@Composable
fun spotMarker(
    spot: Spot
) {
    var showDialog by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState(position = LatLng(spot.latitude, spot.longitude)) }

    //控制是否顯示事件視窗
    var showEventDialog by remember { mutableStateOf(false) }
    //被選中的事件
    var selectedEvent by remember { mutableStateOf<DailyEvent?>(null) }

    Marker(
        state = markerState,
        title = spot.spotName,
        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE),
        onClick = {
            showDialog = true
            true //回傳true，表示以處理點擊事件
        }
    )

    // 點擊地標後的對話框
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    showDialog = false
                    selectedEvent = dailyEvents.random() //隨機選取事件
                    showEventDialog = true
                }) {
                    Text("領取隨機事件")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("取消")
                }
            },
            title = { Text(spot.spotName) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //顯示圖片
                    Image(
                        painter = rememberAsyncImagePainter(spot.spotPhoto),
                        contentDescription = "地標圖片",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                    )
                }
            }
        )
    }
    //顯示隨機事件的對話框
    selectedEvent?.let { event ->
        if (showEventDialog) {
            DailyEventDialog(
                event = event,
                onOptionSelected = { option ->
                    //這邊可以處理玩家點擊某個事件選項後的邏輯，目前先以print表示
                    println("玩家選擇了：$option")
                },
                onDismiss = { showEventDialog = false }
            )
        }
    }

}
