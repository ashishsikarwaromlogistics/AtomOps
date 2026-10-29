package com.example.atomops.models.dispatch

data class DispatchMod(
    val actor: String,
    val checklist: Checklist,
    val commodityClass: String,
    val companyCode: Int
)