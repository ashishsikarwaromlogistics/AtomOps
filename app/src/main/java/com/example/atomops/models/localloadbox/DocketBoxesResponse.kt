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
    @SerializedName("status") val status: String?,
    @SerializedName("currentBranch") val currentBranch: String?,
    @SerializedName("voided") val voided: Boolean = false
)