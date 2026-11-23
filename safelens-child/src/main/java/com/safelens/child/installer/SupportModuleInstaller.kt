package com.safelens.child.installer

import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.core.content.FileProvider
import com.safelens.child.SafelensChildApplication.Companion.TAG
import com.safelens.core.ipc.SafelensIpcConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

sealed class SupportModuleStatus {
    object NotInstalled : SupportModuleStatus()
    data class Outdated(
        val installedVersionCode: Long,
        val targetVersionCode: Long,
        val installedVersionName: String
    ) : SupportModuleStatus()
    data class UpToDate(
        val versionCode: Long,
        val versionName: String
    ) : SupportModuleStatus()
}

object SupportModuleInstaller {

    // Default GitHub repository release download URL for Support Module
    const val DEFAULT_GITHUB_APK_URL =
        "https://github.com/safelens-project/safelens/releases/latest/download/safelens-support-debug.apk"

    /**
     * Checks the precise installation and version state of the Device Support Service.
     */
    fun getSupportModuleStatus(
        context: Context,
        targetVersionCode: Long = SafelensIpcConstants.SUPPORT_TARGET_VERSION_CODE.toLong()
    ): SupportModuleStatus {
        return try {
            val packageInfo: PackageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    SafelensIpcConstants.SUPPORT_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(SafelensIpcConstants.SUPPORT_PACKAGE, 0)
            }

            val installedVersionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                packageInfo.versionCode.toLong()
            }
            val installedVersionName = packageInfo.versionName ?: "unknown"

            if (installedVersionCode >= targetVersionCode) {
                SupportModuleStatus.UpToDate(installedVersionCode, installedVersionName)
            } else {
                SupportModuleStatus.Outdated(installedVersionCode, targetVersionCode, installedVersionName)
            }
        } catch (_: PackageManager.NameNotFoundException) {
            SupportModuleStatus.NotInstalled
        }
    }

    fun isSupportModuleInstalled(context: Context): Boolean {
        return getSupportModuleStatus(context) !is SupportModuleStatus.NotInstalled
    }

    fun isSupportModuleUpToDate(context: Context): Boolean {
        return getSupportModuleStatus(context) is SupportModuleStatus.UpToDate
    }

    /**
     * Checks if this application is allowed to request package installations on Android 8.0+.
     */
    fun canRequestPackageInstalls(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.packageManager.canRequestPackageInstalls()
        } else {
            true
        }
    }

    /**
     * Generates the intent to redirect the user to grant Unknown App Sources permission.
     */
    fun createInstallPermissionIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        }
    }

    /**
     * Downloads the Support Module APK from GitHub with real-time percentage progress.
     */
    suspend fun downloadApk(
        context: Context,
        downloadUrl: String = DEFAULT_GITHUB_APK_URL,
        onProgress: (percentage: Int) -> Unit
    ): Result<File> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            val destinationDir = context.externalCacheDir ?: context.cacheDir
            val targetFile = File(destinationDir, "device-support.apk")
            if (targetFile.exists()) {
                targetFile.delete()
            }

            val url = URL(downloadUrl)
            connection = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                connect()
            }

            if (connection.responseCode !in 200..299) {
                return@withContext Result.failure(
                    Exception("Server returned HTTP response ${connection.responseCode}: ${connection.responseMessage}")
                )
            }

            val fileLength = connection.contentLength
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(targetFile)

            val buffer = ByteArray(8192)
            var totalBytesRead = 0L
            var bytesRead: Int

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalBytesRead += bytesRead
                if (fileLength > 0) {
                    val progress = ((totalBytesRead * 100) / fileLength).toInt()
                    withContext(Dispatchers.Main) {
                        onProgress(progress)
                    }
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            Log.i(TAG, "Support APK downloaded successfully to ${targetFile.absolutePath} (${targetFile.length()} bytes)")
            Result.success(targetFile)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to download Support Module APK", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }

    /**
     * Triggers the Android PackageInstaller to install the downloaded Support Module APK.
     */
    fun triggerInstall(context: Context, apkFile: File) {
        val authority = "${context.packageName}.fileprovider"
        val contentUri = FileProvider.getUriForFile(context, authority, apkFile)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }

        context.startActivity(installIntent)
        Log.i(TAG, "Triggered package installation intent for ${apkFile.name}")
    }
}
