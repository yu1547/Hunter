package com.ntou01157.hunter.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.model.model_api.User as ApiUser
import com.ntou01157.hunter.model.model_api.BackpackItem
import com.ntou01157.hunter.model.model_api.Mission
import androidx.compose.ui.platform.LocalContext
import com.ntou01157.hunter.model.model_api.Settings as ApiSettings
import com.ntou01157.hunter.models.User as UiUser
import com.ntou01157.hunter.models.Settings as UiSettings
import kotlinx.coroutines.launch
import com.ntou01157.hunter.temp.MusicPlayerManager

@Composable
fun SettingDialog(
    user: UiUser,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onLogout: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var musicEnabled by remember { mutableStateOf(false) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("zh-TW") }

    var isEditingName by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf(user.displayName) }

    val userId = "68846d797609912e5e6ba9b0"

    // ⏬ 初始化設定資料
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            try {
                val fetchedUser = RetrofitClient.apiService.getUser(userId)
                fetchedUser.settings?.let {
                    musicEnabled = it.music
                    notificationsEnabled = it.notification
                    selectedLanguage = it.language
                }
            } catch (e: Exception) {
                Log.e("SettingDialog", "取得設定失敗：${e.message}")
            }
        }
    }

    fun updateSettings() {
        coroutineScope.launch {
            try {
                val updatedSettings = ApiSettings(
                    music = musicEnabled,
                    notification = notificationsEnabled,
                    language = selectedLanguage
                )
                RetrofitClient.apiService.updateSettings(userId, updatedSettings)
                Log.d("SettingDialog", "設定已送出")

                // 音樂控制
                if (musicEnabled) {
                    MusicPlayerManager.playMusic(context)
                } else {
                    MusicPlayerManager.pauseMusic()
                }

            } catch (e: Exception) {
                Log.e("SettingDialog", "更新設定失敗：${e.message}")
            }
        }
    }



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

            Column {
                Text("名稱", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = nameText,
                    onValueChange = { if (isEditingName) nameText = it },
                    enabled = isEditingName,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (isEditingName) {
                                onNameChange(nameText.trim())
                            }
                            isEditingName = !isEditingName
                        }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = if (isEditingName) "確定修改" else "編輯名稱"
                            )
                        }
                    }
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("聲音", style = MaterialTheme.typography.titleMedium)
                CustomSwitch(isOn = musicEnabled, onToggle = {
                    musicEnabled = it
                    updateSettings()
                })
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("通知", style = MaterialTheme.typography.titleMedium)
                CustomSwitch(isOn = notificationsEnabled, onToggle = {
                    notificationsEnabled = it
                    updateSettings()
                })
            }

            Text("語言", style = MaterialTheme.typography.titleMedium)
            LanguageDropdown(selected = selectedLanguage, onSelect = {
                selectedLanguage = it
                updateSettings()
            })

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("登出", color = Color.White)
            }
        }
    }
}

@Composable
fun CustomSwitch(isOn: Boolean, onToggle: (Boolean) -> Unit) {
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
