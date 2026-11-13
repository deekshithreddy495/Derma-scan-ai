package com.example.data.remote

import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.util.concurrent.TimeUnit

interface DermaScanApiService {
    @GET("api/health")
    suspend fun getHealth(): Response<HealthResponseDto>

    @GET("api/metrics")
    suspend fun getMetrics(): Response<EvaluationMetricsDto>

    @Multipart
    @POST("api/predict")
    suspend fun predictLesion(
        @Part file: MultipartBody.Part,
    ): Response<PredictionResponseDto>
}

object DermaScanNetworkFactory {
    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(45, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()
    }

    fun create(baseUrl: String): DermaScanApiService {
        val safeBase = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeBase)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(DermaScanApiService::class.java)
    }
}
