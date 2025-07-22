package com.ntou01157.hunter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.shape.RoundedCornerShape
import com.ntou01157.hunter.models.User
import com.ntou01157.hunter.mock.FakeUser

@Composable
fun SettingDialog(user: User, onDismiss: () -> Unit) {
    var musicEnabled by remember { mutableStateOf(user.settings.music) }
    var notificationsEnabled by remember { mutableStateOf(user.settings.notification) }
    var selectedLanguage by remember { mutableStateOf(user.settings.language) }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color(0xFFF6EDF7),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Text("設定", style = MaterialTheme.typography.titleLarge, modifier = Modifier.align(Alignment.Center))
                IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd)) {
                    Icon(Icons.Default.Close, contentDescription = "關閉")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("聲音", style = MaterialTheme.typography.titleMedium)
                CustomSwitch(isOn = musicEnabled, onToggle = { musicEnabled = it })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("通知", style = MaterialTheme.typography.titleMedium)
                CustomSwitch(isOn = notificationsEnabled, onToggle = { notificationsEnabled = it })
            }

            Text("語言", style = MaterialTheme.typography.titleMedium)
            LanguageDropdown(selected = selectedLanguage, onSelect = { selectedLanguage = it })
        }
    }
}

@Composable
fun CustomSwitch(isOn: Boolean, onToggle: (Boolean) -> Unit) {
    val label = if (isOn) "ON" else "OFF"
    val backgroundColor = if (isOn) Color(0xFFEFE6E6) else Color(0xFFD9CCCC)

    Box(
        modifier = Modifier
            .width(60.dp)
            .height(30.dp)
            .background(backgroundColor, shape = RoundedCornerShape(15.dp))
            .clickable { onToggle(!isOn) },
        contentAlignment = if (isOn) Alignment.CenterEnd else Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (isOn) Text("ON", modifier = Modifier.padding(start = 6.dp), fontSize = 10.sp)
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .padding(2.dp)
                    .background(Color.White, shape = RoundedCornerShape(12.dp))
            )
            if (!isOn) Text("OFF", modifier = Modifier.padding(end = 6.dp), fontSize = 10.sp)
        }
    }
}

@Composable
fun LanguageDropdown(selected: String, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("zh-TW", "English")

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected)
        }

        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}


