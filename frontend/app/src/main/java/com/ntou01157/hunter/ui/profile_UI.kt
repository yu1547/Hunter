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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Logout
import android.content.Context
import androidx.lifecycle.viewModelScope
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
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.ntou01157.hunter.R


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
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    var isEditing by remember { mutableStateOf(false) }

    var editedUsername by remember { mutableStateOf("") }
    var editedGender by remember { mutableStateOf("") }
    var editedAge by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var userId by remember { mutableStateOf<String?>(null) }
    var musicEnabled by remember { mutableStateOf(false) }
    var selectedLanguage by remember { mutableStateOf("zh-TW") }

    val snackbarHostState = remember { SnackbarHostState() }
    var isAvatarUploading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { picked ->
        if (picked != null) {
            selectedImageUri = picked
            coroutineScope.launch {
                isAvatarUploading = true
                val r = profileViewModel.uploadPhotoToCloudinary(picked, context)
                r.fold(
                    onSuccess = {
                        isAvatarUploading = false
                        snackbarHostState.showSnackbar("頭像已更新")
                    },
                    onFailure = { e ->
                        isAvatarUploading = false
                        snackbarHostState.showSnackbar(e.message ?: "上傳失敗")
                    }
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
            val s = apiUser.settings
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 背景鋪滿
            Image(
                painter = painterResource(id = R.drawable.person),
                contentDescription = "背景",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Profile 區塊 → 螢幕正中央
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 25.dp)
            ) {
                // 頭像
                if(!isEditing) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .clip(CircleShape)
                            .background(Color.LightGray)
                            .clickable(enabled = !isAvatarUploading) { launcher.launch("image/*") },
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

                        if (isAvatarUploading) {
                            Box(
                                Modifier
                                    .matchParentSize()
                                    .background(Color.Black.copy(alpha = 0.25f))
                            )
                            CircularProgressIndicator()
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // 使用者資訊 / 編輯模式
                val u = userState
                if (u == null) {
                    Text("載入中...", style = MaterialTheme.typography.bodyLarge)
                } else {
                    if (isEditing) {
                        OutlinedTextField(
                            value = editedUsername,
                            onValueChange = { v -> editedUsername = v },
                            label = { Text("暱稱") },
                            modifier = Modifier
                                .width(screenWidth * 0.65f)   // ✅ 寬度 50%
                                .height(screenHeight * 0.1f) // ✅ 高度 6%
                                .padding(vertical = 8.dp)
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

                    } else {
                        Column(horizontalAlignment = Alignment.Start) {
                            Text("暱稱：${u.username}", style = MaterialTheme.typography.bodyLarge)
                            Text("性別：${u.gender}", style = MaterialTheme.typography.bodyLarge)
                            Text("年齡：${u.age}", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }

            // 左上角 Home icon
            IconButton(
                onClick = { navController.navigate("main") },
                modifier = Modifier.padding(top = 25.dp, start = 10.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.home_icon),
                    contentDescription = "回首頁",
                    modifier = Modifier.size(48.dp)
                )
            }

            IconButton(
                onClick = {
                    if (isEditing) {
                        val u = userState ?: return@IconButton
                        val id = u.id ?: return@IconButton
                        profileViewModel.updateUserProfile(
                            userId = id,
                            username = editedUsername,
                            gender = editedGender,
                            age = editedAge
                        )
                        isEditing = false
                    } else {
                        isEditing = true
                    }
                },
                modifier = Modifier
                    .fillMaxHeight(0.7f)
                    .offset(x = screenWidth * 11 / 14)
            ) {
                Icon(
                    imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Settings,
                    contentDescription = if (isEditing) "儲存" else "設定",
                    modifier = Modifier.size(40.dp),
                    tint = Color.Black
                )
            }

            // 全螢幕 Loading 遮罩
            if (isAvatarUploading) {
                val blocker = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.40f))
                        .clickable(
                            indication = null,
                            interactionSource = blocker
                        ) { },
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            val configuration = LocalConfiguration.current
            val screenHeight = configuration.screenHeightDp.dp

            Surface(
                modifier = Modifier
                    .width(screenWidth * 0.7f) // ✅ 卡片寬度 70%
                    .align(Alignment.BottomCenter)
                    .offset(y = -screenHeight * 0.2f),
                shape = RoundedCornerShape(16.dp),
                color = Color.Transparent,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // 音樂 + Icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.MusicNote,
                            contentDescription = "音樂",
                            tint = Color.Black,
                            modifier = Modifier.size(24.dp)
                        )
                        Text("音樂", style = MaterialTheme.typography.bodyLarge)
                    }

                    // 開關
                    CustomSwitch(
                        isOn = musicEnabled,
                        onToggle = { on ->
                            musicEnabled = on
                            updateSettings()
                        }
                    )

                    Spacer(Modifier.width(35.dp))

                    // 登出按鈕 (黑色，縮小)
                    Button(
                        onClick = {
                            MusicPlayerManager.pauseMusic()
                            onLogout()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Black),
                        shape = RoundedCornerShape(50),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp), // ✅ 縮小
                        modifier = Modifier.wrapContentWidth() // ✅ 不要填滿
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "登出",
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("登出", color = Color.White, fontSize = 14.sp)
                    }
                }
            }

        }
    }
}



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

    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val screenHeight = configuration.screenHeightDp.dp

    // ✅ 用 Column 置中
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = !expanded }
        ) {
            OutlinedTextField(
                readOnly = true,
                value = value,
                onValueChange = {},
                label = { Text(label) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor()
                    .width(screenWidth * 0.65f)   // ✅ 直接限制寬度
                    .height(screenHeight * 0.08f) // ✅ 限制高度
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
}
