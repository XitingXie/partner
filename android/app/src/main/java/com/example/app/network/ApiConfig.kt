package com.example.app.network

import android.util.Log
import com.example.app.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiConfig {
    private const val BASE_URL = BuildConfig.BASE_URL + "/" // Add trailing slash for Retrofit

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d("API", message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val client = OkHttpClient.Builder()
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

    val apiService: ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)
} 