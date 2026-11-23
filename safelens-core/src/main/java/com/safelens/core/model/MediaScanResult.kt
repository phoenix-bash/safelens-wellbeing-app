package com.safelens.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MediaScanResult(
    val id: String,
    val fileHash: String,
    val fileName: String,
    val mimeType: String,
    val timestamp: Long,
    val isFlaggedExplicit: Boolean,
    val confidenceScore: Float,
    val scanDurationMs: Long
)
