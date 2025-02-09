package com.lingomia.android.network

import android.content.Context
import android.util.Log
import com.lingomia.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.GetTokenResult

object ApiConfig {
    private const val BASE_URL = BuildConfig.BASE_URL + "/" // Add trailing slash for Retrofit
    private const val TAG = "ApiConfig"

    private val loggingInterceptor = HttpLoggingInterceptor { message ->
        Log.d(TAG, message)
    }.apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        
        // Get current Firebase user
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        
        if (firebaseUser == null) {
            Log.d(TAG, "No Firebase user found, proceeding without token")
            return@Interceptor chain.proceed(originalRequest)
        }
        
        // Use runBlocking to handle the async token retrieval
        val idToken = runBlocking {
            try {
                // Force token refresh if needed
                firebaseUser.getIdToken(true).await().token
            } catch (e: Exception) {
                Log.e(TAG, "Error getting ID token", e)
                null
            }
        }
        
        // If we got a token, add it to the request
        val request = if (idToken != null) {
            Log.d(TAG, "Adding token to request: ${idToken.take(10)}...")
            originalRequest.newBuilder()
                .header("Authorization", "Bearer $idToken")
                .build()
        } else {
            Log.w(TAG, "No token available, proceeding without authorization")
            originalRequest
        }
        
        // Proceed with the request
        val response = chain.proceed(request)
        
        // Handle 401 responses by forcing a token refresh and retrying once
        if (response.code == 401 && idToken != null) {
            Log.w(TAG, "Received 401, attempting token refresh and retry")
            response.close()
            
            val newToken = runBlocking {
                try {
                    // Force a new token
                    firebaseUser.getIdToken(true).await().token
                } catch (e: Exception) {
                    Log.e(TAG, "Error refreshing token", e)
                    null
                }
            }
            
            return@Interceptor if (newToken != null) {
                Log.d(TAG, "Retrying with new token: ${newToken.take(10)}...")
                val newRequest = originalRequest.newBuilder()
                    .header("Authorization", "Bearer $newToken")
                    .build()
                chain.proceed(newRequest)
            } else {
                response
            }
        }
        
        response
    }

    private fun createOkHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)  // Connection timeout
        .readTimeout(30, TimeUnit.SECONDS)     // Read timeout
        .writeTimeout(30, TimeUnit.SECONDS)    // Write timeout
        .addInterceptor(authInterceptor)       // Add auth interceptor first
        .addInterceptor { chain ->
            val request = chain.request()
            Log.d(TAG, "Request URL: ${request.url}")
            Log.d(TAG, "Request Headers: ${request.headers}")
            
            try {
                val response = chain.proceed(request)
                val responseBody = response.peekBody(Long.MAX_VALUE).string()
                Log.d(TAG, "Response Code: ${response.code}")
                Log.d(TAG, "Response Body: $responseBody")
                response
            } catch (e: Exception) {
                Log.e(TAG, "Network Error", e)
                throw e
            }
        }
        .addInterceptor(loggingInterceptor)
        .build()

    private fun createApiService(): ApiService = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(createOkHttpClient())
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(ApiService::class.java)

    // Expose the singleton ApiService instance
    val apiService: ApiService = createApiService()
} 