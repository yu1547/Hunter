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
import androidx.compose.material3.Button
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
import com.ntou01157.hunter.LocationService
import com.ntou01157.hunter.R
import com.ntou01157.hunter.handlers.CheckInHandler
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.models.model_api.User as ApiUser
import com.ntou01157.hunter.models.model_api.UserTask
import com.ntou01157.hunter.utils.GeoVerifier
import com.ntou01157.hunter.utils.Vectorizer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// 輔助函式：將後端事件名稱映射到前端的導航路徑
private fun getRouteFromEventName(eventName: String): String? {
    return when (eventName) {
        "打扁史萊姆" -> "slimeAttack"
        "神秘商人的試煉", "神秘商人" -> "merchant" // 考慮到可能的名稱變化
        "石堆下的碎片" -> "stonePile"
        "偶遇銅寶箱", "偶遇銀寶箱", "偶遇金寶箱" -> "treasureBox"
        "古樹的祝福" -> "ancientTree"
        "在小小的 code 裡面抓阿抓阿抓" -> "bugHunt" // Wordle 遊戲
        else -> null // 如果沒有對應的遊戲畫面，返回 null
    }
}

@SuppressLint("UnrememberedMutableState")
@Composable
fun spotMarker(
        spot: Spot,
        userId: String,
        userTasks: List<UserTask>, // <-- 【修改點 C】 修改參數
        navController: NavHostController, // <-- 修正型別
        apiUser: ApiUser? // ✅ 2. 新增 apiUser 參數
) {
    // --- 狀態管理 ---
    var showDialog by remember { mutableStateOf(false) }
    val markerState = remember { MarkerState(position = LatLng(spot.latitude, spot.longitude)) }

    // --- 新增：正確篩選與此地標相關的進行中任務 ---
    val relevantMissions =
            remember(userTasks, spot.spotId) {
                userTasks.filter { userTask ->
                    val task = userTask.task
                    val isCorrectState = userTask.state == "in_progress"
                    val isNotLlm = !task.isLLM

                    isCorrectState && isNotLlm
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
                // 標題
                title = { Text(spot.ChName) },
                // 主要內容區
                text = {
                    Column(
                            modifier = Modifier.fillMaxWidth(),
                    ) {
                        // 顯示地標圖片 (這段邏輯不變)
                        val context = LocalContext.current
                        val resName =
                                remember(spot.spotName) {
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
                        val safeId = imageId.takeIf { it != 0 } ?: R.drawable.ic_spot_default
                        Image(
                                painter = painterResource(id = safeId),
                                contentDescription = "地標圖片",
                                modifier = Modifier.fillMaxWidth().height(150.dp)
                        )

                        // 如果有相關任務，就把它們作為按鈕列表顯示在圖片下方
                        if (relevantMissions.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("相關任務:")
                            Spacer(modifier = Modifier.height(8.dp))

                            // 用 Column 把任務按鈕垂直排列
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                relevantMissions.forEach { userTask ->
                                    Button(
                                            onClick = {
                                                showDialog = false
                                                val route =
                                                        getRouteFromEventName(
                                                                userTask.task.taskName
                                                        )
                                                if (route != null) {
                                                    // 如果是 bugHunt，它沒有 userId 參數，直接導航
                                                    if (route == "bugHunt") {
                                                        val taskId = userTask.task.taskId
                                                        navController.navigate(
                                                                "$route/$userId/$taskId"
                                                        )
                                                    } else if (route == "slimeAttack") {
                                                        // ✅ 3. 定義 now 來取得當前時間
                                                        val now = System.currentTimeMillis()
                                                        // ✅ 4. 使用傳入的 apiUser 來檢查 BUFF
                                                        val hasTorch =
                                                                apiUser?.buff?.any {
                                                                    it.name == "torch" &&
                                                                            (it.expiresAtMillisOrNull()
                                                                                    ?: 0) > now
                                                                }
                                                                        ?: false
                                                        // ✅ 5. 導航到正確的路徑
                                                        navController.navigate(
                                                                "$route/$userId/$hasTorch"
                                                        )
                                                    } else {
                                                        // 其他事件頁面需要 userId
                                                        navController.navigate("$route/$userId")
                                                    }
                                                } else {
                                                    Toast.makeText(
                                                                    ctx,
                                                                    "此任務沒有對應的活動頁面",
                                                                    Toast.LENGTH_SHORT
                                                            )
                                                            .show()
                                                    Log.w(
                                                            "SpotMarker",
                                                            "無法為任務 '${userTask.task.taskName}' 找到導航路徑"
                                                    )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth()
                                    ) {
                                        // 按鈕文字現在有足夠的空間顯示
                                        Text("執行：${userTask.task.taskName}")
                                    }
                                }
                            }
                        }
                    }
                },
                // 主要動作按鈕：打卡
                confirmButton = {
                    TextButton(
                            onClick = {
                                showDialog = false
                                scope.launch {
                                    val loc = locationService.getCurrentLocation()
                                    if (loc == null) {
                                        Toast.makeText(ctx, "無法取得定位", Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                    val currentLatLng = LatLng(loc.latitude, loc.longitude)
                                    val spotLatLng = LatLng(spot.latitude, spot.longitude)
                                    val inRange =
                                            GeoVerifier.distanceMeters(currentLatLng, spotLatLng) <=
                                                    GeoVerifier.DEFAULT_THRESHOLD_METERS

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
                },
                // 次要動作按鈕：取消
                dismissButton = { TextButton(onClick = { showDialog = false }) { Text("取消") } }
        )
    }
}
