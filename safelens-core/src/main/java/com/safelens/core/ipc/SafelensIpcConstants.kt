package com.safelens.core.ipc

object SafelensIpcConstants {
    const val CHILD_PACKAGE = "com.safelens.child"
    const val SUPPORT_PACKAGE = "com.safelens.support"

    // Custom Signature-Level Permission
    const val PERMISSION_INTERNAL_IPC = "com.safelens.permission.INTERNAL_COMMUNICATION"

    // Explicit Action Intents
    const val ACTION_INITIALIZE_SUPPORT = "com.safelens.support.ACTION_INITIALIZE"
    const val ACTION_HEARTBEAT_PING = "com.safelens.support.ACTION_HEARTBEAT_PING"
    const val ACTION_HEARTBEAT_ACK = "com.safelens.child.ACTION_HEARTBEAT_ACK"
    const val ACTION_SECURITY_ALERT = "com.safelens.action.SECURITY_ALERT"
    const val ACTION_REQUEST_AUDIT = "com.safelens.support.ACTION_REQUEST_AUDIT"

    // Component Services
    const val SUPPORT_SERVICE_CLASS = "com.safelens.support.service.HeadlessSecurityService"
    const val CHILD_SERVICE_CLASS = "com.safelens.child.service.ChildWatchdogService"

    // Intent Extras
    const val EXTRA_PAIRING_TOKEN = "extra_pairing_token"
    const val EXTRA_PAYLOAD_JSON = "extra_payload_json"
    const val EXTRA_EVENT_TYPE = "extra_event_type"
    const val EXTRA_TIMESTAMP = "extra_timestamp"
}
