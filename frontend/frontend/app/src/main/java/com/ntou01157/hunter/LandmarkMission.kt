package com.ntou01157.hunter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.*
import com.google.maps.android.compose.Marker
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import androidx.navigation.NavHostController
import com.google.maps.android.compose.MarkerState
import com.ntou01157.hunter.R

data class Landmark(
    val spotId: String,
    val spotName: String,
    val spotPhoto: Int,
    val position: LatLng
)

//定義固定的每日隨機事件
val dailyEvents = listOf(
    DailyEvent(
        title = "神秘商人的試煉",
        description = "一位背著大袋子的神秘商人出現在你面前。他看起來像是走遍世界的收藏家。「你願意用你手上的鑰匙碎片與我交易嗎？我有更值得的東西給你。」他咧嘴一笑。",
        options = listOf(
            "交出 銅鑰匙碎片 x3 → 獲得 銀鑰匙碎片 x1",
            "交出 銀鑰匙碎片 x3 → 獲得 金鑰匙碎片 x1",
            "什麼都不做"
        )
    ),
    DailyEvent(
        title = "石堆下的碎片",
        description = "你發現打卡點旁有一堆亂石，當你搬開其中一顆後，發現底下藏著一枚閃閃發亮的東西。",
        options = listOf("獲得 銅鑰匙碎片 x2")
    )
)

@Composable
fun LandmarkMarker(
    landmark: Landmark,
    navController: NavHostController
) {
    var showDialog by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState(position = landmark.position) }

    //控制是否顯示事件視窗
    var showEventDialog by remember { mutableStateOf(false) }
    //被選中的事件
    var selectedEvent by remember { mutableStateOf<DailyEvent?>(null) }

    Marker(
        state = markerState,
        title = landmark.spotName,
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
            title = { Text(landmark.spotName) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    //顯示圖片
                    Image(
                        painter = painterResource(id = landmark.spotPhoto),
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
