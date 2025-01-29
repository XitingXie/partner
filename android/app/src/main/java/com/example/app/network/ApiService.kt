package com.example.app.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import com.example.app.data.models.*

interface ApiService {
    @GET("api/topics")
    suspend fun getTopics(): List<Topic>

    @GET("api/topics/{topicId}/scenes")
    suspend fun getScenes(@Path("topicId") topicId: Int): List<Scene>

    @POST("api/conversation/session")
    suspend fun createSession(@Body request: CreateSessionRequest): CreateSessionResponse

    @POST("api/conversation/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}