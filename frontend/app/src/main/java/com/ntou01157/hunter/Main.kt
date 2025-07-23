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
import com.ntou01157.hunter.temp.RankingRepository
import com.ntou01157.hunter.ui.*
import java.io.BufferedReader
import java.io.InputStreamReader
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Close
import androidx.compose.foundation.shape.RoundedCornerShape


class Main : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val context = LocalContext.current
            val jsonString = remember {
                context.assets.open("rankjson.json").bufferedReader().use { it.readText() }
            }
            val rankResponse = remember { RankingRepository.parseRankingJson(jsonString) }

            NavHost(navController = navController, startDestination = "login") {
                composable("login") {
                    LoginScreen(navController)
                }
                composable("main") {
                    MainScreen(navController)
                }
                composable("bag") {
                    BagScreen(navController = navController, userId = "6879fdbc125a5443a1d4bade")
                }
                composable("favorites") {
                    FavoritesScreen(navController)
                }
                composable("ranking") {
                    RankingScreen(rankResponse = rankResponse, navController = navController)
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
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )
    val missionLandmark = Spot("打卡點1", "(地標)", "", 25.149853, 121.778352)
    val supplyStations = remember {
        listOf(
            Supply("station1", "補給站 1", 25.149034, 121.779087),
            Supply("station2", "補給站 2", 25.149836, 121.779452)
        )
    }
    var selectedSupply by remember { mutableStateOf<Supply?>(null) }
    var showSupplyDialog by remember { mutableStateOf(false) }
    val user: User = FakeUser
    val supplyLog = selectedSupply?.supplyId?.let { user.supplyScanLogs[it] }

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
            onMapClick = { selectedSupply = null }
        ) {
            userLocation?.let {
                Marker(
                    state = MarkerState(position = it),
                    title = "所在位置"
                )
            }
            supplyStations.forEach { station ->
                SupplyMarker(station) {
                    selectedSupply = it
                    showSupplyDialog = true
                }
            }
            spotMarker(missionLandmark)
        }

        if (showSupplyDialog && selectedSupply != null) {
            SupplyDialog(
                supply = selectedSupply!!,
                onDismiss = { showSupplyDialog = false },
                onCollect = { showSupplyDialog = false },
                isAvailable = isSupplyAvailable(supplyLog),
                remainingTimeFormatted = { formattedRemainingCooldown(supplyLog) }
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
                    SettingDialog(user = FakeUser, onDismiss = { showDialog = false })
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
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 10.dp, bottom = 320.dp),
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
        }
    }
}
