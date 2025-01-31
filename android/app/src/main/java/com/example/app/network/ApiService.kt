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

    @POST("/api/conversation/tutor")
    suspend fun chatWithTutor(@Body request: ChatRequest): TutorResponse

    @POST("/api/conversation/partner")
    suspend fun chatWithPartner(@Body request: ChatRequest): PartnerResponse

    @POST("/api/conversation/session")
    suspend fun createSession(@Body request: CreateSessionRequest): CreateSessionResponse

    @GET("/api/scenes/{sceneId}/levels/{level}")
    suspend fun getSceneLevel(
        @Path("sceneId") sceneId: Int,
        @Path("level") englishLevel: String
    ): SceneLevel
}