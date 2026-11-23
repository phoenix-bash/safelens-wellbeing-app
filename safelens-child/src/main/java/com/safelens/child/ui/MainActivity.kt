package com.safelens.child.ui

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.safelens.child.databinding.ActivityMainBinding
import com.safelens.child.notification.NotificationMonitorService
import com.safelens.child.telephony.CallLogObserver
import com.safelens.child.media.MediaSafetyObserver
import com.safelens.child.util.OemBatteryHelper
import com.safelens.core.ipc.SafelensIpcConstants

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

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

        binding.btnInitializeSupport.setOnClickListener {
            initializeSupportSecurityModule()
        }
    }

    private fun initializeSupportSecurityModule() {
        try {
            // Explicit Intent to wake up the headless Support Security Module from Android's stopped state
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

            Toast.makeText(this, "Handshake sent to Safelens Support Module", Toast.LENGTH_LONG).show()
            binding.tvSupportStatus.text = "Status: Initializing / Connected"
            binding.tvSupportStatus.setTextColor(ContextCompat.getColor(this, com.safelens.child.R.color.accent))
        } catch (e: Exception) {
            Toast.makeText(this, "Support Module not installed or accessible: ${e.message}", Toast.LENGTH_LONG).show()
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

        // OEM Detection hint
        if (OemBatteryHelper.isPocoOrXiaomi()) {
            binding.tvOemHint.text = "Poco/Xiaomi device detected. Setting Autostart and disabling MIUI battery killer is critical."
        } else if (OemBatteryHelper.isRealmeOrOppo()) {
            binding.tvOemHint.text = "Realme/Oppo device detected. Background startup and power protection exemption required."
        }
    }
}
