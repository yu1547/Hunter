package com.ntou01157.hunter.temp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.models.model_api.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import com.ntou01157.hunter.temp.UserRepository

class ProfileViewModel : ViewModel() {
    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user

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
            val success = updatePhotoUrl(userId, photoUrl)
            if (success) {
                // 更新本地 state（可選）
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

