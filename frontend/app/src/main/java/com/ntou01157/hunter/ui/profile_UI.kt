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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.ntou01157.hunter.api.CloudinaryService
import com.ntou01157.hunter.temp.ProfileViewModel
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody
import java.io.File
import java.io.FileOutputStream

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

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            coroutineScope.launch {
                profileViewModel.uploadPhotoToCloudinary(uri, context)
            }

        }
    }


    LaunchedEffect(Unit) {
        val email = FirebaseAuth.getInstance().currentUser?.email
        email?.let { profileViewModel.fetchUserProfile(it) }
    }

    LaunchedEffect(user) {
        user?.let {
            editedUsername = it.username ?: ""
            editedGender = it.gender ?: ""
            editedAge = it.age ?: ""
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = { isEditing = !isEditing }) {
                Text(if (isEditing) "取消" else "編輯")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(Color.LightGray)
                .clickable { launcher.launch("image/*") },
            contentAlignment = Alignment.Center
        ) {
            val photoUrl = user?.photoURL ?: ""

            when {
                selectedImageUri != null -> {
                    Image(
                        painter = rememberAsyncImagePainter(selectedImageUri),
                        contentDescription = "選擇的頭像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                photoUrl.isNotBlank() -> {
                    Image(
                        painter = rememberAsyncImagePainter(photoUrl),
                        contentDescription = "已設定的頭像",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (user == null) {
            Text("載入中...", style = MaterialTheme.typography.bodyLarge)
        } else {
            if (isEditing) {
                OutlinedTextField(
                    value = editedUsername,
                    onValueChange = { editedUsername = it },
                    label = { Text("暱稱") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = editedGender,
                    onValueChange = { editedGender = it },
                    label = { Text("性別") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
                OutlinedTextField(
                    value = editedAge,
                    onValueChange = { editedAge = it },
                    label = { Text("年齡") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
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
                ) {
                    Text("儲存變更")
                }
            } else {
                Text("暱稱：${user?.username}", style = MaterialTheme.typography.bodyLarge)
                Text("性別：${user?.gender}", style = MaterialTheme.typography.bodyLarge)
                Text("年齡：${user?.age}", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
