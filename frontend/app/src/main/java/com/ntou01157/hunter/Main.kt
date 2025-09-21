package com.ntou01157.hunter

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.ntou01157.hunter.api.RetrofitClient // Correct import for RetrofitClient
import com.ntou01157.hunter.api.SpotApi
import com.ntou01157.hunter.api.SupplyApi
import com.ntou01157.hunter.data.RankRepository // Correct import for your RankRepository
import com.ntou01157.hunter.handlers.MissionHandler
import com.ntou01157.hunter.handlers.SpotLogHandler
import androidx.lifecycle.viewmodel.compose.viewModel
import android.util.Log
import com.ntou01157.hunter.models.model_api.User as ApiUser
import com.ntou01157.hunter.models.*
import com.ntou01157.hunter.temp.*
import com.ntou01157.hunter.models.SupplyRepository
import com.ntou01157.hunter.ui.*
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.ntou01157.hunter.ui.event_ui.AncientTreeUI
import com.ntou01157.hunter.ui.event_ui.MerchantUI
import com.ntou01157.hunter.ui.event_ui.SlimeAttackUI
import com.ntou01157.hunter.ui.event_ui.StonePileUI
import com.ntou01157.hunter.ui.event_ui.TreasureBoxUI
import com.ntou01157.hunter.ui.event_ui.WordleGameUI
import com.ntou01157.hunter.models.model_api.expireAtOf
import com.ntou01157.hunter.utils.GeoVerifier
import kotlinx.coroutines.launch

class MainApplication : android.app.Application() {
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
            val profileViewModel: ProfileViewModel = viewModel()

