package com.ntou01157.hunter.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.temp.ProfileViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(profileViewModel: ProfileViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val user by profileViewModel.user.collectAsState()
    var isEditing by remember { mutableStateOf(false) }

    var editedUsername by remember { mutableStateOf("") }
    var editedGender by remember { mutableStateOf("") }
    var editedAge by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var lastUploadUrl by remember { mutableStateOf<String?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            coroutineScope.launch {
                val r = profileViewModel.uploadPhotoToCloudinary(it, context)
                r.fold(
                    onSuccess = {
                        snackbarHostState.showSnackbar("頭像已更新")
                        // _user.photoURL 已在 ViewModel 內更新，這裡不用再做事
                    },
                    onFailure = { e ->
                        snackbarHostState.showSnackbar(e.message ?: "上傳失敗")
                    }
                )
            }
        }
    }


    // 初次載入使用者
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.email?.let { profileViewModel.fetchUserProfile(it) }
    }
    // 把資料帶入編輯欄位
    LaunchedEffect(user) {
        user?.let {
            editedUsername = it.username ?: ""
            editedGender = it.gender ?: ""
            editedAge = it.age ?: ""
        }
    }

    Scaffold(

        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .padding(innerPadding),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = { isEditing = !isEditing }) {
                    Text(if (isEditing) "取消" else "編輯")
                }
            }

            Spacer(Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray)
                    .clickable { launcher.launch("image/*") },
                contentAlignment = Alignment.Center
            ) {
                val photoUrl = user?.photoURL.orEmpty()
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

            // 上傳成功的圖片連結（可長按選取複製）
            lastUploadUrl?.let { url ->
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "圖片連結：$url",
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall
                )
            }

            Spacer(Modifier.height(24.dp))

            if (user == null) {
                Text("載入中...", style = MaterialTheme.typography.bodyLarge)
            } else {
                if (isEditing) {
                    // 暱稱
                    OutlinedTextField(
                        value = editedUsername,
                        onValueChange = { editedUsername = it },
                        label = { Text("暱稱") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    // 性別：下拉
                    DropdownField(
                        label = "性別",
                        options = listOf("男", "女", "不透露"),
                        value = editedGender.ifBlank { "不透露" },
                        onValueChange = { editedGender = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    // 年齡：1~50 下拉
                    DropdownField(
                        label = "年齡",
                        options = (1..50).map { it.toString() },
                        value = editedAge.ifBlank { "1" },
                        onValueChange = { editedAge = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                    )

                    Button(
                        onClick = {
                            user?.let {
                                profileViewModel.updateUserProfile(
                                    userId = it.id ?: return@let,
                                    username = editedUsername,
                                    gender = editedGender,
                                    age = editedAge
                                )
                            }
                            isEditing = false
                        },
                        modifier = Modifier.padding(top = 16.dp)
                    ) { Text("儲存變更") }
                } else {
                    Text("暱稱：${user?.username}", style = MaterialTheme.typography.bodyLarge)
                    Text("性別：${user?.gender}", style = MaterialTheme.typography.bodyLarge)
                    Text("年齡：${user?.age}", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** 通用下拉欄位（Material 3 ExposedDropdown） */
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
            modifier = Modifier.menuAnchor().fillMaxWidth()
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
