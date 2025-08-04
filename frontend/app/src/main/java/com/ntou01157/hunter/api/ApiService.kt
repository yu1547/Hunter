package com.ntou01157.hunter.api

import com.google.gson.annotations.SerializedName
import com.ntou01157.hunter.models.model_api.Item
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.models.model_api.User
import com.ntou01157.hunter.models.model_api.Task
import com.ntou01157.hunter.models.model_api.EventModel
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
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

    // --- Task endpoints ---
    @GET("api/tasks/{id}")
    suspend fun getTask(@Path("id") id: String): Task

    @GET("api/rank/{userId}") // Changed "api/ranks" to "api/rank" for consistency with backend routes
    suspend fun getRank(@Path("userId") userId: String): Response<RankResponse>

    // --- Mission endpoints ---
    @POST("api/users/{userId}/missions/refresh")
    suspend fun refreshMissions(@Path("userId") userId: String): User

    @POST("api/users/{userId}/missions/{taskId}/accept")
    suspend fun acceptTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/decline")
    suspend fun declineTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/complete")
    suspend fun completeTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

    @POST("api/users/{userId}/missions/{taskId}/claim")
    suspend fun claimReward(@Path("userId") userId: String, @Path("taskId") taskId: String): UserResponse

    @GET("api/events/daily")
    suspend fun getDailyEvents(): Response<List<EventModel>>

    @GET("api/events/permanent")
    suspend fun getPermanentEvents(): Response<List<EventModel>>

    @POST("api/events/merchant/exchange")
    suspend fun postMerchantExchange(@Body exchangeRequest: ExchangeRequest): Response<GeneralResponse>

    @POST("api/events/slime/attack")
    suspend fun postSlimeAttack(@Body attackRequest: AttackRequest): Response<SlimeAttackResponse>

    @POST("api/events/treasurebox/open")
    suspend fun postOpenTreasureBox(@Body openRequest: OpenTreasureBoxRequest): Response<EventRewardResponse>

    @POST("api/events/ancienttree/bless")
    suspend fun postAncientTreeBlessing(@Body blessRequest: BlessRequest): Response<GeneralResponse>
}

// 請求 Body 的資料類別
data class CraftRequestBody(val itemId: String)

// 處理後端回傳 { user, message } 格式的資料類別
data class UserResponse(
    @SerializedName("user") val user: User,
    @SerializedName("message") val message: String?
)

// --- 新增事件相關的請求與回應資料類別 ---
data class ExchangeRequest(val userId: String, val option: String)
data class AttackRequest(val userId: String, val totalDamage: Int, val usedTorch: Boolean)
data class OpenTreasureBoxRequest(val userId: String, val keyId: String)
data class BlessRequest(val userId: String, val option: String)

// 處理後端回傳 { success, message, user } 的通用回應
data class GeneralResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User
)

// 處理後端回傳 { success, message, user, rewards } 的史萊姆回應
data class SlimeAttackResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User,
    val rewards: List<EventReward>
)

// 處理後端回傳寶箱開啟的回應
data class EventRewardResponse(
    val success: Boolean,
    val message: String,
    @SerializedName("user") val user: User,
    val rewards: EventRewards
)

data class EventRewards(
    val points: Int,
    val items: List<EventReward>
)

data class EventReward(
    val itemId: String,
    val quantity: Int
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
