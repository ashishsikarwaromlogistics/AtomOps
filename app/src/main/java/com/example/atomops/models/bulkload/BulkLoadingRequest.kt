package com.example.atomops.models.bulkload

import com.google.gson.annotations.SerializedName

data class BulkLoadingRequest(
    @SerializedName("closeIfComplete") val closeIfComplete: Boolean,
    @SerializedName("actor") val actor: String,
    @SerializedName("items") val items: List<BulkItem>,
    @SerializedName("exceptions") val exceptions: List<BulkException>
)

data class BulkItem(
    @SerializedName("docketNo") val docketNo: String,
    @SerializedName("boxId") val boxId: String,
    @SerializedName("publicScanId") val publicScanId: String
)

data class BulkException(
    @SerializedName("docketNo") val docketNo: String,
    @SerializedName("boxId") val boxId: String,
    @SerializedName("exceptionType") val exceptionType: String,   // DAMAGED_BOX / BOX_MISSING
    @SerializedName("reason") val reason: String,
    @SerializedName("publicScanId") val publicScanId: String?      // null for BOX_MISSING
)

data class BulkLoadingResponse(
    @SerializedName("data") val data: Any?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?
)