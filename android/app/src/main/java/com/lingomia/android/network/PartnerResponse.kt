package com.lingomia.android.network
import com.google.gson.annotations.SerializedName

data class PartnerResponse(
    @SerializedName("message") val message: String
)