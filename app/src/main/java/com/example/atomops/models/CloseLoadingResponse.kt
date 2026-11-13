package com.example.atomops.models

import com.google.gson.annotations.SerializedName

data class CloseLoadingResponse(
    @SerializedName("data") val data: Any?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?
)