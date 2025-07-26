package com.ntou01157.hunter.api

import retrofit2.http.*
import retrofit2.Call

data class UserSettings(
    val music: Boolean,
    val notification: Boolean,
    val language: String
)

interface SettingsApi {
    @GET("api/settings/{id}")
    fun getSettings(@Path("id") userId: String): Call<UserSettings>

    @PUT("api/settings/{id}")
    fun updateSettings(@Path("id") userId: String, @Body settings: UserSettings): Call<UserSettings>
}
