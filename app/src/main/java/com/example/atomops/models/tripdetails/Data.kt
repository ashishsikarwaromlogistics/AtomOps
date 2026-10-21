package com.example.atomops.models.tripdetails

data class Data(
    val drivers: List<Driver>,
    val holds: List<Any>,
    val lastGateEvent: Any,
    val load: Load,
    val loaders: List<Loader>,
    val manifests: List<Manifest>,
    val route: Route,
    val trip: Trip,
    val vehicle: Vehicle
)