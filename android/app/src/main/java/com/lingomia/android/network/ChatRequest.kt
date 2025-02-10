package com.lingomia.android.network
import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("scene_id") val sceneId: String,
    @SerializedName("uid") val userId: String,
    @SerializedName("user_input") val userInput: String,
    @SerializedName("first_language") val first_language: String? = null
) 