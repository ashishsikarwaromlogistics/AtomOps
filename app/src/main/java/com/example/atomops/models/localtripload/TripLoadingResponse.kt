package com.example.atomops.models.localtripload

import com.google.gson.annotations.SerializedName

data class TripLoadingResponse(
    @SerializedName("data") val data: TripLoadingData?,
    @SerializedName("message") val message: String?,
    @SerializedName("status") val status: String?
)

data class TripLoadingData(
    @SerializedName("trip_no") val tripNo: String?,
    @SerializedName("trip_status") val tripStatus: String?,
    @SerializedName("vehicle_no") val vehicleNo: String?,
    @SerializedName("dock_code") val dockCode: String?,
    @SerializedName("drs_status") val drsStatus: String?,
    @SerializedName("total_boxes") val totalBoxes: Int = 0,
    @SerializedName("loaded_boxes") val loadedBoxes: Int = 0,
    @SerializedName("exception_boxes") val exceptionBoxes: Int = 0,
    @SerializedName("short_boxes") val shortBoxes: Int = 0,
    @SerializedName("excess_boxes") val excessBoxes: Int = 0,
    @SerializedName("damage_boxes") val damageBoxes: Int = 0,
    @SerializedName("total_dockets") val totalDockets: Int = 0,
    @SerializedName("started_dockets") val startedDockets: Int = 0,
    @SerializedName("last_scan_at") val lastScanAt: String?,
    @SerializedName("pending_boxes") val pendingBoxes: Int = 0,
    @SerializedName("percent_complete") val percentComplete: Int = 0,
    @SerializedName("exceptions") val exceptions: List<TripException>?,
    @SerializedName("dockets") val dockets: List<TripDocket>?,
    @SerializedName("removed_on_board") val removedOnBoard: List<Any>?
)

data class TripException(
    @SerializedName("docket_no") val docketNo: String?,
    @SerializedName("box_id") val boxId: String?,
    @SerializedName("exception_type") val exceptionType: String?,
    @SerializedName("exception_reason") val exceptionReason: String?,
    @SerializedName("scan_time") val scanTime: String?
)

data class TripDocket(
    @SerializedName("docket_no") val docketNo: String?,
    @SerializedName("consignee_name") val consigneeName: String?,
    @SerializedName("delivery_sequence") val deliverySequence: Int = 0,
    @SerializedName("cn_status") val cnStatus: String?,
    @SerializedName("total_boxes") val totalBoxes: Int = 0,
    @SerializedName("loaded_boxes") val loadedBoxes: Int = 0,
    @SerializedName("exception_boxes") val exceptionBoxes: Int = 0,
    @SerializedName("pending_boxes") val pendingBoxes: Int = 0,
    @SerializedName("fully_accounted") val fullyAccounted: Boolean = false
)