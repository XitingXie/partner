package com.lingomia.android.data.models

import com.google.gson.annotations.SerializedName

data class Topic(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String
) {
    override fun toString(): String {
        return "Topic(id='$id', name='$name', description='$description')"
    }
}

data class Scene(
    @SerializedName("id") val id: String,
    @SerializedName("name") val title: String,
    @SerializedName("description") val description: String
)

data class SceneLevel(
    @SerializedName("id") val id: String,
    @SerializedName("sceneId") val sceneId: String,
    @SerializedName("englishLevel") val englishLevel: String,
    @SerializedName("exampleDialogs") val exampleDialogs: String?,
    @SerializedName("keyPhrases") val keyPhrases: String?,
    @SerializedName("vocabulary") val vocabulary: String?,
    @SerializedName("grammarPoints") val grammarPoints: String?,
    @SerializedName("createdAt") val createdAt: Any? = null
) 

data class CreateSessionRequest(
    @SerializedName("topic_id") val topicId: String,
    @SerializedName("scene_id") val sceneId: String,
    @SerializedName("user_id") val userId: String
)

data class CreateSessionResponse(
    @SerializedName("id") val id: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("scene_id") val sceneId: String,
    @SerializedName("started_at") val startedAt: String
)

data class ChatRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("scene_id") val sceneId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("user_input") val userInput: String,
    @SerializedName("first_language") val first_language: String? = null
)

data class ChatResponse(
    @SerializedName("message") val message: String
)

data class ChatMessage(
    @SerializedName("id") val id: String,
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("user_id") val userId: String,
    @SerializedName("content") val content: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("type") val type: String
)

data class LearningPoint(
    val type: String, // "UNFAMILIAR_WORD", "BETTER_EXPRESSION", etc.
    val original: String,
    val explanation: String
)

data class UserRequest(
    @SerializedName("uid") val uid: String,
    @SerializedName("email") val email: String,
    @SerializedName("display_name") val displayName: String? = null,
    @SerializedName("given_name") val givenName: String? = null,
    @SerializedName("family_name") val familyName: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null
)

data class UserResponse(
    @SerializedName("exists") val exists: Boolean,
    @SerializedName("message") val message: String? = null,
    @SerializedName("first_language") val first_language: String? = null,
    @SerializedName("uid") val uid: String? = null,
    @SerializedName("user_info") val userInfo: UserInfo? = null
)

data class UserInfo(
    @SerializedName("display_name") val displayName: String?,
    @SerializedName("given_name") val givenName: String?,
    @SerializedName("family_name") val familyName: String?,
    @SerializedName("photo_url") val photoUrl: String?
)