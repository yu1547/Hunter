package com.ntou01157.hunter.temp

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class ProfileViewModel : ViewModel() {
    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _gender = MutableStateFlow("")
    val gender: StateFlow<String> = _gender

    private val _age = MutableStateFlow("")
    val age: StateFlow<String> = _age

    private val client = OkHttpClient()

    fun fetchUserProfile(email: String) {
        viewModelScope.launch {
            try {
                val request = Request.Builder()
                    .url("http://10.0.2.2:3000/api/users/$email")
                    .build()

                val response = client.newCall(request).execute()

                if (response.isSuccessful) {
                    val json = JSONObject(response.body?.string() ?: "")
                    _username.value = json.optString("username", "")
                    _gender.value = json.optString("gender", "")
                    _age.value = json.optString("age", "")
                } else {
                    println("錯誤代碼: ${response.code}")
                }
            } catch (e: Exception) {
                println("錯誤: ${e.message}")
            }
        }
    }
}