            val onLogout: () -> Unit = {
                FirebaseAuth.getInstance().signOut()
                navController.navigate("login") {
                    popUpTo("login") { inclusive = true }
                }
            }



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
                        val email = FirebaseAuth.getInstance().currentUser?.email ?: return@LaunchedEffect
                        try {
                            val apiUser = RetrofitClient.apiService.getUserByEmail(email)
                            userId = apiUser.id
                        } catch (e: Exception) {
                            Log.e("FavoritesScreen", "載入使用者失敗: ${e.message}", e)
                            userId = null
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
                                pageIndex = newIndex.coerceIn(0, (pages.size - 1).coerceAtLeast(0))
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
                    val doLogout: () -> Unit = {
                        MusicPlayerManager.pauseMusic()
                        FirebaseAuth.getInstance().signOut()
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
                composable("ancientTree") {
                    AncientTreeUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("merchant") {
                    MerchantUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("slimeAttack") {
                    SlimeAttackUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("stonePile") {
                    StonePileUI(onEventCompleted = { navController.popBackStack() })
                }
                composable("treasureBox") {
                    TreasureBoxUI(onEventCompleted = { navController.popBackStack() })
                }
                /*composable("settings") {
                    SettingsScreen(
                        navController = navController,
                        profileViewModel = profileViewModel,
                        onLogout = onLogout
                    )
                }*/
            }
        }
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MainScreen(navController: androidx.navigation.NavHostController) {
    var showChatDialog by remember { mutableStateOf(false) }
    var apiUser by remember { mutableStateOf<ApiUser?>(null) }

    // ===== Buff 狀態 ===========================================
    var branchExpireAt by remember { mutableStateOf<Long?>(null) }
    LaunchedEffect(Unit) {
        try {
            val email = FirebaseAuth.getInstance().currentUser?.email ?: return@LaunchedEffect
            val u: ApiUser = RetrofitClient.apiService.getUserByEmail(email)
            branchExpireAt = u.buff.expireAtOf("ancient_branch")
            apiUser = u
        } catch (e: Exception) {
            Log.e("MainScreen", "load buff failed: ${e.message}", e)
        }
    }

    // 打卡點 / 補給站資料
    var spots by remember { mutableStateOf<List<Spot>>(emptyList()) }
    var supplyStations by remember { mutableStateOf<List<Supply>>(emptyList()) }
    var selectedSupply by remember { mutableStateOf<Supply?>(null) }
    var showSupplyDialog by remember { mutableStateOf(false) }

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
        } else locationPermissionState.launchPermissionRequest()
    }

    // 載入 DB
    LaunchedEffect(Unit) {
        spots = withContext(Dispatchers.IO) { SpotApi.getAllSpots() }
        supplyStations = withContext(Dispatchers.IO) { SupplyApi.getAll() }
    }

    // 底部導航列的項目
    val bottomItems = listOf(
        NavItem("ranking", "排行榜", R.drawable.ranklist_icon),
        NavItem("tasklist", "任務版", R.drawable.tasklist_icon),
        NavItem("bag", "背包", R.drawable.backpack_icon),
        NavItem("favorites", "收藏冊", R.drawable.checkinbook_icon),
        NavItem("profile", "個人檔案", R.drawable.profile_icon),
    )
    val scope = rememberCoroutineScope()
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar {
                val currentRoute = navController.currentBackStackEntry?.destination?.route
                bottomItems.forEach { item ->
                    NavigationBarItem(
                        selected = currentRoute == item.route,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo("main") { inclusive = false }
                                launchSingleTop = true
                            }
                        },
                        icon = {
                            Icon(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.label,
                                tint = Color.Unspecified,
                                modifier = Modifier.size(40.dp)
                            )
                        },
                        label = { Text(item.label, fontSize = 9.sp) }
                    )
                }
            }
        }
    ) { innerPadding ->
        val bottomPadding = innerPadding.calculateBottomPadding()
        Box(modifier = Modifier.fillMaxSize().padding(bottom = bottomPadding)) {
            GoogleMap(
                modifier = Modifier.matchParentSize(),
                cameraPositionState = cameraPositionState,
                uiSettings = MapUiSettings(myLocationButtonEnabled = true),
                properties = MapProperties(isMyLocationEnabled = true),
                onMapClick = { selectedSupply = null }
            ) {
                userLocation?.let {
                    Marker(state = MarkerState(position = it), title = "所在位置")
                }
                apiUser?.let { u ->
                    spots.forEach { spot -> spotMarker(spot = spot, userId = u.id) }
                }
                supplyStations.forEach { supply ->
                    SupplyMarker(supply = supply, onClick = {
                        scope.launch {
                            val loc = locationService.getCurrentLocation()
                            if (loc == null) {
                                android.widget.Toast.makeText(context, "無法取得定位", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            val current = LatLng(loc.latitude, loc.longitude)
                            val target = LatLng(supply.latitude, supply.longitude)
                            val distance = GeoVerifier.distanceMeters(current, target)
                            val inRange = distance <= GeoVerifier.DEFAULT_THRESHOLD_METERS
                            if (!inRange) {
                                android.widget.Toast.makeText(context, "離該補給站不夠近，再靠近一點喔!", android.widget.Toast.LENGTH_SHORT).show()
                                return@launch
                            }
                            selectedSupply = supply
                            showSupplyDialog = true
                        }
                    })
                }
            }

            if (showSupplyDialog && selectedSupply != null && apiUser != null) {
                SupplyHandlerDialog(
                    supply = selectedSupply!!,
                    user = apiUser!!,
                    onDismiss = { showSupplyDialog = false },
                    navController = navController
                )
            }

            // 右側活動類按鈕
            Column(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 10.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                val sideButtonColors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFbc8f8f),
                    contentColor = Color.White
                )

//                Button(onClick = { navController.navigate("bugHunt") }, colors = sideButtonColors) {
//                    Text("BugHunt")
//                }
//                Button(onClick = { navController.navigate("ancientTree") }, colors = sideButtonColors) {
//                    Text("古樹")
//                }
//                Button(onClick = { navController.navigate("merchant") }, colors = sideButtonColors) {
//                    Text("商人")
//                }
//                Button(onClick = { navController.navigate("slimeAttack") }, colors = sideButtonColors) {
//                    Text("史萊姆")
//                }
//                Button(onClick = { navController.navigate("stonePile") }, colors = sideButtonColors) {
//                    Text("石堆")
//                }
//                Button(onClick = { navController.navigate("treasureBox") }, colors = sideButtonColors) {
//                    Text("寶箱")
//                }
            }

            // 客服
            FloatingActionButton(
                onClick = { showChatDialog = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 24.dp, bottom = 96.dp),
                containerColor = Color(0xFFbc8f8f),
                contentColor = Color.White,
                shape = RoundedCornerShape(50)
            ) {
                Text("客服", fontSize = 16.sp)
            }

            if (showChatDialog) {
                Dialog(onDismissRequest = { showChatDialog = false }) {
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = Color.White,
                        tonalElevation = 4.dp,
                        modifier = Modifier
                            .width(350.dp)
                            .height(650.dp)
                    ) {
                        ChatScreen(onClose = { showChatDialog = false })
                    }
                }
            }

            BuffBadge(
                expireAtMillis = branchExpireAt,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(start = 12.dp, top = 12.dp)
            )
        }
    }
}


data class NavItem(val route: String, val label: String, val iconRes: Int)


/**
 * 在左上角顯示：圓形圖片 + 剩餘時間，剩餘時間每秒更新。
 * 若 expireAtMillis 為 null 或已過期，不顯示任何內容。
 */
@Composable
private fun BuffBadge(
    expireAtMillis: Long?,
    modifier: Modifier = Modifier
) {
    if (expireAtMillis == null) return

    var now by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(1000)
        }
    }
    val remaining = expireAtMillis - now
    if (remaining <= 0L) return

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(id = R.drawable.itembranch),
            contentDescription = "ancient_branch",
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = formatRemaining(remaining),
            style = MaterialTheme.typography.labelSmall,
            color = Color.Black
        )
    }
}

private fun formatRemaining(millis: Long): String {
    val totalSec = (millis / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}
