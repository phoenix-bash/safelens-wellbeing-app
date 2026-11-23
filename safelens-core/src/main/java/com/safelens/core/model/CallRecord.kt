package com.safelens.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED,
    REJECTED
}

@Serializable
data class CallRecord(
    val id: String,
    val contactNumber: String,
    val contactName: String? = null,
    val callType: CallType,
    val startTimestamp: Long,
    val endTimestamp: Long,
    val durationSeconds: Long
)
