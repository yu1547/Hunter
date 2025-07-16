package com.ntou01157.hunter

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import android.util.Log
import com.ntou01157.hunter.Backpack.model.UserBackpack
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor

// API 接口定義
interface ApiService {
    @GET("api/items/{id}")
    suspend fun getItem(@Path("id") id: String): Item

    @GET("api/users/{id}")
    suspend fun getUser(@Path("id") id: String): UserBackpack
}

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

// 從API獲取用戶背包和物品數據
suspend fun fetchUserItems(userId: String): SnapshotStateList<Item> = withContext(Dispatchers.IO) {
    try {
        Log.d("API", "正在獲取用戶數據，用戶ID: $userId")
        val userBackpack = RetrofitClient.apiService.getUser(userId)
        val items = mutableStateListOf<Item>()
        
        Log.d("API", "獲取到用戶數據，背包物品數量: ${userBackpack.backpackItems.size}")
        Log.d("API", "背包內容 (來自 UserBackpack 物件): ${userBackpack.backpackItems}")
        
        if (userBackpack.backpackItems.isEmpty()) {
            Log.w("API", "用戶背包為空或資料解析失敗")
            return@withContext items
        }
        
        for (backpackItem in userBackpack.backpackItems) {
            try {
                Log.d("API", "正在獲取物品詳情，物品ID: ${backpackItem.itemId}")
                val item = RetrofitClient.apiService.getItem(backpackItem.itemId)
                
                // 更新物品數量為背包中的數量
                item.quantity = backpackItem.quantity
                item.count = mutableStateOf(backpackItem.quantity)
                
                Log.d("API", "成功獲取物品: ${item.itemName}, 數量: ${item.count.value}")
                items.add(item)
            } catch (e: Exception) {
                Log.e("API", "獲取物品失敗: ${backpackItem.itemId}, 錯誤: ${e.message}", e)
                // 如果無法獲取特定物品，繼續獲取其他物品
            }
        }
        
        Log.d("API", "成功獲取所有物品，總數: ${items.size}")
        items
    } catch (e: Exception) {
        Log.e("API", "API請求失敗: ${e.message}", e)
        // 如果API請求失敗，返回空列表
        mutableStateListOf()
    }
}
