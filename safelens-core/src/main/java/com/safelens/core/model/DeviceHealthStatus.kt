package com.safelens.core.model

import kotlinx.serialization.Serializable

@Serializable
data class DeviceHealthStatus(
    val batteryLevel: Int,
    val isCharging: Boolean,
    val networkType: String,
    val isChildAppActive: Boolean,
    val isSupportModuleActive: Boolean,
    val isNotificationListenerEnabled: Boolean,
    val isCallLogPermitted: Boolean,
    val isMediaPermitted: Boolean,
    val lastSyncTimestamp: Long,
    val timestamp: Long = System.currentTimeMillis()
)
