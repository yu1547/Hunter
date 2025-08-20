package com.ntou01157.hunter.api

import com.ntou01157.hunter.api.ApiConfig.BASE_URL
import com.ntou01157.hunter.models.RecognizeRequest
import com.ntou01157.hunter.models.RecognizeResponse
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST

interface RecognizeApi {
//    @POST("api/recognize/true")//必成功
    @POST("api/recognize")//真實辨識
    suspend fun recognize(@Body body: RecognizeRequest): Response<RecognizeResponse>
}

object RecognizeApiProvider {
    val service: RecognizeApi by lazy {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
        val http = OkHttpClient.Builder().addInterceptor(logging).build()
        Retrofit.Builder()
            .baseUrl(BASE_URL) // 來自 ApiConfig
            .client(http)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(RecognizeApi::class.java)
    }
}
