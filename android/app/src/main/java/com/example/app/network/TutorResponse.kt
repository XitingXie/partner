package com.example.app.network
import com.google.gson.annotations.SerializedName

data class TutorResponse(
    @SerializedName("feedback") val feedback: String,
    @SerializedName("needs_correction") val needsCorrection: Boolean
)