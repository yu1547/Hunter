package com.ntou01157.hunter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.maps.android.compose.*
import com.ntou01157.hunter.api.RetrofitClient // Correct import for RetrofitClient
import com.ntou01157.hunter.api.SpotApi
import com.ntou01157.hunter.api.SupplyApi
import com.ntou01157.hunter.data.RankRepository // Correct import for your RankRepository
import com.ntou01157.hunter.handlers.SpotLogHandler
import com.ntou01157.hunter.mock.FakeUser
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.models.User
import com.ntou01157.hunter.temp.*
import com.ntou01157.hunter.ui.*
import com.ntou01157.hunter.ui.event_ui.AncientTreeUI
import com.ntou01157.hunter.ui.event_ui.MerchantUI
import com.ntou01157.hunter.ui.event_ui.SlimeAttackUI
import com.ntou01157.hunter.ui.event_ui.StonePileUI
import com.ntou01157.hunter.ui.event_ui.TreasureBoxUI
import com.ntou01157.hunter.ui.event_ui.WordleGameUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

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

            // NavHost(navController = navController, startDestination = "login") {
            NavHost(navController = navController, startDestination = "login") {
                composable("login") { LoginScreen(navController) }
                composable("main") { MainScreen(navController) }
                composable("bag") { BagScreen(navController = navController) }
                // 收藏冊
                composable("favorites") {
                    var userId by remember { mutableStateOf<String?>(null) }
                    var pages by remember { mutableStateOf<List<List<Spot>>>(emptyList()) }
                    var pageIndex by remember { mutableStateOf(0) }
                    var selectedSpot by remember { mutableStateOf<Spot?>(null) }
                    var showLockedDialog by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        try {
                            val email = FirebaseAuth.getInstance().currentUser?.email
                            if (email != null) {
                                val apiUser = RetrofitClient.apiService.getUserByEmail(email)
                                userId = apiUser.id // 後端 User 的 id
                            } else {
                                // 沒登入就退而求其次用 FakeUser
                                userId = FakeUser.uid
                            }
                        } catch (e: Exception) {
                            Log.e("FavoritesScreen", "載入使用者失敗: ${e.message}", e)
                            userId = FakeUser.uid
                        }

                        pages = SpotLogHandler.getSpotPages()
                    }

                    if (userId == null) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    } else {
                        FavoritesScreen(
                                navController = navController,
                                userId = userId!!,
                                pageIndex = pageIndex,
                                onPageChange = { newIndex ->
                                    pageIndex =
                                            newIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
                                },
                                onSpotClicked = { spot -> selectedSpot = spot },
                                selectedSpot = selectedSpot,
                                onDismissSpotDialog = { selectedSpot = null },
                                showLockedDialog = showLockedDialog,
                                onDismissLockedDialog = { showLockedDialog = false }
                        )
                    }
                }

                composable("profile") {
                    val profileViewModel = viewModel<ProfileViewModel>()
                    val ctx = LocalContext.current

                    val doLogout: () -> Unit = {
                        // 雙保險：登出時一定關音樂
                        com.ntou01157.hunter.temp.MusicPlayerManager.pauseMusic()
                        com.google.firebase.auth.FirebaseAuth.getInstance().signOut()
                        navController.navigate("login") {
                            popUpTo(0) { inclusive = true }
                            launchSingleTop = true
                        }
                    }

                    ProfileScreen(
                            profileViewModel = profileViewModel,
                            navController = navController,
                            onLogout = doLogout
                    )
                }

                composable("ranking") { RankingScreen(navController = navController) }
                composable("tasklist") { TaskListScreen(navController) }

                composable("bugHunt") { WordleGameUI() }
                // 新增：事件 UI 的路由
                composable("ancientTree") {
                    // 修正：使用正確的 UI 函式名稱
                    AncientTreeUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("merchant") {
                    // 修正：使用正確的 UI 函式名稱
                    MerchantUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("slimeAttack") {
                    // 修正：使用正確的 UI 函式名稱
                    SlimeAttackUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("stonePile") {
                    StonePileUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("treasureBox") { //
                    // 修正：使用正確的 UI 函式名稱
                    TreasureBoxUI(onEventCompleted = { navController.popBackStack() })
                }
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: androidx.navigation.NavController) {
    var showDialog by remember { mutableStateOf(false) }
    var showChatDialog by remember { mutableStateOf(false) }
    val buttonColors =
            ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFbc8f8f),
                    contentColor = Color.White
            )

    // 打卡點 導入DB資料
    var spots by remember { mutableStateOf<List<Spot>>(emptyList()) }
    // 補給站 導入DB資料
    var supplyStations by remember { mutableStateOf<List<Supply>>(emptyList()) }
    LaunchedEffect(Unit) { supplyStations = withContext(Dispatchers.IO) { SupplyApi.getAll() } }
    var selectedSupply by remember { mutableStateOf<Supply?>(null) }
    var showSupplyDialog by remember { mutableStateOf(false) }
    val user: User = FakeUser // 後面要改
    val supplyLog = selectedSupply?.supplyId?.let { user.supplyScanLogs[it] }

    val context = LocalContext.current
    val locationPermissionState =
            rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
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

    // 載入所有打卡點
    LaunchedEffect(Unit) { spots = withContext(Dispatchers.IO) { SpotApi.getAllSpots() } }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true),
                onMapClick = { selectedSupply = null }
        ) {
            userLocation?.let { Marker(state = MarkerState(position = it), title = "所在位置") }
            // 顯示所有打卡點
            spots.forEach { spot ->
                spotMarker(
                        spot = spot,
                        userId = user.uid,
                        user = user,
                        navController = navController
                )
            }

            // 顯示補給站
            supplyStations.forEach { supply ->
                SupplyMarker(
                        supply = supply,
                        onClick = {
                            selectedSupply = it
                            showSupplyDialog = true
                        }
                )
            }
        }

        if (showSupplyDialog && selectedSupply != null) {
            SupplyHandlerDialog(
                    supply = selectedSupply!!,
                    user = user,
                    onDismiss = { showSupplyDialog = false }
            )
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
                            user = FakeUser, // 後面要改
                            onDismiss = { showDialog = false },
                            onNameChange = { newName -> },
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
                    modifier =
                            Modifier.align(Alignment.CenterHorizontally)
                                    .size(120.dp)
                                    .padding(bottom = 60.dp)
            ) { Text("背包", fontSize = 20.sp) }
        }

        Column(
                modifier =
                        Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 320.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp),
                horizontalAlignment = Alignment.End
        ) {
            Button(onClick = { navController.navigate("profile") }, colors = buttonColors) {
                Text("個人設定")
            }
            Button(onClick = { navController.navigate("favorites") }, colors = buttonColors) {
                Text("收藏冊")
            }
            Button(onClick = { navController.navigate("ranking") }, colors = buttonColors) {
                Text("排行榜")
            }
            Button(onClick = { navController.navigate("tasklist") }, colors = buttonColors) {
                Text("任務版")
            }
            Button(onClick = { navController.navigate("bugHunt") }, colors = buttonColors) {
                Text("啟動 BugHunt 任務")
            }
            Button(onClick = { navController.navigate("ancientTree") }, colors = buttonColors) {
                Text("古樹")
            }
            Button(onClick = { navController.navigate("merchant") }, colors = buttonColors) {
                Text("神秘商人")
            }
            Button(onClick = { navController.navigate("slimeAttack") }, colors = buttonColors) {
                Text("史萊姆戰鬥")
            }
            Button(onClick = { navController.navigate("stonePile") }, colors = buttonColors) {
                Text("石堆")
            }
            Button(onClick = { navController.navigate("treasureBox") }, colors = buttonColors) {
                Text("寶箱")
            }
            Button(onClick = { showChatDialog = true }, colors = buttonColors) { Text("客服") }

            // 客服聊天
            if (showChatDialog) {
                Dialog(onDismissRequest = { showChatDialog = false }) {
                    Surface(
                            shape = RoundedCornerShape(24.dp),
                            color = Color.White,
                            tonalElevation = 4.dp,
                            modifier = Modifier.width(350.dp).height(650.dp)
                    ) { ChatScreen(onClose = { showChatDialog = false }) }
                }
            }
        }
    }
}
