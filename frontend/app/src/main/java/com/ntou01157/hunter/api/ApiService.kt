package com.ntou01157.hunter.api

import com.google.gson.annotations.SerializedName
import com.ntou01157.hunter.models.model_api.Item
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.models.model_api.User
import com.ntou01157.hunter.models.model_api.Task
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.models.model_api.EventResponse
import com.ntou01157.hunter.models.model_api.UserItem
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path


// 為了 BugHuntUI
data class CompleteBugHuntRequestBody(val userId: String, val solution: String)
data class CompleteBugHuntResponse(val success: Boolean, val message: String)

// 為了 TreasureBoxUI
data class OpenTreasureBoxRequest(val userId: String, val keyType: String)
data class OpenTreasureBoxResponse(val success: Boolean, val message: String, val drops: List<String>)

// 為了 Merchant_UI
data class TradeRequest(val userId: String, val tradeType: String)
data class TradeResponse(val success: Boolean, val message: String)

// 為了 AncientTree_UI
data class BlessTreeRequest(val userId: String, val itemToOffer: String)
data class BlessTreeResponse(val success: Boolean, val message: String)

// 為了 SlimeAttack_UI
data class CompleteSlimeAttackRequest(val userId: String, val totalDamage: Int)
data class CompleteSlimeAttackResponse(val success: Boolean, val message: String, val rewards: List<String>)

// 為了 StonePileUI
data class GetStonePileStatusResponse(val hasTriggeredToday: Boolean)
data class TriggerStonePileRequest(val userId: String)
data class TriggerStonePileResponse(val success: Boolean, val message: String)



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
    @GET("events/all")
    suspend fun getEvents(): List<EventModel>

    @POST("events/trigger/{eventId}")
    suspend fun triggerEvent(
        @Path("eventId") eventId: String,
        @Body request: TriggerEventRequest
    ): EventResponse // 修正: 回傳型別應該是 EventResponse

    @POST("events/complete/{eventId}")
    suspend fun completeEvent(
        @Path("eventId") eventId: String,
        @Body request: CompleteEventRequest
    ): EventResponse

    // 修正了以下路由
    @POST("api/events/complete-bug-hunt")
    suspend fun completeBugHunt(@Body requestBody: CompleteBugHuntRequestBody): CompleteBugHuntResponse

    @POST("api/events/open-treasure-box")
    suspend fun openTreasureBox(@Body request: OpenTreasureBoxRequest): OpenTreasureBoxResponse

    @POST("api/events/trade")
    suspend fun trade(@Body request: TradeRequest): TradeResponse

    @POST("api/events/bless-tree")
    suspend fun blessTree(@Body request: BlessTreeRequest): BlessTreeResponse

    @POST("api/events/complete-slime-attack")
    suspend fun completeSlimeAttack(@Body request: CompleteSlimeAttackRequest): CompleteSlimeAttackResponse

    @GET("api/events/stone-pile-status/{userId}")
    suspend fun getStonePileStatus(@Path("userId") userId: String): GetStonePileStatusResponse

    @POST("api/events/trigger-stone-pile")
    suspend fun triggerStonePile(@Body request: TriggerStonePileRequest): TriggerStonePileResponse

    // 您可能還需要一個 API 來獲取使用者背包物品
    @GET("api/users/{userId}/items")
    suspend fun fetchUserItems(@Path("userId") userId: String): List<UserItem>
}

// 請求 Body 的資料類別
data class CraftRequestBody(val itemId: String)

// 處理後端回傳 { user, message } 格式的資料類別
data class UserResponse(
    @SerializedName("user") val user: User,
    @SerializedName("message") val message: String?
)

// 定義請求的資料結構
data class TriggerEventRequest(
    val userId: String,
    val userLatitude: Double,
    val userLongitude: Double
)

data class CompleteEventRequest(
    val userId: String,
    val selectedOption: String?,
    val gameResult: Int?
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