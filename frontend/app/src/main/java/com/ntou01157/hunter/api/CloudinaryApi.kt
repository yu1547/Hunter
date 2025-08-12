package com.ntou01157.hunter.api

import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response
import java.util.concurrent.TimeUnit

// ✅ 回應欄位需包含 secure_url
data class CloudinaryUploadResponse(
    val asset_id: String? = null,
    val public_id: String? = null,
    val version: Long? = null,
    val secure_url: String? = null
)

interface CloudinaryService {
    @Multipart
    @POST("image/upload")
    suspend fun uploadImage(
        @Part file: MultipartBody.Part,
        @Part("upload_preset") uploadPreset: RequestBody,
        @Part("public_id") publicId: RequestBody? = null,
        @Part("folder") folder: RequestBody? = null
    ): Response<CloudinaryUploadResponse>


    companion object {
        fun create(): CloudinaryService {
            val client = OkHttpClient.Builder()
                .callTimeout(60, TimeUnit.SECONDS)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build()

            return Retrofit.Builder()
                // ⚠️ 這裡要以你的 cloud_name 結尾：/v1_1/<cloud_name>/
                .baseUrl("https://api.cloudinary.com/v1_1/djlab6wed/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(CloudinaryService::class.java)
        }
    }
}
