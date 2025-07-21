package com.ntou01157.hunter.api

import com.ntou01157.hunter.model.model_api.Item
import com.ntou01157.hunter.model.model_api.User
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

// API 接口定義
interface ApiService {
    @GET("api/items/{id}")
    suspend fun getItem(@Path("id") id: String): Item

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): User

    @POST("api/users/{id}/craft")
    suspend fun craftItem(@Path("id") id: String, @Body body: CraftRequestBody): User
}

// 請求 Body 的資料類別
data class CraftRequestBody(val itemId: String)

// 創建 Retrofit 實例
object RetrofitClient {
    private const val BASE_URL = "http://10.0.2.2:3000/"  // 10.0.2.2 是 Android 模擬器訪問主機的特殊 IP

    val apiService: ApiService by lazy {
        // 增加日誌攔截器以查看網路請求和回應的詳細資訊
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client) // 使用帶有日誌攔截器的 client
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
