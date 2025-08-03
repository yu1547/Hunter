package com.ntou01157.hunter.data

import com.ntou01157.hunter.api.RetrofitClient
import com.ntou01157.hunter.model.model_api.Settings

suspend fun fetchSettings(userId: String): Settings {
    // 直接呼叫 RetrofitClient 的 fetchSettings
    return RetrofitClient.apiService.fetchSettings(userId)
}
