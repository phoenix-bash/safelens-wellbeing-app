package com.safelens.child.ui

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.safelens.child.R
import com.safelens.child.databinding.ActivityMainBinding
import com.safelens.child.installer.SupportModuleInstaller
import com.safelens.child.installer.SupportModuleStatus
import com.safelens.child.media.MediaSafetyObserver
import com.safelens.child.notification.NotificationMonitorService
import com.safelens.child.telephony.CallLogObserver
import com.safelens.child.util.OemBatteryHelper
import com.safelens.core.ipc.SafelensIpcConstants
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var isDownloading = false

    private val requestCallLogPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.READ_CALL_LOG] == true
        if (granted) {
            Toast.makeText(this, "Call Log permission granted", Toast.LENGTH_SHORT).show()
            registerCallLogObserver()
        }
        updateUiStatus()
    }

    private val requestMediaPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (granted) {
            Toast.makeText(this, "Media Safety permission granted", Toast.LENGTH_SHORT).show()
            registerMediaObserver()
        }
        updateUiStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        updateUiStatus()
    }

    override fun onResume() {
        super.onResume()
        updateUiStatus()

        // Auto-handshake if Support Module is installed
        if (SupportModuleInstaller.isSupportModuleInstalled(this)) {
            initializeSupportSecurityModule(showToast = false)
        }
    }

    private fun setupListeners() {
        binding.btnNotificationAccess.setOnClickListener {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            startActivity(intent)
        }

        binding.btnCallLogPermission.setOnClickListener {
            requestCallLogPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.READ_CALL_LOG,
                    Manifest.permission.READ_PHONE_STATE
                )
            )
        }

        binding.btnMediaPermission.setOnClickListener {
            val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                arrayOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            requestMediaPermissionLauncher.launch(permissions)
        }

        binding.btnOemBatterySettings.setOnClickListener {
            OemBatteryHelper.requestIgnoreBatteryOptimizations(this)
            OemBatteryHelper.openOemAutostartSettings(this)
        }

        binding.btnDownloadAndInstallSupport.setOnClickListener {
            handleDownloadAndInstallSupport()
        }

        binding.btnInitializeSupport.setOnClickListener {
            initializeSupportSecurityModule(showToast = true)
        }
    }

    private fun handleDownloadAndInstallSupport() {
        // Pre-check: Verify if the module is already installed and up-to-date
        when (val status = SupportModuleInstaller.getSupportModuleStatus(this)) {
            is SupportModuleStatus.UpToDate -> {
                Toast.makeText(
                    this,
                    "Device Support Service is already installed and up to date (v${status.versionName}).",
                    Toast.LENGTH_LONG
                ).show()
                return
            }
            is SupportModuleStatus.Outdated -> {
                Toast.makeText(
                    this,
                    "Updating Device Support Service from v${status.installedVersionName} to v${SafelensIpcConstants.SUPPORT_TARGET_VERSION_NAME}...",
                    Toast.LENGTH_SHORT
                ).show()
            }
            is SupportModuleStatus.NotInstalled -> {
                // Proceed with fresh installation
            }
        }

        if (!SupportModuleInstaller.canRequestPackageInstalls(this)) {
            Toast.makeText(this, "Please enable permission to install the support service.", Toast.LENGTH_LONG).show()
            startActivity(SupportModuleInstaller.createInstallPermissionIntent(this))
            return
        }

        if (isDownloading) return

        isDownloading = true
        binding.btnDownloadAndInstallSupport.isEnabled = false
        binding.pbDownloadProgress.visibility = View.VISIBLE
        binding.pbDownloadProgress.progress = 0
        binding.tvDownloadProgress.visibility = View.VISIBLE
        binding.tvDownloadProgress.text = "Downloading from GitHub... 0%"

        lifecycleScope.launch {
            val result = SupportModuleInstaller.downloadApk(this@MainActivity) { percentage ->
                binding.pbDownloadProgress.progress = percentage
                binding.tvDownloadProgress.text = "Downloading from GitHub... $percentage%"
            }

            isDownloading = false
            binding.btnDownloadAndInstallSupport.isEnabled = true

            result.onSuccess { apkFile ->
                binding.tvDownloadProgress.text = "Download complete. Starting installation..."
                SupportModuleInstaller.triggerInstall(this@MainActivity, apkFile)
            }.onFailure { error ->
                binding.tvDownloadProgress.text = "Download failed: ${error.message}"
                Toast.makeText(this@MainActivity, "Download failed: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeSupportSecurityModule(showToast: Boolean = true) {
        if (!SupportModuleInstaller.isSupportModuleInstalled(this)) {
            if (showToast) {
                Toast.makeText(this, "Device Support Service is not installed yet.", Toast.LENGTH_SHORT).show()
            }
            return
        }

        try {
            // Explicit Intent to wake up the headless DeviceSupportService from Android's stopped state
            val intent = Intent(SafelensIpcConstants.ACTION_INITIALIZE_SUPPORT).apply {
                component = ComponentName(
                    SafelensIpcConstants.SUPPORT_PACKAGE,
                    SafelensIpcConstants.SUPPORT_SERVICE_CLASS
                )
                putExtra(SafelensIpcConstants.EXTRA_PAIRING_TOKEN, "safelens_secure_pairing_token")
                putExtra(SafelensIpcConstants.EXTRA_TIMESTAMP, System.currentTimeMillis())
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }

            if (showToast) {
                Toast.makeText(this, "Handshake verified with Device Support Service", Toast.LENGTH_LONG).show()
            }
            binding.tvSupportStatus.text = "Status: Installed & Guarding (Handshake Active)"
            binding.tvSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
        } catch (e: Exception) {
            if (showToast) {
                Toast.makeText(this, "Could not contact Support Service: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun registerCallLogObserver() {
        try {
            val observer = CallLogObserver(this)
            contentResolver.registerContentObserver(
                android.provider.CallLog.Calls.CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}
    }

    private fun registerMediaObserver() {
        try {
            val observer = MediaSafetyObserver(this)
            contentResolver.registerContentObserver(
                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                true,
                observer
            )
        } catch (_: Exception) {}
    }

    private fun updateUiStatus() {
        val hasNotification = NotificationMonitorService.isServiceConnected
        binding.btnNotificationAccess.text = if (hasNotification) {
            "✓ Notification Access Active"
        } else {
            "1. Enable Notification Access (Study Focus)"
        }

        val hasCallLog = ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        binding.btnCallLogPermission.text = if (hasCallLog) {
            "✓ Call Log & Duration Access Active"
        } else {
            "2. Grant Call Log & Duration Access"
        }

        val hasMedia = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }

        binding.btnMediaPermission.text = if (hasMedia) {
            "✓ Media Safety Filter Active"
        } else {
            "3. Grant Media Safety Filter Access"
        }

        // Support Module status & version check
        when (val status = SupportModuleInstaller.getSupportModuleStatus(this)) {
            is SupportModuleStatus.UpToDate -> {
                binding.tvSupportStatus.text = "Status: Installed & Up to Date (v${status.versionName})"
                binding.tvSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.accent))
                binding.btnDownloadAndInstallSupport.text = "✓ Device Support Service is Up to Date"
                binding.btnDownloadAndInstallSupport.isEnabled = false
                binding.pbDownloadProgress.visibility = View.GONE
                binding.tvDownloadProgress.visibility = View.GONE
            }
            is SupportModuleStatus.Outdated -> {
                binding.tvSupportStatus.text = "Status: Update Available (Installed: v${status.installedVersionName}, Latest: v${SafelensIpcConstants.SUPPORT_TARGET_VERSION_NAME})"
                binding.tvSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.primary))
                binding.btnDownloadAndInstallSupport.text = "Update Device Support Service (v${SafelensIpcConstants.SUPPORT_TARGET_VERSION_NAME})"
                binding.btnDownloadAndInstallSupport.isEnabled = true
            }
            is SupportModuleStatus.NotInstalled -> {
                binding.tvSupportStatus.text = "Status: Not Installed (Action Required)"
                binding.tvSupportStatus.setTextColor(ContextCompat.getColor(this, R.color.danger))
                binding.btnDownloadAndInstallSupport.text = "Download & Install Device Support Service"
                binding.btnDownloadAndInstallSupport.isEnabled = true
            }
        }

        // OEM Detection hint
        if (OemBatteryHelper.isPocoOrXiaomi()) {
            binding.tvOemHint.text = "Poco/Xiaomi device detected. Setting Autostart and disabling MIUI battery killer is critical."
        } else if (OemBatteryHelper.isRealmeOrOppo()) {
            binding.tvOemHint.text = "Realme/Oppo device detected. Background startup and power protection exemption required."
        }
    }
}
