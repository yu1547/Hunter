package com.ntou01157.hunter

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntou01157.hunter.models.Item

@Composable
fun ItemDetailDialog(
    item: Item,
    onDismiss: () -> Unit,
    onCraftClicked: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { onDismiss() },
        confirmButton = {},
        title = {
            Box(Modifier.fillMaxWidth()) {
                Text(" ")
                Text(
                    "✕",
                    modifier = Modifier.align(Alignment.TopEnd)
                        .clickable { onDismiss() },
                    fontSize = 24.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 8.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("稀有度：${item.itemRarity}")
                    Text("擁有 ${item.count.value} 件")
                }
                Spacer(modifier = Modifier.height(12.dp))
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text("物品介紹：", fontSize = 16.sp)
                    Text(text = item.itemEffect, modifier = Modifier.padding(top = 4.dp))
                }
                if (item.isblend) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onCraftClicked,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp)
                    ) {
                        Text("前往合成")
                    }
                }
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}
