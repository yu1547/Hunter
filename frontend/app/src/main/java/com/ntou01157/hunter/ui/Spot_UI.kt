package com.ntou01157.hunter.ui
import com.ntou01157.hunter.R
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
import androidx.compose.ui.platform.LocalContext
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import com.ntou01157.hunter.handlers.CheckInHandler
import com.ntou01157.hunter.utils.GeoVerifier
import com.ntou01157.hunter.utils.Vectorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.ntou01157.hunter.LocationService   // ← 新增：按下時再取最新定位
import com.ntou01157.hunter.handlers.MissionHandler // 確保已正確導入

@SuppressLint("UnrememberedMutableState")
@Composable
fun spotMarker(
    spot: Spot,
    userId: String,
) {
    var showDialog by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState(position = LatLng(spot.latitude, spot.longitude)) }

    //控制是否顯示事件視窗
    var showEventDialog by remember { mutableStateOf(false) }
    //被選中的事件
    var selectedEvent by remember { mutableStateOf<DailyEvent?>(null) }

    // === 新增：相機與權限 launcher（僅接上打卡流程） ===
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(ctx) } // ← 新增：用於按下時取定位

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicturePreview()
    ) { bmp ->
        if (bmp != null) {
            // --------圖片轉向量
//            val vector = imageToVector(bmp)//測試用
            scope.launch {
                val vector = withContext(Dispatchers.Default) {
                    Vectorizer.imageToVector(ctx, bmp)
                }

                // （位置驗證改在按下按鈕時做，這裡不再檢查）

                val spotName = spot.spotName
                try {
                    val res = CheckInHandler.checkIn(userId, spotName, vector)
                    if (res.success) {
                        Toast.makeText(ctx, "打卡成功", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(ctx, "打卡失敗", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(ctx, "打卡錯誤：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
            // --------圖片轉向量
            // 這裡用傳入參數 userId；spotName 使用當前 spot.spotName
        } else {
            Toast.makeText(ctx, "未取得照片", Toast.LENGTH_SHORT).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            cameraLauncher.launch(null)
        } else {
            Toast.makeText(ctx, "需要相機權限", Toast.LENGTH_SHORT).show()
        }
    }
    // === 相機與權限 launcher 結束 ===

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
                androidx.compose.foundation.layout.Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {

                    TextButton(onClick = {
                        showDialog = false
                        // TODO: 打卡流程
                        // 例如：onCheckIn(spot) 或呼叫 ViewModel 發送請求
                        // → 已接上：檢查相機權限前，先以「當下定位」做 30m 距離驗證，通過才開相機
                        scope.launch {
                            val loc = locationService.getCurrentLocation()
                            if (loc == null) {
                                Toast.makeText(ctx, "無法取得定位", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val currentLatLng = LatLng(loc.latitude, loc.longitude)
                            val spotLatLng = LatLng(spot.latitude, spot.longitude)
                            //確認距離
                            val distance = GeoVerifier.distanceMeters(currentLatLng, spotLatLng)
                            Log.i(
                                "GeoCheck",
                                "距離=%.2f m, 閾值=%.1f m, user=(%.6f,%.6f), spot=(%.6f,%.6f)".format(
                                    distance,
                                    GeoVerifier.DEFAULT_THRESHOLD_METERS,
                                    currentLatLng.latitude, currentLatLng.longitude,
                                    spotLatLng.latitude, spotLatLng.longitude
                                )
                            )
                            val inRange = distance <= GeoVerifier.DEFAULT_THRESHOLD_METERS

                            if (!inRange) {
                                Toast.makeText(ctx, "不在打卡範圍內（需 ≤ 30 公尺）", Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            // --- 新增：檢查補給站任務邏輯 ---
                            try {
                                // 修正: 使用 MissionHandler 類別
                                val missionCheckRes = MissionHandler.checkSpotMission(userId, spot.spotId)
                                if (missionCheckRes != null) {      // 這行可能有點問題我只先改成可以跑的寫法
                                    Toast.makeText(ctx, "任務地點已標記完成！", Toast.LENGTH_LONG).show()
                                    val isMissionCompleted = missionCheckRes.isMissionCompleted ?: false
                                    if (isMissionCompleted) {
                                        Toast.makeText(ctx, "恭喜，您已完成一個任務！", Toast.LENGTH_LONG).show()
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e("MissionCheck", "檢查任務地點時發生錯誤: ${e.message}")
                                Toast.makeText(ctx, "任務檢查失敗: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                            // --- 新增邏輯結束 ---

                            val hasCam = ContextCompat.checkSelfPermission(
                                ctx,
                                Manifest.permission.CAMERA
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasCam) {
                                cameraLauncher.launch(null)
                            } else {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        }
                    }) { Text("打卡") }

                    TextButton(onClick = {
                        showDialog = false
                        selectedEvent = dailyEvents.random()
                        showEventDialog = true
                    }) { Text("領取隨機事件") }
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
                    val context = LocalContext.current
                    val resName = remember(spot.spotName) {
                        // 將「(地標)」「空白」「非 a-z0-9_」轉成底線
                        spot.spotName.lowercase().replace(Regex("[^a-z0-9_]+"), "_").trim('_')
                    }
                    val imageId = remember(resName) {
                        context.resources.getIdentifier(resName, "drawable", context.packageName)
                    }
                    val safeId = imageId.takeIf { it != 0 } ?: run {
                        Log.e("SpotImage", "缺少 drawable：原名='${spot.spotName}', 正規化='${resName}'")
                        R.drawable.ic_spot_default
                    }

                    Image(
                        painter = painterResource(id = safeId),
                        contentDescription = "地標圖片",
                        modifier = Modifier.fillMaxWidth().height(150.dp)
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
