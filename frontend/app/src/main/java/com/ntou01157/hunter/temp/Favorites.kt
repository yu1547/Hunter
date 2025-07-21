package com.ntou01157.hunter

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.google.android.gms.maps.model.LatLng

data class Spots(
    val spotId: String,
    val spotName: String,
    val spotPhoto: String,
    val position: LatLng,
    val isUnlocked: Boolean
)
//收藏冊
@Composable
fun FavoritesScreen(navController: NavHostController) {
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )
    //當前頁面索引
    var pageIndex by remember { mutableStateOf(0) }

    var selectedLandmark by remember { mutableStateOf<Spots?>(null) }
    var showLockedDialog by remember { mutableStateOf(false) }

    val pageData = listOf(
        listOf(
            Spots("1", "地標A", "", LatLng(0.0, 0.0), false),
            Spots("2", "寰宇之書", "", LatLng(0.0, 0.0), true),
            Spots("3", "地標C", "", LatLng(0.0, 0.0), false),
            Spots("4", "地標D", "", LatLng(0.0, 0.0), false)
        ),
        listOf(
            Spots("5", "地標E", "", LatLng(0.0, 0.0), false),
            Spots("6", "地標F", "", LatLng(0.0, 0.0), false),
            Spots("7", "地標G", "", LatLng(0.0, 0.0), false),
            Spots("8", "地標H", "", LatLng(0.0, 0.0), false)
        )
    )

    val totalPages = pageData.size

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF3DCDC))
            .padding(horizontal = 16.dp)
    ) {
        IconButton(
            onClick = { navController.navigate("main") },
            modifier = Modifier.padding(top = 25.dp, bottom = 4.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_home),
                contentDescription = "回首頁",
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .width(370.dp)
                    .fillMaxHeight(0.8f)
                    .background(Color(0xFFDADADA))
                    .padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val items = pageData[pageIndex % pageData.size]
                //每頁顯示2X2地標
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            val landmark = items[index]

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .background(if (landmark.isUnlocked) Color.White else Color.LightGray)
                                    .clickable {
                                        if (landmark.isUnlocked) {
                                            selectedLandmark = landmark
                                        } else {
                                            showLockedDialog = true
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = landmark.spotName,
                                    color = if (landmark.isUnlocked) Color.Black else Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }

        //分頁按鈕
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = {
                    if (pageIndex > 0) pageIndex--
                },
                colors = buttonColors,
                enabled = pageIndex > 0
            ) {
                Text("<")
            }
            Button(
                onClick = {
                    if (pageIndex < totalPages - 1) pageIndex++
                },
                colors = buttonColors,
                enabled = pageIndex < totalPages - 1
            ) {
                Text(">")
            }
        }
    }

    // 顯示解鎖的地標內容彈窗
    selectedLandmark?.let { landmark ->
        AlertDialog(
            onDismissRequest = { selectedLandmark = null },
            confirmButton = {
                TextButton(onClick = { selectedLandmark = null }) {
                    Text("關閉")
                }
            },
            title = { Text(landmark.spotName) },
            text = { Text("內容說明。") }
        )
    }

    // 顯示地標未解鎖提示
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = { showLockedDialog = false },
            confirmButton = {
                TextButton(onClick = { showLockedDialog = false }) {
                    Text("確定")
                }
            },
            //title = { Text("尚未解鎖") },
            text = { Text("此地標尚未解鎖，請先前往現場打卡！") }
        )
    }
}

