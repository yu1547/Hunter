package com.ntou01157.hunter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.ntou01157.hunter.mock.FakeUser
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.models.SupplyRepository
import com.ntou01157.hunter.models.User
import com.ntou01157.hunter.ui.*
import com.ntou01157.hunter.api.RetrofitClient // Correct import for RetrofitClient
import com.ntou01157.hunter.data.RankRepository // Correct import for your RankRepository


class MainApplication : android.app.Application() {

    // 聲明為 lateinit var，因為它會在 onCreate 中初始化
    lateinit var rankRepository: RankRepository

    override fun onCreate() {
        super.onCreate()
        rankRepository = RankRepository(RetrofitClient.apiService)
    }
}



class Main : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("main") {
                    MainScreen(navController)
                }
                composable("bag") {
                    BagScreen(navController = navController)
                }
                composable("favorites") {
                    var pageIndex by remember { mutableStateOf(0) }
                    var selectedSpot by remember { mutableStateOf<Spot?>(null) }
                    var showLockedDialog by remember { mutableStateOf(false) }

                    val user = FakeUser
                    val pages = MockSpotData.pages

                    FavoritesScreen(
                        navController = navController,
                        user = user,
                        pages = pages,
                        pageIndex = pageIndex,
                        onPageChange = { pageIndex = it },
                        onSpotClicked = { spot ->
                            val isUnlocked = user.spotsScanLogs[spot.spotId] == true
                            if (isUnlocked) {
                                selectedSpot = spot
                            } else {
                                showLockedDialog = true
                            }
                        },
                        selectedSpot = selectedSpot,
                        onDismissSpotDialog = { selectedSpot = null },
                        showLockedDialog = showLockedDialog,
                        onDismissLockedDialog = { showLockedDialog = false }
                    )
                }

                composable("ranking") {
                    RankingScreen(navController = navController)
                }
                composable("tasklist") {
                    TaskListScreen(navController)
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: androidx.navigation.NavHostController) {
    var showDialog by remember { mutableStateOf(false) }
    // 客服對話框
    var showChatDialog by remember { mutableStateOf(false) }
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )

    // 打卡點資料，從 MockSpotData 取得
    val missionLandmark = remember { mutableStateOf(MockSpotData.allSpots) }

    //補給站
    val supplyStations = remember { SupplyRepository.supplyStations }
    val user: User = FakeUser
    val (spotsScanLogs, setSpotsScanLogs) = remember { mutableStateOf(user.spotsScanLogs) }

    val context = LocalContext.current
    val locationPermissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val locationService = remember { LocationService(context) }
    val defaultLatLng = LatLng(25.149995, 121.778730)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLatLng, 16f)
    }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }

    LaunchedEffect(locationPermissionState.status.isGranted) {
        if (locationPermissionState.status.isGranted) {
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

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(myLocationButtonEnabled = true),
            properties = MapProperties(isMyLocationEnabled = true),
            onMapClick = {  }
        ) {
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "所在位置"
                )
            }

            // 顯示打卡點
            SpotMapSection(
                missionLandmark = missionLandmark.value,
                spotsScanLogs = spotsScanLogs,
                setSpotsScanLogs = setSpotsScanLogs,
                userLocation = userLocation,
                cameraPositionState = cameraPositionState
            )

            // 顯示補給站
            SupplyMapSection(
                supplyStations = supplyStations,
                user = user,
                userLocation = userLocation,
                cameraPositionState = cameraPositionState
            )
        }

        IconButton(
            onClick = { showDialog = true },
            modifier = Modifier.align(Alignment.TopStart).padding(start = 16.dp, top = 50.dp)
        ) {
            Icon(Icons.Default.Settings, contentDescription = "設定", tint = Color.Black)
        }

        if (showDialog) {
            Dialog(onDismissRequest = { showDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color(0xFFF6EDF7),
                    tonalElevation = 4.dp,
                    modifier = Modifier.width(280.dp).wrapContentHeight()
                ) {
                    SettingDialog(
                        user = FakeUser,
                        onDismiss = { showDialog = false },
                        onNameChange = {newName -> },
                        onLogout = {}
                    )
                }
            }
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
                modifier = Modifier.align(Alignment.CenterHorizontally).size(120.dp).padding(bottom = 60.dp)
            ) {
                Text("背包", fontSize = 20.sp)
            }
        }

        Column(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 200.dp),
            verticalArrangement = Arrangement.spacedBy(30.dp),
            horizontalAlignment = Alignment.End
        ) {
            Button(onClick = { navController.navigate("favorites") }, colors = buttonColors) {
                Text("收藏冊")
            }
            Button(onClick = { navController.navigate("ranking") }, colors = buttonColors) {
                Text("排行榜")
            }
            Button(onClick = { navController.navigate("tasklist") }, colors = buttonColors) {
                Text("任務版")
            }
            Button(
                onClick = { showChatDialog = true }, colors = buttonColors) {
                Text("客服")
            }
        }

        // 客服聊天
        if (showChatDialog) {
            Dialog(onDismissRequest = { showChatDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.White,
                    tonalElevation = 4.dp,
                    modifier = Modifier.width(350.dp).height(650.dp)
                ) {
                    ChatScreen(
                        onClose = { showChatDialog = false }
                    )
                }
            }
        }
    }
}