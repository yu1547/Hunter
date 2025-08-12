package com.ntou01157.hunter.temp

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.api.CloudinaryService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.HttpException


class ProfileViewModel : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

    private val userRepository = UserRepository()

    val editedUsername = MutableStateFlow("")
    val editedGender = MutableStateFlow("")
    val editedAge = MutableStateFlow("")

    fun fetchUserProfile(email: String) {
        viewModelScope.launch {
            try {
                val userData = RetrofitClient.apiService.getUserByEmail(email)
                _user.value = userData
                editedUsername.value = userData.username ?: ""
                editedGender.value = userData.gender ?: ""
                editedAge.value = userData.age ?: ""
            } catch (e: Exception) {
                println("載入使用者資料失敗: ${e.message}")
            }
        }
    }

    suspend fun uploadPhotoToCloudinary(uri: Uri, context: Context) {
        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
            val file = File.createTempFile("upload", ".jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()

            val requestFile = file.asRequestBody("image/*".toMediaTypeOrNull())
            val body = MultipartBody.Part.createFormData("file", file.name, requestFile)

            // 這裡記得替換成你 Cloudinary 的上傳 preset 名稱
            val preset = "Hunter"
            val presetBody = preset.toRequestBody("text/plain".toMediaTypeOrNull())

            val response = CloudinaryService.create().uploadImage(body, presetBody)
            val photoUrl = response.url
            println("上傳成功 URL: $photoUrl")

            val userId = user.value?.id ?: return
            updateUserPhotoUrl(userId, photoUrl)

        } catch (e: Exception) {
            println("Cloudinary 上傳失敗：${e.message}")
            if (e is HttpException) {
                println("錯誤 Response：${e.response()?.errorBody()?.string()}")
            }
        }
    }


    fun updateUserProfile(userId: String, username: String, gender: String, age: String) {
        viewModelScope.launch {
            try {
                val updated = mapOf(
                    "username" to username,
                    "gender" to gender,
                    "age" to age
                )
                val response = RetrofitClient.apiService.updateUser(userId, updated)
                _user.value = response
                println("使用者資料已更新")
            } catch (e: Exception) {
                println("更新使用者資料失敗: ${e.message}")
            }
        }
    }

    fun updateUserPhotoUrl(userId: String, photoUrl: String) {
        viewModelScope.launch {
            val success = userRepository.updatePhotoUrl(userId, photoUrl)
            if (success) {
                _user.value = _user.value?.copy(photoURL = photoUrl)
            }
        }
    }

    fun saveProfileUpdates() {
        viewModelScope.launch {
            _user.value?.let { currentUser ->
                val updatedUser = currentUser.copy(
                    username = editedUsername.value,
                    gender = editedGender.value,
                    age = editedAge.value
                )
                try {
                    val updated = RetrofitClient.apiService.updateUser(currentUser.id, updatedUser)
                    _user.value = updated
                } catch (e: Exception) {
                    println("更新使用者資料失敗: ${e.message}")
                }
            }
        }
    }
}
