package com.example.atomops.models.localloadbox

import com.google.gson.annotations.SerializedName


data class DocketBoxesResponse(
    @SerializedName("data") val data: DocketBoxesData?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?
)

data class DocketBoxesData(
    @SerializedName("dockets") val dockets: List<DocketBoxInfo>?
)

data class DocketBoxInfo(
    @SerializedName("docketNo") val docketNo: String?,
    @SerializedName("found") val found: Boolean = false,
    @SerializedName("boxes") val boxes: List<BoxInfo>?
)

data class BoxInfo(
    @SerializedName("boxCode") val boxCode: String?,
    @SerializedName("seq") val seq: Int = 0,
    @SerializedName("status") val status: String?,    //DELIVERED --
    @SerializedName("currentBranch") val currentBranch: String?,
    @SerializedName("scanActionReason") val scanActionReason: String?,
    @SerializedName("scanAction") val scanAction: String?,
    @SerializedName("voided") val voided: Boolean = false,
    @SerializedName("lastMovement") val lastMovement: LastMovement?
)

data class LastMovement(
    @SerializedName("scanEventId") val scanEventId: String?,
    @SerializedName("eventType") val eventType: String?,
    @SerializedName("branchCode") val branchCode: String?,
    @SerializedName("scannedAt") val scannedAt: String?
)