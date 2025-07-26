package com.ntou01157.hunter.data

import com.ntou01157.hunter.model.model_api.Settings
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

interface SettingsApi {
    @GET("settings/{userId}")
    suspend fun getSettings(@Path("userId") userId: String): Response<Settings>

    @PUT("settings/{userId}")
    suspend fun updateSettings(
        @Path("userId") userId: String,
        @Body settings: Settings
    ): Response<Settings>
}

object SettingsRepository {
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://your-api-url.com/") // ⛳️改成你自己的 API URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(SettingsApi::class.java)

    suspend fun fetchSettings(userId: String): Settings? {
        val response = api.getSettings(userId)
        return if (response.isSuccessful) response.body() else null
    }

    suspend fun updateSettings(userId: String, settings: Settings): Boolean {
        val response = api.updateSettings(userId, settings)
        return response.isSuccessful
    }
}
