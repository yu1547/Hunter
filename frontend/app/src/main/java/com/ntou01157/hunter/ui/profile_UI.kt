package com.ntou01157.hunter.ui

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.Settings as ApiSettings
import com.ntou01157.hunter.temp.MusicPlayerManager
import com.ntou01157.hunter.temp.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    profileViewModel: ProfileViewModel,
    navController: NavController,
    onLogout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val userState by profileViewModel.user.collectAsState()

    var isEditing by remember { mutableStateOf(false) }

    var editedUsername by remember { mutableStateOf("") }
    var editedGender by remember { mutableStateOf("") }
    var editedAge by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var userId by remember { mutableStateOf<String?>(null) }
    var musicEnabled by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("zh-TW") }

    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { picked ->
        if (picked != null) {
            selectedImageUri = picked
            coroutineScope.launch {
                val r = profileViewModel.uploadPhotoToCloudinary(picked, context)
                r.fold(
                    onSuccess = { snackbarHostState.showSnackbar("頭像已更新") },
                    onFailure = { e -> snackbarHostState.showSnackbar(e.message ?: "上傳失敗") }
                )
            }
        }
    }

    LaunchedEffect(Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email ?: return@LaunchedEffect
        profileViewModel.fetchUserProfile(email)
        try {
            val apiUser = RetrofitClient.apiService.getUserByEmail(email)
            userId = apiUser.id
            val s = apiUser.settings?.firstOrNull()
            musicEnabled = s?.music ?: false
            selectedLanguage = s?.language ?: "zh-TW"
        } catch (e: Exception) {
            Log.e("ProfileScreen", "載入設定失敗: ${e.message}", e)
        }
    }

    LaunchedEffect(userState) {
        val u = userState ?: return@LaunchedEffect
        editedUsername = u.username.orEmpty()
        editedGender = u.gender.orEmpty()
        editedAge = u.age.orEmpty()
    }

    fun updateSettings() {
        coroutineScope.launch {
            try {
                val id = userId ?: return@launch
                // 注意：notification 改傳 false，避免傳 null 造成型別錯誤
                val updated = ApiSettings(
                    music = musicEnabled,
                    notification = false,
                    language = selectedLanguage
                )
                RetrofitClient.apiService.updateSettings(id, updated)
                if (musicEnabled) MusicPlayerManager.playMusic(context) else MusicPlayerManager.pauseMusic()
            } catch (e: Exception) {
                Log.e("ProfileScreen", "更新設定失敗: ${e.message}", e)
            }
        }
    }

    Scaffold(
        topBar = {
            Box(modifier = Modifier.fillMaxWidth()) {
                IconButton(
                    onClick = { navController.navigate("main") },
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(top = 25.dp, start = 16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "回首頁",
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
        },

                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding)
                .offset(y = (-24).dp),   // 往上移 24dp
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { isEditing = !isEditing }) {
                    Text(if (isEditing) "取消" else "編輯")
                }
            }

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = userState?.photoURL.orEmpty()
                when {
                    selectedImageUri != null -> Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "選擇的頭像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    photoUrl.isNotBlank() -> Image(
                        painter = rememberAsyncImagePainter(photoUrl),
                        contentDescription = "已設定的頭像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            val u = userState
            if (u == null) {
                Text("載入中...", style = MaterialTheme.typography.bodyLarge)
            } else {
                if (isEditing) {
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { v -> editedUsername = v },
                        label = { Text("暱稱") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    DropdownField(
                        label = "性別",
                        options = listOf("男", "女", "不透露"),
                        value = if (editedGender.isBlank()) "不透露" else editedGender,
                        onValueChange = { v -> editedGender = v },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    DropdownField(
                        label = "年齡",
                        options = (1..50).map { it.toString() },
                        value = if (editedAge.isBlank()) "1" else editedAge,
                        onValueChange = { v -> editedAge = v },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )
                    Button(
                        onClick = {
                            val id = u.id ?: return@Button
                            profileViewModel.updateUserProfile(
                                userId = id,
                                username = editedUsername,
                                gender = editedGender,
                                age = editedAge
                            )
                            isEditing = false
                        },
                        modifier = Modifier.padding(top = 12.dp)
                    ) { Text("儲存變更") }
                } else {
                    Text("暱稱：${u.username}", style = MaterialTheme.typography.bodyLarge)
                    Text("性別：${u.gender}", style = MaterialTheme.typography.bodyLarge)
                    Text("年齡：${u.age}", style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(24.dp))
            Divider()
            Spacer(Modifier.height(8.dp))

            // 設定區塊：只保留聲音、語言、登出
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFFF6EDF7)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("設定", style = MaterialTheme.typography.titleMedium)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("聲音", style = MaterialTheme.typography.bodyLarge)
                        // 這裡沿用 Setting_UI.kt 裡現成的 CustomSwitch
                        CustomSwitch(
                            isOn = musicEnabled,
                            onToggle = { on ->
                                musicEnabled = on
                                updateSettings()
                            }
                        )
                    }

                    Column {
                        Text("語言", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(6.dp))
                        // 這裡沿用 Setting_UI.kt 裡的 LanguageDropdown
                        LanguageDropdown(
                            selected = selectedLanguage,
                            onSelect = { lang ->
                                selectedLanguage = lang
                                updateSettings()
                            }
                        )
                    }

                    Button(
                        onClick = {
                            // 先停音樂，再登出
                            MusicPlayerManager.pauseMusic()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("登出", color = Color.White)
                    }

                }
            }
        }
    }
}


/** 下拉欄位維持不變 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    options: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            readOnly = true,
            value = value,
            onValueChange = {},
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = {
                        onValueChange(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}

