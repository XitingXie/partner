package com.lingomia.android.network

import android.content.Context
import android.util.Log
import com.lingomia.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiConfig {
    private const val BASE_URL = BuildConfig.BASE_URL + "/" // Add trailing slash for Retrofit

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Connection timeout
        .readTimeout(30, TimeUnit.SECONDS)     // Read timeout
        .writeTimeout(30, TimeUnit.SECONDS)    // Write timeout
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d("API", "Request URL: ${request.url}")
            Log.d("API", "Request Headers: ${request.headers}")
            
            try {
                val response = chain.proceed(request)
                val responseBody = response.peekBody(Long.MAX_VALUE).string()
                Log.d("API", "Response Code: ${response.code}")
                Log.d("API", "Response Body: $responseBody")
                response
            } catch (e: Exception) {
                Log.e("API", "Network Error", e)
                throw e
            }
        }
        .addInterceptor(loggingInterceptor)
        .build()

    val apiService: ApiService = createApiService()

    private fun createApiService(): ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    // Add a method to get ApiService with optional context
    fun getApiService(context: Context? = null): ApiService {
        // If context is provided and you want to add context-specific interceptors, you can do so here
        return apiService
    }
} 