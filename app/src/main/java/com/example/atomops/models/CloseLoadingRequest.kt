package com.example.atomops.models

import com.google.gson.annotations.SerializedName

data class CloseLoadingRequest(
    @SerializedName("actor") val actor: String
)