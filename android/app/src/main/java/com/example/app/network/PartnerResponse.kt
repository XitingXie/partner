package com.example.app.network
import com.google.gson.annotations.SerializedName

data class PartnerResponse(
    @SerializedName("message") val message: String
)