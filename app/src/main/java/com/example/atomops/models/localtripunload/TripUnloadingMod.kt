package com.example.atomops.models.localtripunload

// Response of getTripUnLoading
data class TripUnloadingMod(
    val data: TripUnloadingData? = null,
    val message: Any? = null,
    val status: String? = null
)

data class TripUnloadingData(
    val tripNo: String? = null,
    val tripStatus: String? = null,
    val boxesNotBack: Int? = null,
    val boxesOnRecord: Int? = null,
    val pickupBoxesShort: Int? = null,
    val delivery: List<UnloadDelivery>? = null,
    val pickups: List<UnloadPickup>? = null,
    val pending: List<UnloadPending>? = null
)

data class UnloadDelivery(
    val docketNo: String? = null,
    val boxId: String? = null,
    val cnStatus: String? = null,
    val rewarehoused: Boolean? = null,
    val exceptionId: Int? = null,
    val exceptionStatus: String? = null
)

data class UnloadPickup(
    val prsNo: String? = null,
    val prqNo: String? = null,
    val prqStatus: String? = null,
    val collectedBoxes: Int? = null,
    val scannedBoxes: Int? = null,
    val receivedBoxes: Int? = null,
    val boxesOutstanding: Int? = null,
    val shortExceptionId: Int? = null,
    val shortExceptionStatus: String? = null,
    val dockets: List<UnloadPickupDocket>? = null
)

data class UnloadPickupDocket(
    val docketNo: String? = null,
    val scannedBoxes: Int? = null,
    val boxesToScan: Int? = null,
    val boxes: List<UnloadPickupBox>? = null      // can be null in the response
)

data class UnloadPickupBox(
    val boxId: String? = null,
    val scanned: Boolean? = null
)

data class UnloadPending(
    val code: String? = null,
    val detail: String? = null
)
