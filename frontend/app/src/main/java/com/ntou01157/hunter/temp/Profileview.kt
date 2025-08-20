package com.ntou01157.hunter.temp

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.api.CloudinaryService
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

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
                Log.e("Profile", "載入使用者資料失敗: ${e.message}", e)
            }
        }
    }

    // ProfileViewModel.kt
    suspend fun uploadPhotoToCloudinary(uri: Uri, context: Context): Result<String> {
        val service = CloudinaryService.create()
        val resolver = context.contentResolver

        val mimeStr = resolver.getType(uri) ?: "image/jpeg"
        val mime = mimeStr.toMediaType()
        val suffix = if (mimeStr.contains("png", ignoreCase = true)) ".png" else ".jpg"
        val tmp = File.createTempFile("avatar_${System.currentTimeMillis()}_", suffix, context.cacheDir)

        return try {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(tmp).use { out -> input.copyTo(out) }
            } ?: return Result.failure(IOException("Cannot open input stream: $uri"))

            val fileBody = tmp.asRequestBody(mime)
            val part = MultipartBody.Part.createFormData("file", tmp.name, fileBody)

            val preset = "Hunter".toRequestBody("text/plain".toMediaType())
            val publicId = "avatar_${System.currentTimeMillis()}".toRequestBody("text/plain".toMediaType())
            val folder = "avatars".toRequestBody("text/plain".toMediaType())

            val resp = service.uploadImage(
                file = part,
                uploadPreset = preset,
                publicId = publicId,
                folder = folder
            )
            if (!resp.isSuccessful) {
                val err = resp.errorBody()?.string().orEmpty()
                Log.e("Cloudinary", "HTTP ${resp.code()} - $err")
                return Result.failure(IOException("HTTP ${resp.code()} - $err"))
            }

            val url = resp.body()?.secure_url.orEmpty()
            if (url.isBlank()) return Result.failure(IOException("Empty secure_url"))

            // ✅ 寫回資料庫（並更新 StateFlow，畫面會即時刷新）
            _user.value?.id?.let { uid ->
                updateUserPhotoUrl(uid, url)
            }

            Result.success(url)
        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            tmp.delete()
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
                Log.d("Profile", "使用者資料已更新")
            } catch (e: Exception) {
                Log.e("Profile", "更新使用者資料失敗: ${e.message}", e)
            }
        }
    }

    fun updateUserPhotoUrl(userId: String, photoUrl: String) {
        viewModelScope.launch {
            val ok = userRepository.updatePhotoUrl(userId, photoUrl)
            if (ok) {
                val https = photoUrl.replaceFirst("http://", "https://")
                _user.value = _user.value?.let { u ->
                    u.copy(
                        photoURL = https,
                        missions = u.missions ?: emptyList(),
                        backpackItems = u.backpackItems ?: emptyList(),
                        //settings = u.settings ?: emptyList(),
                    )
                }
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
                    Log.e("Profile", "更新使用者資料失敗: ${e.message}", e)
                }
            }
        }
    }
}
