package com.example.app.data.models

import com.google.gson.annotations.SerializedName

data class Topic(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String
) {
    override fun toString(): String {
        return "Topic(id='$id', title='$title', description='$description')"
    }
}

data class Scene(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String
)

data class CreateSessionRequest(
    val userId: String,
    val sceneId: String
)

data class CreateSessionResponse(
    val sessionId: String
)

data class ChatRequest(
    val sessionId: String,
    val message: String
)

data class ChatResponse(
    @SerializedName("message") val message: String
)

data class ChatMessage(
    val id: String,
    val content: String,
    val isUser: Boolean,
    val timestamp: Long
)

data class LearningPoint(
    val type: String, // "UNFAMILIAR_WORD", "BETTER_EXPRESSION", etc.
    val original: String,
    val explanation: String
) 