package com.example.atomops.models.findbox

data class Docket(
    val boxes: List<Boxe>,
    val docketNo: String,
    val found: Boolean
)