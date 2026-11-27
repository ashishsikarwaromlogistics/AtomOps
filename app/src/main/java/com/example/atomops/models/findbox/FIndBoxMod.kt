package com.example.atomops.models.findbox

data class FIndBoxMod(
    val companyCode: Int,
    val docketNos: List<String>,
    val eventType:String,
    val scanStage:String,
    val branchCode:String,
   /* "eventType": "OUT_SCAN",
"scanStage": "LOAD1",
"branchCode": "1314"*/
)