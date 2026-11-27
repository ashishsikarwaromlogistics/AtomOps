package com.example.atomops.models.manifiests


    data class ManifestProgressResponse(
        val data: ManifestProgressData?,
        val message: String?,
        val status: String?
    )

    data class ManifestProgressData(
        val tripNo: String?,
        val manifestNo: String?,
        val legSequence: Int?,
        val sourceBranch: String?,
        val destinationBranch: String?,
        val status: String?,
        val statusChangedAt: String?,
        val stage: String?,
        val progress: ManifestProgress?
    )

    data class ManifestProgress(
        val started: Boolean?,
        val startedAt: String?,
        val startedBy: String?,
        val closed: Boolean?,
        val closedAt: String?,
        val closedBy: String?,
        val boxesExpected: Int?,
        val boxesScanned: Int?,
        val boxesShort: Int?,
        val boxesExcess: Int?,
        val boxesPending: Int,
        val photo50: PhotoInfo?,
        val photo100: PhotoInfo?,
        val photo100Required: Boolean?,
        val canClose: Boolean?,
        val closeBlockers: List<String>?
    )

    data class PhotoInfo(
        val photoDocumentId: String?,
        val capturedAt: String?,
        val capturedBy: String?,
        val branchCode: String?
    )
