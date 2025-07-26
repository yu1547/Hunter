// frontend/app/src/main/java/com/ntou01157/hunter/api/RankApi.kt

package com.ntou01157.hunter.api

import com.ntou01157.hunter.models.model_api.RankResponse
import retrofit2.Response
import retrofit2.http.GET

interface RankApi {
    @GET("api/rank") // 假設後端 API 路徑是 /api/rank
    suspend fun getRank(): Response<RankResponse>
}