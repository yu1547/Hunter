package com.ntou01157.hunter

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.ntou01157.hunter.models.Item

@Composable
fun CraftDialog(
    allItems: MutableList<Item>,
    recipes: List<Recipe>,
    selectedItem: Item?,
    onDismiss: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val coroutineScope = rememberCoroutineScope()
    val matchingRecipes = remember(selectedItem) {
        recipes.filter { it.requiredItems.containsKey(selectedItem?.itemid) }
    }
    var currentRecipeIndex by remember { mutableStateOf(0) }
    val currentRecipe = matchingRecipes.getOrNull(currentRecipeIndex)
    val resultItem = allItems.find { it.itemid == currentRecipe?.resultItemId }

    if (currentRecipe != null && resultItem != null) {
        AlertDialog(
            onDismissRequest = {
                onDismiss()
                currentRecipeIndex = 0
            },
            title = {
                Box(Modifier.fillMaxWidth()) {
                    Text("合成物", modifier = Modifier.align(Alignment.Center))
                    Text(
                        "✕",
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .clickable {
                                onDismiss()
                                currentRecipeIndex = 0
                            },
                        fontSize = 24.sp
                    )
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Image(
                        painter = painterResource(id = resultItem.imageResId),
                        contentDescription = resultItem.itemName,
                        modifier = Modifier.size(100.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (matchingRecipes.size > 1) {
                            Text("<", fontSize = 24.sp, modifier = Modifier
                                .clickable {
                                    currentRecipeIndex =
                                        (currentRecipeIndex - 1 + matchingRecipes.size) % matchingRecipes.size
                                }
                                .padding(8.dp))
                        }

                        currentRecipe.requiredItems.forEach { (id, count) ->
                            val material = allItems.find { it.itemid == id }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                material?.let {
                                    Image(
                                        painter = painterResource(id = it.imageResId),
                                        contentDescription = it.itemName,
                                        modifier = Modifier.size(50.dp)
                                    )
                                    Text("x$count")
                                }
                            }
                        }

                        if (matchingRecipes.size > 1) {
                            Text(">", fontSize = 24.sp, modifier = Modifier
                                .clickable {
                                    currentRecipeIndex = (currentRecipeIndex + 1) % matchingRecipes.size
                                }
                                .padding(8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    currentRecipe.let { recipe ->
                        val crafted = CraftingSystem.craftItem(allItems, recipe)
                        if (!crafted) {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("材料不足，無法合成")
                            }
                        } else {
                            onDismiss()
                            currentRecipeIndex = 0
                        }
                    }
                }) {
                    Text("合成")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}
