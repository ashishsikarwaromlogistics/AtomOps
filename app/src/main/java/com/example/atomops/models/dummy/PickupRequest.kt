package com.example.atomops.models.dummy


enum class RequestStatus(val label: String) {
    CREATED("Created"),
    OUT_FOR_PICKUP("Out for Pickup")
}

data class PickupRequest(
    val id: String,
    val createdAt: String,
    val status: RequestStatus,
    val customer: String,
    val location: String,
    val phone: String,
    val pickupSlot: String,
    val loadType: String
)