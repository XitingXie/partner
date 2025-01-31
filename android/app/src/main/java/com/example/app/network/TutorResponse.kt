package com.example.app.network
import com.google.gson.annotations.SerializedName

data class TutorResponse(
    @SerializedName("feedback") val feedback: String,
    @SerializedName("tutor_message") val tutorMessage: String,
    @SerializedName("needs_correction") val needsCorrection: Boolean
)