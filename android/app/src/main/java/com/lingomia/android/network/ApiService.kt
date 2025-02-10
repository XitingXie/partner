package com.lingomia.android.network

import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.PUT
import retrofit2.http.Query
import com.lingomia.android.data.models.*
import com.google.gson.annotations.SerializedName

data class OpenAIKeyResponse(
    @SerializedName("api_key") val apiKey: String
)

interface ApiService {
    @GET("api/topics")
    suspend fun getTopics(@Query("uid") userId: String): List<Topic>

    @GET("api/topics/{topicId}/scenes")
    suspend fun getScenes(@Path("topicId") topicId: String): List<Scene>

    @POST("api/conversation/tutor")
    suspend fun chatWithTutor(@Body request: ChatRequest): TutorResponse

    @POST("api/conversation/partner")
    suspend fun chatWithPartner(@Body request: ChatRequest): PartnerResponse

    @POST("api/conversation/session")
    suspend fun createSession(@Body request: CreateSessionRequest): CreateSessionResponse

    @GET("api/scenes/{sceneId}/levels/{level}")
    suspend fun getSceneLevel(
        @Path("sceneId") sceneId: String,
        @Path("level") englishLevel: String
    ): SceneLevel

    @GET("api/users/{userId}")
    suspend fun checkUserExists(@Path("userId") userId: String): UserResponse

    @POST("api/users")
    suspend fun insertUser(@Body user: UserRequest): UserResponse

    @PUT("api/users/{userId}/language")
    suspend fun updateFirstLanguage(
        @Path("userId") userId: String,
        @Body request: Map<String, String>
    ): UserResponse

    @GET("api/config/openai-key")
    suspend fun getOpenAIKey(): OpenAIKeyResponse
}