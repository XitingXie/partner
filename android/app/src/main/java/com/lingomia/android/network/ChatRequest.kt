package com.lingomia.android.network
import com.google.gson.annotations.SerializedName

data class ChatRequest(
    @SerializedName("session_id") val sessionId: String,
    @SerializedName("scene_id") val sceneId: Int,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("user_input") val userInput: String
) 