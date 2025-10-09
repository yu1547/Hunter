package com.ntou01157.hunter.ui

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.ntou01157.hunter.DailyEvent
import com.ntou01157.hunter.LocationService
import com.ntou01157.hunter.R
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.handlers.CheckInHandler
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.utils.GeoVerifier
import com.ntou01157.hunter.utils.Vectorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 定義 Screen sealed class 以供 navController.navigate 使用
sealed class Screen(val route: String) {
    object TaskScreen : Screen("task_screen/{taskId}") {
        fun createRoute(taskId: String) = "task_screen/$taskId"
    }
    object BugHunt : Screen("bug_hunt")
    object TreasureBox : Screen("treasure_box")
    object Merchant : Screen("merchant")
    object AncientTree : Screen("ancient_tree")
    object SlimeAttack : Screen("slime_attack")
    object StonePile : Screen("stone_pile")
    object WordleGame : Screen("wordle_game")
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun spotMarker(
        spot: Spot,
        userId: String,
        navController: NavHostController, // <-- 修正型別
) {
    // --- 狀態管理 ---
    var showDialog by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState(position = LatLng(spot.latitude, spot.longitude)) }
    // 控制是否顯示事件視窗
    var showEventDialog by remember { mutableStateOf(false) }
    // 被選中的事件
    var selectedEvent by remember { mutableStateOf<DailyEvent?>(null) }

    // --- 資料載入與刷新邏輯 ---
    val apiService = RetrofitClient.apiService
    // *** 新增：在 spotMarker 內部管理任務狀態和載入邏輯 ***
    var missions by remember { mutableStateOf<List<Mission>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // 當 userId 變化時，觸發資料載入
    LaunchedEffect(key1 = userId) {
        if (userId.isNotBlank()) {
            isLoading = true
            errorMessage = null
            try {
                val response = withContext(Dispatchers.IO) { apiService.getUserTasks(userId) }
                if (response.isSuccessful) {
                    val tasks = response.body()?.tasks ?: emptyList()
                    missions =
                            tasks.map { task ->
                                Mission(
                                        taskId = task.taskId,
                                        // 修正：移除 taskName，因為 hunter/models/users.kt 中的 Mission
                                        // 模型沒有此欄位
                                        state = "in_progress",
                                        acceptedAt = null,
                                        expiresAt = null,
                                        refreshedAt = null,
                                        haveCheckPlaces = emptyList(), // 確保符合模型結構
                                        isLLM = task.isLLM
                                )
                            }
                } else {
                    errorMessage = "載入任務失敗: ${response.errorBody()?.string()}"
                }
            } catch (e: Exception) {
                errorMessage = "網路錯誤: ${e.message}"
            }
            isLoading = false
        }
    }

    // 根據 User 狀態計算符合條件的任務列表
    val relevantMissions =
            remember(missions) {
                missions.filter { mission ->
                    // 假設 isLLM 是 nullable boolean
                    mission.isLLM == false && mission.state == "in_progress"
                }
            }

    // === 新增：相機與權限 launcher（僅接上打卡流程） ===
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationService = remember { LocationService(ctx) } // ← 新增：用於按下時取定位

    val cameraLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
                if (bmp != null) {
                    // --------圖片轉向量
                    //            val vector = imageToVector(bmp)//測試用
                    scope.launch {
                        val vector =
                                withContext(Dispatchers.Default) {
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

    val permissionLauncher =
            rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted
                ->
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
                true // 回傳true，表示以處理點擊事件
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
                        TextButton(
                                onClick = {
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
                                        // 確認距離
                                        val distance =
                                                GeoVerifier.distanceMeters(
                                                        currentLatLng,
                                                        spotLatLng
                                                )
                                        Log.i(
                                                "GeoCheck",
                                                "距離=%.2f m, 閾值=%.1f m, user=(%.6f,%.6f), spot=(%.6f,%.6f)".format(
                                                        distance,
                                                        GeoVerifier.DEFAULT_THRESHOLD_METERS,
                                                        currentLatLng.latitude,
                                                        currentLatLng.longitude,
                                                        spotLatLng.latitude,
                                                        spotLatLng.longitude
                                                )
                                        )
                                        val inRange =
                                                distance <= GeoVerifier.DEFAULT_THRESHOLD_METERS

                                        if (!inRange) {
                                            Toast.makeText(
                                                            ctx,
                                                            "不在打卡範圍內（需 ≤ 30 公尺）",
                                                            Toast.LENGTH_SHORT
                                                    )
                                                    .show()
                                            return@launch
                                        }

                                        val hasCam =
                                                ContextCompat.checkSelfPermission(
                                                        ctx,
                                                        Manifest.permission.CAMERA
                                                ) == PackageManager.PERMISSION_GRANTED
                                        if (hasCam) {
                                            cameraLauncher.launch(null)
                                        } else {
                                            permissionLauncher.launch(Manifest.permission.CAMERA)
                                        }
                                    }
                                }
                        ) { Text("打卡") }
                        // 動態任務按鈕
                        relevantMissions.forEach { mission ->
                            TextButton(
                                    onClick = {
                                        showDialog = false
                                        navController.navigate(
                                                Screen.TaskScreen.createRoute(mission.taskId)
                                        )
                                    }
                            ) { Text("執行任務") } // 修正：因 Mission 模型無 taskName，使用通用文字
                        }
                    }
                },
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } },
                title = { Text(spot.ChName) },
                text = {
                    Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // 顯示圖片
                        val context = LocalContext.current
                        val resName =
                                remember(spot.spotName) {
                                    // 將「(地標)」「空白」「非 a-z0-9_」轉成底線
                                    spot.spotName
                                            .lowercase()
                                            .replace(Regex("[^a-z0-9_]+"), "_")
                                            .trim('_')
                                }
                        val imageId =
                                remember(resName) {
                                    context.resources.getIdentifier(
                                            resName,
                                            "drawable",
                                            context.packageName
                                    )
                                }
                        val safeId =
                                imageId.takeIf { it != 0 }
                                        ?: run {
                                            Log.e(
                                                    "SpotImage",
                                                    "缺少 drawable：原名='${spot.spotName}', 正規化='${resName}'"
                                            )
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
}
