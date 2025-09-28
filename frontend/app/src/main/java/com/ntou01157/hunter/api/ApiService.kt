package com.ntou01157.hunter.api

import com.google.gson.annotations.SerializedName
import com.ntou01157.hunter.models.model_api.EventModel
import com.ntou01157.hunter.models.model_api.EventResponse
import com.ntou01157.hunter.models.model_api.Item
import com.ntou01157.hunter.models.model_api.RankResponse
import com.ntou01157.hunter.models.model_api.User
import com.ntou01157.hunter.models.model_api.Task
import com.ntou01157.hunter.models.model_api.UserItem
import com.ntou01157.hunter.models.model_api.Settings
import com.ntou01157.hunter.models.model_api.RankCreateRequest
import com.ntou01157.hunter.models.PhotoUrlBody

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.PATCH

// 為了 BugHuntUI
data class CompleteBugHuntRequestBody(val userId: String, val solution: String)

data class CompleteBugHuntResponse(val success: Boolean, val message: String)

// 為了 TreasureBoxUI
data class OpenTreasureBoxRequest(val userId: String, val keyType: String)

data class OpenTreasureBoxResponse(
        val success: Boolean,
        val message: String,
        val drops: List<String>
)

// 為了 Merchant_UI
data class TradeRequest(val userId: String, val tradeType: String)

data class TradeResponse(val success: Boolean, val message: String)

// 為了 AncientTree_UI
data class BlessTreeRequest(val userId: String, val itemToOffer: String)
data class BlessTreeResponse(val success: Boolean, val message: String)

// 為了 SlimeAttack_UI
data class CompleteSlimeAttackRequest(val userId: String, val totalDamage: Int)

data class CompleteSlimeAttackResponse(
        val success: Boolean,
        val message: String,
        val rewards: List<String>
)

// 為了 StonePileUI
data class GetStonePileStatusResponse(val hasTriggeredToday: Boolean)

data class TriggerStonePileRequest(val userId: String)

data class TriggerStonePileResponse(val success: Boolean, val message: String)

data class CheckSpotMissionResponse(
        val user: User?, // Make this field nullable
        val message: String,
        val isMissionCompleted: Boolean // Indicate if the mission is now fully completed
)
// 通用成功回傳
data class SuccessResponse(
        val success: Boolean,
        val message: String,
        val cooldownUntil: Long? = null
)

// API 接口定義
interface ApiService {
        // --- Item endpoints ---
        @GET("api/items/{id}") suspend fun getItem(@Path("id") id: String): Item

        @GET("api/users/{id}") suspend fun getUser(@Path("id") id: String): User

        @POST("api/users/{id}/craft")
        suspend fun craftItem(@Path("id") id: String, @Body body: CraftRequestBody): User

        @GET("api/users/email/{email}")
        suspend fun getUserByEmail(@Path("email") email: String): User

        @PUT("api/users/{id}")
        suspend fun updateUser(@Path("id") id: String, @Body updatedUser: User): User

        @PUT("api/users/{id}")
        suspend fun updateUser(@Path("id") id: String, @Body updatedData: Map<String, String>): User

        @PATCH("/api/users/{id}/photo")
        suspend fun updatePhotoUrl(
                @Path("id") userId: String,
                @Body body: PhotoUrlBody
        ): Response<Unit>

        
        // --- Task endpoints ---
        @GET("api/tasks/{id}")
        suspend fun getTask(@Path("id") id: String): Task

        @GET("api/rank/{userId}") // Changed "api/ranks" to "api/rank" for consistency with backend routes
        suspend fun getRank(@Path("userId") userId: String): Response<RankResponse>

        // ✅ 取得排行榜（帶目前使用者 userId）
        @GET("api/ranks/{userId}")
        suspend fun getRankByUserId(@Path("userId") userId: String): RankResponse

        // ✅ 建立排名資料（當 userRank 不存在時用）
        @POST("api/ranks")
        suspend fun createRank(@Body body: RankCreateRequest): Response<Unit>
        
        
        // --- Mission endpoints ---
        @POST("api/users/{userId}/missions/refresh")
        suspend fun refreshAllMissions(@Path("userId") userId: String): User

        @POST("api/users/{userId}/missions/{taskId}/accept")
        suspend fun acceptTask(@Path("userId") userId: String, @Path("taskId") taskId: String): User

        @POST("api/users/{userId}/missions/{taskId}/decline")
        suspend fun declineTask(
                @Path("userId") userId: String,
                @Path("taskId") taskId: String
        ): User

