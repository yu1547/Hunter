package com.ntou01157.hunter.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*


data class CloudinaryResponse(
    val url: String
)

interface CloudinaryService {
    @Multipart
    @POST("image/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody
    ): CloudinaryResponse

    companion object {
        fun create(): CloudinaryService {
            return Retrofit.Builder()
                .baseUrl("https://api.cloudinary.com/v1_1/djlab6wed/") // 用你的 cloud name
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CloudinaryService::class.java)
        }
    }
}
