package com.example.atomops.models.localloadbox

import com.google.gson.annotations.SerializedName

data class DocketBoxesRequest(
    @SerializedName("companyCode") val companyCode: Int,
    @SerializedName("docketNos") val docketNos: List<String>
)