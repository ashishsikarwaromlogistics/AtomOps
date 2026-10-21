package com.example.atomops.models

enum class RequestType { PICKUP, DROP }

enum class RequestStatus(val label: String) {
    CREATED("Created"),
    OUT_FOR_PICKUP("Out for Pickup")
}

data class PrqRequest(
    val id: String,
    val createdAt: String,
    val status: RequestStatus,
    val type: RequestType,
    val customer: String,
    val location: String,
    val phone: String,
    val slotDate: String,
    val slotTime: String,
    val loadType: String
)

object DummyPrqData {

    private val customers = listOf(
        "CUS789656 - Creative Wealth Management Pvt. Ltd.",
        "CUS123401 - Sharma Traders",
        "CUS556677 - Bright Logistics India",
        "CUS990011 - Delhi Auto Parts Co.",
        "CUS334455 - Royal Textiles Pvt. Ltd.",
        "CUS778899 - Kapoor Electronics"
    )

    private val locations = listOf(
        "110064 (Mayapuri, Delhi)",
        "110020 (Okhla, Delhi)",
        "122001 (Gurugram, Haryana)",
        "201301 (Noida, UP)",
        "110092 (Preet Vihar, Delhi)",
        "110018 (Tilak Nagar, Delhi)"
    )

    private val loadTypes = listOf(
        "Part Truck Load (PTL)",
        "Full Truck Load (FTL)"
    )

    private fun build(type: RequestType, prefix: String): List<PrqRequest> =
        List(10) { i ->
            PrqRequest(
                id = "$prefix${ID_BASE + i}",
                createdAt = "14-01-2025, 14:${(30 + i).toString().padStart(2, '0')}",
                status = if (i % 2 == 0) RequestStatus.CREATED else RequestStatus.OUT_FOR_PICKUP,
                type = type,
                customer = customers[i % customers.size],
                location = locations[i % locations.size],
                phone = "82916584${(10 + i)}",
                slotDate = "12-03-2026",
                slotTime = if (i % 2 == 0) "09:00 - 11:00" else "13:00 - 15:00",
                loadType = loadTypes[i % loadTypes.size]
            )
        }

    private const val ID_BASE = 8785255

    fun pickupRequests(): List<PrqRequest> = build(RequestType.PICKUP, "PRQ87S")
    fun dropRequests(): List<PrqRequest> = build(RequestType.DROP, "DRQ87S")
}
