package com.ntou01157.hunter.api

import com.ntou01157.hunter.api.ApiConfig.BASE_URL
import com.ntou01157.hunter.models.RecognizeRequest
import com.ntou01157.hunter.models.RecognizeResponse
import com.ntou01157.hunter.temp.TokenManager // ✅ 引入 TokenManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface RecognizeApi {
    @POST("api/recognize")
    suspend fun recognize(@Body body: RecognizeRequest): Response<RecognizeResponse>
}

object RecognizeApiProvider {
    val service: RecognizeApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }

        val http = OkHttpClient.Builder()
            .addInterceptor(logging)
            .addInterceptor { chain ->
                val requestBuilder = chain.request().newBuilder()
                val token = TokenManager.idToken
                if (!token.isNullOrEmpty()) {
                    requestBuilder.addHeader("Authorization", "Bearer $token")
                }
                chain.proceed(requestBuilder.build())
            }
            .build()

        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecognizeApi::class.java)
    }
}
