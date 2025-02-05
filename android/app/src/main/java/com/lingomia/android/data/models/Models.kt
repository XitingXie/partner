package com.lingomia.android.data.models

import com.google.gson.annotations.SerializedName

data class Topic(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String
) {
    override fun toString(): String {
        return "Topic(id='$id', name='$name', description='$description')"
    }
}

data class Scene(
    @SerializedName("id") val id: Int,
    @SerializedName("name") val title: String,
    @SerializedName("context") val description: String
)

data class SceneLevel(
    val id: Int,
    val sceneId: Int,
    val englishLevel: String,
    val exampleDialogs: String?,
    val keyPhrases: String?,
    val vocabulary: String?,
    val grammarPoints: String?,
    val createdAt: String?
) 

data class CreateSessionRequest(
    @SerializedName("topic_id") val topicId: Int,
    @SerializedName("scene_id") val sceneId: Int,
    @SerializedName("user_id") val userId: Int
)

data class CreateSessionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("scene_id") val sceneId: Int,
    @SerializedName("started_at") val startedAt: String
)

data class ChatRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("scene_id") val sceneId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_input") val userInput: String
)

data class ChatResponse(
    @SerializedName("message") val message: String
)

data class ChatMessage(
    @SerializedName("id") val id: Int,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("type") val type: String
)

data class LearningPoint(
    val type: String, // "UNFAMILIAR_WORD", "BETTER_EXPRESSION", etc.
    val original: String,
    val explanation: String
)