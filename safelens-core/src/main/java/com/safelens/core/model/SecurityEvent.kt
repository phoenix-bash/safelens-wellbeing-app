package com.safelens.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class SecurityEventType {
    PACKAGE_UNINSTALLED,
    PACKAGE_DATA_CLEARED,
    PERMISSION_REVOKED,
    NOTIFICATION_LISTENER_DISABLED,
    FORCE_STOP_SUSPECTED,
    WATCHDOG_HEARTBEAT_TIMEOUT,
    TAMPER_SUSPECTED
}

@Serializable
data class SecurityEvent(
    val id: String,
    val eventType: SecurityEventType,
    val affectedPackage: String,
    val timestamp: Long = System.currentTimeMillis(),
    val description: String,
    val severity: String = "HIGH"
)
