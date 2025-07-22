package com.ntou01157.hunter.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ntou01157.hunter.R
import com.ntou01157.hunter.models.*

@Composable
fun FavoritesScreen(
    navController: NavHostController,
    user: User,
    pages: List<List<Spot>>,
    pageIndex: Int,
    onPageChange: (Int) -> Unit,
    onSpotClicked: (Spot) -> Unit,
    selectedSpot: Spot?,
    onDismissSpotDialog: () -> Unit,
    showLockedDialog: Boolean,
    onDismissLockedDialog: () -> Unit
) {
    val buttonColors = ButtonDefaults.buttonColors(
        containerColor = Color(0xFFbc8f8f),
        contentColor = Color.White
    )

    val items = pages[pageIndex]

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
                for (row in 0..1) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        for (col in 0..1) {
                            val index = row * 2 + col
                            if (index < items.size) {
                                val landmark = items[index]

                                val isUnlocked = user.spotsScanLogs[landmark.spotId]?.isCheck == true

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .background(if (isUnlocked) Color.White else Color.LightGray)
                                        .clickable { onSpotClicked(landmark) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = landmark.spotName,
                                        color = if (isUnlocked) Color.Black else Color.Gray
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onPageChange(pageIndex - 1) },
                colors = buttonColors,
                enabled = pageIndex > 0
            ) {
                Text("<")
            }
            Button(
                onClick = { onPageChange(pageIndex + 1) },
                colors = buttonColors,
                enabled = pageIndex < pages.size - 1
            ) {
                Text(">")
            }
        }
    }

    // 解鎖彈窗
    selectedSpot?.let {
        AlertDialog(
            onDismissRequest = onDismissSpotDialog,
            confirmButton = {
                TextButton(onClick = onDismissSpotDialog) {
                    Text("關閉")
                }
            },
            title = { Text(it.spotName) },
            text = { Text("內容說明。") }
        )
    }

    // 鎖定提示彈窗
    if (showLockedDialog) {
        AlertDialog(
            onDismissRequest = onDismissLockedDialog,
            confirmButton = {
                TextButton(onClick = onDismissLockedDialog) {
                    Text("確定")
                }
            },
            text = { Text("此地標尚未解鎖，請先前往現場打卡！") }
        )
    }
}
