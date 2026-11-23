package com.safelens.core.model

import kotlinx.serialization.Serializable

@Serializable
enum class AppCategory {
    MESSAGING,
    SOCIAL,
    GAMING,
    STUDY_PRODUCTIVITY,
    ENTERTAINMENT,
    OTHER
}

@Serializable
data class NotificationEvent(
    val id: String,
    val packageName: String,
    val appName: String,
    val timestamp: Long,
    val appCategory: AppCategory,
    val isFlagged: Boolean,
    val matchedKeywords: List<String> = emptyList(),
    val distractionScore: Int = 0,
    val contextSummary: String? = null
)
