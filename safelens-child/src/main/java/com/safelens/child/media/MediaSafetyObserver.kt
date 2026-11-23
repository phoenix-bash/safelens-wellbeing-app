package com.safelens.child.media

import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.core.model.MediaScanResult
import com.safelens.core.security.SecurityUtils
import java.util.UUID

/**
 * Event-Driven Media Safety Observer.
 * Listens for new media items via MediaStore ContentObserver.
 * Privacy-First: Analyzes images strictly ON-DEVICE without uploading raw images.
 */
class MediaSafetyObserver(
    private val context: Context,
    handler: Handler = Handler(Looper.getMainLooper())
) : ContentObserver(handler) {

    private var lastScannedUri: String? = null

    override fun onChange(selfChange: Boolean, uri: Uri?) {
        super.onChange(selfChange, uri)
        scanLatestMedia()
    }

    fun scanLatestMedia() {
        try {
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.DATE_ADDED
            )

            val cursor = context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC LIMIT 1"
            )

            cursor?.use {
                if (it.moveToFirst()) {
                    val id = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                    val name = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)) ?: "media_$id"
                    val mimeType = it.getString(it.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)) ?: "image/jpeg"

                    if (id == lastScannedUri) return // Already processed
                    lastScannedUri = id

                    val startTime = System.currentTimeMillis()

                    // Privacy-First: Hash the filename/URI rather than transmitting file contents
                    val fileHash = SecurityUtils.sha256("$id:$name")

                    // On-device evaluation pipeline stub:
                    // In production, downscale thumbnail to 224x224 and run TFLite MobileNet classifier.
                    val isFlagged = evaluateImageSafety(name, mimeType)
                    val confidence = if (isFlagged) 0.92f else 0.05f
                    val durationMs = System.currentTimeMillis() - startTime

                    val result = MediaScanResult(
                        id = UUID.randomUUID().toString(),
                        fileHash = fileHash,
                        fileName = name,
                        mimeType = mimeType,
                        timestamp = System.currentTimeMillis(),
                        isFlaggedExplicit = isFlagged,
                        confidenceScore = confidence,
                        scanDurationMs = durationMs
                    )

                    Log.i(TAG, "Media scan complete: $name (Flagged: ${result.isFlaggedExplicit}, Duration: ${durationMs}ms)")
                    MediaSafetyBuffer.addResult(result)
                }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Media storage permission not granted or revoked: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Error scanning media", e)
        }
    }

    private fun evaluateImageSafety(fileName: String, mimeType: String): Boolean {
        val lower = fileName.lowercase()
        // Heuristic and name check; in full TFLite module, replaced by model inference
        return lower.contains("nsfw") || lower.contains("explicit") || lower.contains("xxx")
    }
}

object MediaSafetyBuffer {
    private val buffer = mutableListOf<MediaScanResult>()

    @Synchronized
    fun addResult(result: MediaScanResult) {
        if (buffer.size >= 200) {
            buffer.removeAt(0)
        }
        buffer.add(result)
    }

    @Synchronized
    fun getRecentResults(): List<MediaScanResult> = buffer.toList()
}
