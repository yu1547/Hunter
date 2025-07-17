package com.ntou01157.hunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import androidx.compose.ui.platform.LocalContext
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.ntou01157.hunter.mock.FakeUser
import kotlinx.coroutines.delay
import com.ntou01157.hunter.ui.BagScreen
import com.ntou01157.hunter.ui.TaskListScreen
import com.ntou01157.hunter.models.*


class Main : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("main") {
                    MainScreen(navController)
                }
                composable("bag") {
                    BagScreen(navController = navController, user = FakeUser)
                }
//                composable("favorites") {
//                    FavoritesScreen(navController)
//                }
                composable("ranking") {
                    RankingScreen(navController)
                }
                composable("tasklist") {
                    TaskListScreen(navController)
                }

            }
        }
    }
}

@OptIn(com.google.accompanist.permissions.ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: androidx.navigation.NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )
    //打卡點
    val missionLandmark = Landmark(
        spotId = "mission1",
        spotName = "(地標名)",
        spotPhoto = R.drawable.item1,
        position = LatLng(25.149853, 121.778352)
    )
    //補給站
    val supplyStations = remember {
        listOf(
            SupplyStation("station1", LatLng(25.149034, 121.779087)),
            SupplyStation("station2", LatLng(25.149836, 121.779452))
        )
    }

    //紀錄當前點擊的補給站
    var selectedStation by remember { mutableStateOf<SupplyStation?>(null) }
    var showSupplyDialog by remember { mutableStateOf(false) }

    //初始地圖位置

    val context = LocalContext.current
    //檢查權限
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    //用來抓目前gps位置
    val locationService = remember { LocationService(context) }

    //先亂給一個預設中心點座標(測試)
    val defaultLatLng = LatLng(25.149995, 121.778730)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 16f)
    }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
            //以取得權限 -> 嘗試抓目前位置
            val location = locationService.getCurrentLocation()
            location?.let {
                val latLng = LatLng(it.latitude, it.longitude)
                userLocation = latLng
                cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(myLocationButtonEnabled = true),
            properties = MapProperties(isMyLocationEnabled = true),
            onMapClick = { selectedStation = null }//點擊地圖時關閉彈窗
        ){//顯示玩家位置
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "所在位置",
                    //snippet = "Player"
                )
            }
            //顯示補給站
            supplyStations.forEach { station ->
                Marker(
                    state = MarkerState(position = station.position),
                    title = "補給站",
                    snippet = station.spotId,
                    onClick = {
                        selectedStation = station
                        showSupplyDialog = true
                        true
                    }
                )
            }
            //顯示打卡點
            LandmarkMarker(landmark = missionLandmark, navController = navController)

        }
        //補給站領取資源視窗
        if (showSupplyDialog && selectedStation != null) {
            val station = selectedStation!!
            var cooldownText by remember {mutableStateOf(station.formattedRemainingCooldown()) }

            //每秒更新一次倒數字串
            LaunchedEffect(station.spotId, showSupplyDialog) {
                while (showSupplyDialog && !station.isAvailable()) {
                    cooldownText = station.formattedRemainingCooldown()
                    delay(1000)
                }
            }
            AlertDialog(
                onDismissRequest = { showSupplyDialog = false },
                confirmButton = {},
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text("補給站", modifier = Modifier.align(Alignment.Center))
                        IconButton(
                            onClick = { showSupplyDialog = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(Icons.Default.Close, contentDescription = "關閉")
                        }
                    }
                },

                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        contentAlignment = Alignment.Center
                    ){
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            if (station.isAvailable()) {
                                Text("點擊下面按鈕領取資源！", style = MaterialTheme.typography.bodyLarge)
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(onClick = {
                                    station.nextClaimTime = System.currentTimeMillis()
                                    showSupplyDialog = false
                                }) {
                                    Text("領取資源")
                                }
                            } else {
                                Text(
                                    "還需等待 $cooldownText 才能再次領取資源。",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }
                }
            )
        }

        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 16.dp, top = 50.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Settings,
                contentDescription = "設定",
                tint = Color.Black
            )
        }

        if (showDialog) { //顯示視窗訊息
            AlertDialog(
                onDismissRequest = { showDialog = false },
                confirmButton = {},
                title = {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Text("設定", modifier = Modifier.align(Alignment.Center))
                        IconButton(
                            onClick = { showDialog = false },
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "關閉"
                            )
                        }
                    }
                },
                text = {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    ) {
                        Text(
                            "設定相關東西",
                            modifier = Modifier.align(Alignment.Center),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            )
        }

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Bottom
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Button(
                onClick = { navController.navigate("bag") },
                colors = buttonColors,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(width = 120.dp, height = 120.dp)
                    .padding(bottom = 60.dp)
            ) {
                Text("背包", fontSize = 20.sp)
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 10.dp, bottom = 320.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(
                onClick = { navController.navigate("favorites") },
                colors = buttonColors
            ) {
                Text("收藏冊")
            }
            Button(
                onClick = { navController.navigate("ranking") },
                colors = buttonColors
            ) {
                Text("排行榜")
            }
            Button(
                onClick = { navController.navigate("tasklist")},
                colors = buttonColors
            ) {
                Text("任務版")
            }
        }
    }
}