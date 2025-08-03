package com.ntou01157.hunter.api

import com.google.gson.annotations.SerializedName
import com.ntou01157.hunter.models.model_api.Item
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.models.model_api.User
import com.ntou01157.hunter.models.model_api.Task
import com.ntou01157.hunter.model.model_api.Settings

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.PUT

// API 接口定義
interface ApiService {
    // --- Item endpoints ---
    @GET("api/items/{id}")
    suspend fun getItem(@Path("id") id: String): Item

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): User

    @POST("api/users/{id}/craft")
    suspend fun craftItem(@Path("id") id: String, @Body body: CraftRequestBody): User

  
    // --- Task endpoints ---
    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") id: String): Task

    @GET("api/rank/{userId}") // Changed "api/ranks" to "api/rank" for consistency with backend routes
    suspend fun getRank(@Path("userId") userId: String): Response<RankResponse>
  
  
    // --- Mission endpoints ---
    @POST("api/users/{userId}/missions/refresh")
    suspend fun refreshAllMissions(@Path("userId") userId: String): User

    @POST("api/users/{userId}/missions/{taskId}/accept")
    suspend fun acceptTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/decline")
    suspend fun declineTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/complete")
    suspend fun completeTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/claim")
    suspend fun claimReward(@Path("userId") userId: String, @Path("taskId") taskId: String): UserResponse

    @POST("api/missions/llm/{userId}")
    suspend fun createLLMMission(@Path("userId") userId: String, @Body body: CreateLLMMissionRequest): User

  
    // --- Settings endpoints ---
    @GET("api/settings/{id}")
    suspend fun fetchSettings(@Path("id") id: String): Settings

    @PUT("api/settings/{id}")
    suspend fun updateSettings(@Path("id") id: String, @Body settings: Settings)
}

// 請求 Body 的資料類別
data class CraftRequestBody(val itemId: String)

data class Location(val latitude: Double, val longitude: Double)
data class CreateLLMMissionRequest(val userLocation: Location)

// 處理後端回傳 { user, message } 格式的資料類別
data class UserResponse(
    @SerializedName("user") val user: User,
    @SerializedName("message") val message: String?
)

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
