package com.ntou01157.hunter.handlers

import android.content.Context
import android.widget.Toast
import com.ntou01157.hunter.api.DropApi
import com.ntou01157.hunter.models.User
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

object DropHandler {
    fun collectDrop(context: Context, user: User, difficulty: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = DropApi.getDrop(user.uid, difficulty)
                withContext(Dispatchers.Main) {
                    if (response != null && response.success) {
                        response.drops.forEach { itemName ->
                            Toast.makeText(context, "獲得物品：$itemName", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "獲取資源失敗", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "發生錯誤：${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
