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
    suspend fun getScenes(@Path("topicId") topicId: String): List<Scene>

    @POST("api/conversation/session")
    suspend fun createSession(@Body request: CreateSessionRequest): CreateSessionResponse

    @POST("api/conversation/chat")
    suspend fun chat(@Body request: ChatRequest): ChatResponse
}

data class CreateSessionRequest(
    val person_id: String,
    val scene_id: String
)

data class CreateSessionResponse(
    val id: String,
    val person_id: String,
    val scene_id: String,
    val started_at: String
)

data class ChatRequest(
    val session_id: String,
    val user_id: String,
    val scene_id: String,
    val user_input: String
)

data class ChatResponse(
    val message: String,
    val feedback: Feedback? = null
)

data class Feedback(
    val unfamiliar_words: List<String>,
    val grammar_errors: Map<String, String>,
    val not_so_good_expressions: Map<String, String>,
    val best_fit_words: Map<String, String>
) 