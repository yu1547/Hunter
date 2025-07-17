package com.ntou01157.hunter

import android.annotation.SuppressLint
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.*
import androidx.navigation.NavHostController
import kotlinx.coroutines.launch

@SuppressLint("UnrememberedMutableState")
@Composable
fun BagScreen(navController: NavHostController) {
    val allItems = remember {
        mutableStateListOf(
            Item(1, "用於開啟寶箱", "金鑰匙", 1, "開啟寶箱", 2, "由銀鑰匙合成", "UR", false, R.drawable.item1),
            Item(2, "讓我充數一下", "史萊姆", 1, "就很可愛讓你觀賞", 4, "路邊撿到的", "S", false, R.drawable.item2),
            Item(3, "用來合成出史萊姆的素材", "史萊姆球", 0, "史萊姆球是史萊姆身體的一部分", 4, "做任務獲得", "R", true, R.drawable.item3),
            Item(4, "就是水", "水滴", 0, "可以用來合成各種素材", 3, "做任務得到", "R", true, R.drawable.item4),
            Item(5, "黃金碎片", "金鑰匙碎片", 0, "可以用來合成金鑰匙", 3, "做任務得到", "R", true, R.drawable.item5),
        )
    }

    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedItem by remember { mutableStateOf<Item?>(null) }
    var filterState by remember { mutableStateOf(0) }
    var showCraftDialog by remember { mutableStateOf(false) }
    var currentRecipeIndex by remember { mutableStateOf(0) }

    val filteredItems = when (filterState) {
        1 -> allItems.filter { it.itemType == 0 && it.count.value > 0 }
        2 -> allItems.filter { it.itemType == 1 && it.count.value > 0 }
        else -> allItems.filter { it.count.value > 0 }
    }

    val matchingRecipes by derivedStateOf {
        recipes.filter { it.requiredItems.containsKey(selectedItem?.itemid) }
    }
    val currentRecipe = matchingRecipes.getOrNull(currentRecipeIndex)

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF3DCDC))
                .padding(horizontal = 16.dp)
                .padding(paddingValues)
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

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFEFEFEF)).padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                listOf("(全部)" to 0, "(碎片)" to 1, "(道具)" to 2).forEach { (label, type) ->
                    Text(
                        label,
                        modifier = Modifier.clickable { filterState = type },
                        color = if (filterState == type) Color.Black else Color.Gray
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.75f)
                    .background(Color(0xFFDADADA))
                    .padding(16.dp)
            ) {
                items(filteredItems) { item ->
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(Color.White)
                            .clickable { selectedItem = item },
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Image(
                            painter = painterResource(id = item.imageResId),
                            contentDescription = item.itemName,
                            modifier = Modifier.fillMaxSize()
                        )
                        Text("${item.count.value}", color = Color.Black, modifier = Modifier.padding(4.dp), fontSize = 14.sp)
                    }
                }
            }

            selectedItem?.let { item ->
                ItemDetailDialog(
                    item = item,
                    onDismiss = { selectedItem = null },
                    onCraftClicked = {
                        currentRecipeIndex = 0
                        showCraftDialog = true
                    }
                )
            }

            if (showCraftDialog) {
                CraftDialog(
                    allItems = allItems,
                    recipes = recipes,
                    selectedItem = selectedItem,
                    onDismiss = {
                        showCraftDialog = false
                        selectedItem = null
                        currentRecipeIndex = 0
                    },
                    snackbarHostState = snackbarHostState
                )
            }

        }
    }
}
