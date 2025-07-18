package com.ntou01157.hunter.Backpack.data

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.ntou01157.hunter.Backpack.api.CraftRequestBody
import com.ntou01157.hunter.Backpack.api.RetrofitClient
import com.ntou01157.hunter.Backpack.model.UserItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// 從API獲取用戶背包和物品數據
suspend fun fetchUserItems(userId: String): SnapshotStateList<UserItem> = withContext(Dispatchers.IO) {
    try {
        Log.d("API", "正在獲取用戶數據，用戶ID: $userId")
        val userBackpack = RetrofitClient.apiService.getUser(userId)
        val userItems = mutableStateListOf<UserItem>()
        
        Log.d("API", "獲取到用戶數據，背包物品數量: ${userBackpack.backpackItems.size}")
        Log.d("API", "背包內容 (來自 UserBackpack 物件): ${userBackpack.backpackItems}")
        
        if (userBackpack.backpackItems.isEmpty()) {
            Log.w("API", "用戶背包為空或資料解析失敗")
            return@withContext userItems
        }
        
        for (backpackItem in userBackpack.backpackItems) {
            try {
                Log.d("API", "正在獲取物品詳情，物品ID: ${backpackItem.itemId}")
                val itemDetails = RetrofitClient.apiService.getItem(backpackItem.itemId)
                
                val userItem = UserItem(item = itemDetails, quantity = backpackItem.quantity)
                
                Log.d("API", "成功獲取物品: ${userItem.item.itemName}, 數量: ${userItem.count.value}")
                userItems.add(userItem)
            } catch (e: Exception) {
                Log.e("API", "獲取物品失敗: ${backpackItem.itemId}, 錯誤: ${e.message}", e)
                // 如果無法獲取特定物品，繼續獲取其他物品
            }
        }
        
        Log.d("API", "成功獲取所有物品，總數: ${userItems.size}")
        userItems
    } catch (e: Exception) {
        Log.e("API", "API請求失敗: ${e.message}", e)
        // 如果API請求失敗，返回空列表
        mutableStateListOf()
    }
}

// 合成物品
suspend fun craftItem(userId: String, materialItemId: String): SnapshotStateList<UserItem> = withContext(Dispatchers.IO) {
    try {
        Log.d("API", "正在合成物品，使用者ID: $userId, 材料ID: $materialItemId")
        val updatedUser = RetrofitClient.apiService.craftItem(userId, CraftRequestBody(materialItemId))
        val userItems = mutableStateListOf<UserItem>()

        for (backpackItem in updatedUser.backpackItems) {
            try {
                val itemDetails = RetrofitClient.apiService.getItem(backpackItem.itemId)
                userItems.add(UserItem(item = itemDetails, quantity = backpackItem.quantity))
            } catch (e: Exception) {
                Log.e("API", "獲取合成後物品詳情失敗: ${backpackItem.itemId}", e)
            }
        }
        Log.d("API", "物品合成成功")
        userItems
    } catch (e: Exception) {
        Log.e("API", "物品合成請求失敗: ${e.message}", e)
        throw e // 將例外向上拋出，以便 UI 層可以處理
    }
}