        @POST("api/users/{userId}/missions/{taskId}/complete")
        suspend fun completeTask(
                @Path("userId") userId: String,
                @Path("taskId") taskId: String
        ): User

        @POST("api/users/{userId}/missions/{taskId}/claim")
        suspend fun claimReward(
                @Path("userId") userId: String,
                @Path("taskId") taskId: String
        ): UserResponse

        // 檢查補給站任務點
        @PUT("api/users/{userId}/missions/check-spot/{spotId}")
        suspend fun checkSpotMission(
                @Path("userId") userId: String,
                @Path("spotId") spotId: String
        ): CheckSpotMissionResponse

        // 取得事件詳細資訊
        @GET("api/events/{eventId}")
        suspend fun getEventById(@Path("eventId") eventId: String): EventModel

        @POST("events/trigger/{eventId}")
        suspend fun triggerEvent(
                @Path("eventId") eventId: String,
                @Body request: TriggerEventRequest
        ): EventResponse // 修正: 回傳型別應該是 EventResponse

        @POST("api/events/complete/{eventId}")
        suspend fun completeEvent(
                @Path("eventId") eventId: String,
                @Body request: CompleteEventRequest
        ): EventResponse

        // 日常事件測試路由
        @POST("api/events/trade") suspend fun trade(@Body request: TradeRequest): TradeResponse

        @GET("api/events/stone-pile-status/{userId}")
        suspend fun getStonePileStatus(@Path("userId") userId: String): GetStonePileStatusResponse

        @POST("api/events/trigger-stone-pile")
        suspend fun triggerStonePile(
                @Body request: TriggerStonePileRequest
        ): TriggerStonePileResponse

        // 任務測試路由
        @POST("api/tasks/complete-bug-hunt")
        suspend fun completeBugHunt(
                @Body requestBody: CompleteBugHuntRequestBody
        ): CompleteBugHuntResponse

        @POST("api/tasks/open-treasure-box")
        suspend fun openTreasureBox(@Body request: OpenTreasureBoxRequest): OpenTreasureBoxResponse

        @POST("api/tasks/bless-tree")
        suspend fun blessTree(@Body request: BlessTreeRequest): BlessTreeResponse

        @POST("api/tasks/complete-slime-attack")
        suspend fun completeSlimeAttack(
                @Body request: CompleteSlimeAttackRequest
        ): CompleteSlimeAttackResponse

        // 您可能還需要一個 API 來獲取使用者背包物品
        @GET("api/users/{userId}/items")
        suspend fun fetchUserItems(@Path("userId") userId: String): List<UserItem>

        @POST("api/missions/llm/{userId}")
        suspend fun createLLMMission(
                @Path("userId") userId: String,
                @Body body: CreateLLMMissionRequest
        ): User

        // --- Settings endpoints ---
        @GET("api/settings/{id}") suspend fun fetchSettings(@Path("id") id: String): Settings

        @PUT("api/settings/{id}")
        suspend fun updateSettings(@Path("id") id: String, @Body settings: Settings)

        // 指派每日任務給使用者
        @POST("api/users/{userId}/missions/assign-daily")
        suspend fun assignDailyMissions(@Path("userId") userId: String): SuccessResponse

        // --- Chat endpoints ---
        @POST("api/chat/{userId}")
        suspend fun chatWithLLM(@Path("userId") userId: String, @Body body: ChatRequest): ChatResponse

        // 刪除對話紀錄
        @DELETE("api/chat/{userId}")
        suspend fun deleteChatHistory(@Path("userId") userId: String)
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

// 聊天對話 API
data class ChatRequest(val message: String, val history: List<ChatHistoryItem>)

data class ChatHistoryItem(val role: String, val content: String, val timestamp: String)

data class ChatResponse(val reply: String)

// 創建 Retrofit 實例
object RetrofitClient {
        private const val BASE_URL = "http://10.0.2.2:4000/" // 10.0.2.2 是 Android 模擬器訪問主機的特殊 IP

        val apiService: ApiService by lazy {
                // 增加日誌攔截器以查看網路請求和回應的詳細資訊
                val logging =
                        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
                val client = OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(6000, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(6000, java.util.concurrent.TimeUnit.SECONDS)
                .writeTimeout(6000, java.util.concurrent.TimeUnit.SECONDS)
                .build()

                Retrofit.Builder()
                        .baseUrl(BASE_URL)
                        .client(client) // 使用帶有日誌攔截器的 client
                        .addConverterFactory(GsonConverterFactory.create())
                        .build()
                        .create(ApiService::class.java)
        }
}
