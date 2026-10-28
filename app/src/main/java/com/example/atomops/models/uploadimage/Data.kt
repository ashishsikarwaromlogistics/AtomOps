package com.example.atomops.models.uploadimage

data class Data(
    val companyId: String,
    val createdAt: String,
    val createdBy: String,
    val description: String,
    val documentCode: String,
    val documentName: String,
    val documentType: String,
    val entityType: String,
    val fileHash: String,
    val fileSizeBytes: Int,
    val fileType: String,
    val id: String,
    val linked: Boolean,
    val status: String,
    val storagePath: String,
    val templateId: String,
    val templateName: String,
    val updatedAt: String,
    val updatedBy: String,
    val version: Int
)