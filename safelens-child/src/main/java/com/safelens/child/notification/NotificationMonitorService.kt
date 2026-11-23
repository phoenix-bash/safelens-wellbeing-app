package com.safelens.child.notification

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.core.model.AppCategory
import com.safelens.core.model.NotificationEvent
import java.util.UUID

/**
 * Event-Driven Notification Monitoring Service.
 * Runs strictly via Android's native callback mechanism - ZERO polling or battery drain when idle.
 * Analyzes communication apps for study distractions and abusive/trash-talk language without storing private chat logs.
 */
class NotificationMonitorService : NotificationListenerService() {

    private val monitoredSocialPackages = setOf(
        "com.whatsapp",
        "com.instagram.android",
        "org.telegram.messenger",
        "com.discord",
        "com.snapchat.android",
        "com.google.android.apps.messaging",
        "com.facebook.orca"
    )

    // Keywords indicating potential abusive language / cyberbullying / trash talk
    private val abusiveKeywords = setOf(
        "idiot", "loser", "shut up", "trash", "noob", "hate you", "stupid", "kys", "kill yourself"
    )

    override fun onListenerConnected() {
        super.onListenerConnected()
        isServiceConnected = true
        Log.i(TAG, "NotificationMonitorService connected and active.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isServiceConnected = false
        Log.w(TAG, "NotificationMonitorService disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val pkgName = sbn.packageName ?: return
        
        // Skip system and core Android apps
        if (pkgName == "android" || pkgName.startsWith("com.android.systemui") || pkgName.startsWith("com.google.android.inputmethod")) {
            return
        }

        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val text = extras.getCharSequence("android.text")?.toString().orEmpty()
        val combinedContent = "$title $text".lowercase()

        val isCommunicationApp = monitoredSocialPackages.contains(pkgName)
        val appCategory = if (isCommunicationApp) AppCategory.MESSAGING else AppCategory.OTHER

        // Scan for flagged language
        val matchedWords = abusiveKeywords.filter { keyword -> combinedContent.contains(keyword) }
        val isFlagged = matchedWords.isNotEmpty()

        // Distraction metric: notifications during focus time
        val distractionScore = if (isCommunicationApp) 1 else 0

        val event = NotificationEvent(
            id = UUID.randomUUID().toString(),
            packageName = pkgName,
            appName = resolveAppName(pkgName),
            timestamp = sbn.postTime,
            appCategory = appCategory,
            isFlagged = isFlagged,
            matchedKeywords = matchedWords,
            distractionScore = distractionScore,
            // Privacy preserved: Do NOT store raw message text. Only save categorization and flagged reasons.
            contextSummary = if (isFlagged) "Flagged language detected: ${matchedWords.joinToString(", ")}" else null
        )

        Log.d(TAG, "Notification evaluated: ${event.appName} (Category: ${event.appCategory}, Flagged: ${event.isFlagged})")
        
        // Record event into local buffer
        NotificationBuffer.addEvent(event)
    }

    private fun resolveAppName(packageName: String): String {
        return try {
            val pm = packageManager
            val appInfo = pm.getApplicationInfo(packageName, 0)
            pm.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            packageName
        }
    }

    companion object {
        var isServiceConnected: Boolean = false
            private set
    }
}

object NotificationBuffer {
    private val buffer = mutableListOf<NotificationEvent>()

    @Synchronized
    fun addEvent(event: NotificationEvent) {
        if (buffer.size >= 500) {
            buffer.removeAt(0)
        }
        buffer.add(event)
    }

    @Synchronized
    fun getRecentEvents(): List<NotificationEvent> = buffer.toList()
}
