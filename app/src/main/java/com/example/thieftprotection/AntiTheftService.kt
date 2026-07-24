package com.example.thieftprotection

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

class AntiTheftService : Service() {

    companion object {
        private const val CHANNEL_ID = "SignalLockAlertChannel"
        private const val NOTIFICATION_ID = 4821
        private val _isRunning = MutableStateFlow(false)
        val isRunning = _isRunning.asStateFlow()
    }

    private val serviceScope = CoroutineScope(Dispatchers.Main)
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private var windowManager: WindowManager? = null
    private var overlayView: FrameLayout? = null
    private var cameraId: String? = null
    private var cameraManager: CameraManager? = null

    override fun onCreate() {
        super.onCreate()
        _isRunning.value = true
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForegroundServiceWithNotification()

        serviceScope.launch {
            val dataStoreManager = DataStoreManager(applicationContext)
            val ttsMessage = dataStoreManager.ttsMessageFlow.first()
            val enableNetworkLocation = dataStoreManager.enableNetworkLocationFlow.first()
            val enableScreenLock = dataStoreManager.enableScreenLockFlow.first()
            val enableSoundAlarm = dataStoreManager.enableSoundAlarmFlow.first()
            val enableFlashStrobe = dataStoreManager.enableFlashStrobeFlow.first()
            val enableOverlay = dataStoreManager.enableOverlayFlow.first()

            // 1. Auto-Activate Wi-Fi, Mobile Data & Location (GPS) if enabled
            if (enableNetworkLocation) {
                NetworkLocationManager.activateNetworkAndLocation(applicationContext)
            }

            // 2. Lock screen if enabled
            if (enableScreenLock) {
                lockScreen()
            }

            // 3. Initialize looping TTS & Volume Override if enabled
            if (enableSoundAlarm) {
                initTextToSpeech(ttsMessage)
                startVolumeMaxLoop()
            }

            // 4. Start Torch Flashing Loop if enabled
            if (enableFlashStrobe) {
                startFlashlightLoop()
            }

            // 5. Draw Fullscreen Security Overlay if enabled
            if (enableOverlay) {
                showFullscreenOverlay()
            }
        }

        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SignalLock Anti-Theft Service",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Keeps Anti-Theft alerting service active in the background"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startForegroundServiceWithNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("SignalLock Active")
            .setContentText("Your anti-theft monitoring and alarm service is active.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(Notification.CATEGORY_ALARM)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun lockScreen() {
        val devicePolicyManager = getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
        val adminComponent = ComponentName(this, SignalLockAdminReceiver::class.java)
        if (devicePolicyManager.isAdminActive(adminComponent)) {
            try {
                devicePolicyManager.lockNow()
            } catch (e: Exception) {
                Log.e("AntiTheftService", "Error calling lockNow()", e)
            }
        } else {
            Log.w("AntiTheftService", "Device Admin is not active. Screen cannot be locked.")
        }
    }

    private fun initTextToSpeech(message: String) {
        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isTtsReady = true
                tts?.language = Locale.US
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                tts?.setAudioAttributes(audioAttributes)

                serviceScope.launch {
                    while (true) {
                        if (isTtsReady) {
                            val params = Bundle().apply {
                                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
                            }
                            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "SignalLockTTS")
                        }
                        delay(6000)
                    }
                }
            } else {
                Log.e("AntiTheftService", "TTS Initialization failed")
            }
        }
    }

    private fun startVolumeMaxLoop() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        serviceScope.launch {
            while (true) {
                try {
                    audioManager.adjustStreamVolume(AudioManager.STREAM_ALARM, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_UNMUTE, 0)
                    audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0)

                    val maxAlarm = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
                    val maxMusic = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                    val maxRing = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)

                    audioManager.setStreamVolume(AudioManager.STREAM_ALARM, maxAlarm, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxMusic, 0)
                    audioManager.setStreamVolume(AudioManager.STREAM_RING, maxRing, 0)
                } catch (e: Exception) {
                    Log.e("AntiTheftService", "Error overriding volumes", e)
                }
                delay(1000)
            }
        }
    }

    private fun startFlashlightLoop() {
        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            cameraId = cameraManager?.cameraIdList?.firstOrNull { id ->
                val characteristics = cameraManager?.getCameraCharacteristics(id)
                characteristics?.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (cameraId != null) {
                serviceScope.launch(Dispatchers.IO) {
                    var toggle = false
                    while (true) {
                        try {
                            cameraManager?.setTorchMode(cameraId!!, toggle)
                            toggle = !toggle
                        } catch (e: Exception) {
                            Log.e("AntiTheftService", "Error setting torch mode", e)
                        }
                        delay(500)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AntiTheftService", "Camera initialization failed", e)
        }
    }

    private fun showFullscreenOverlay() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w("AntiTheftService", "Draw overlay permission is not granted.")
            return
        }

        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        overlayView = FrameLayout(this).apply {
            setBackgroundColor(Color.parseColor("#FA0B0F19"))

            val container = LinearLayout(this.context).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                setPadding(40, 40, 40, 40)
            }

            val textView = TextView(this.context).apply {
                text = "⚠️ SIGNAL LOCKDOWN ACTIVE ⚠️\n\nThis device is locked down.\nTouch options & sound alarms are active."
                setTextColor(Color.WHITE)
                textSize = 20f
                gravity = Gravity.CENTER
            }
            container.addView(textView)

            // Test / Emergency Deactivation Button inside Overlay
            val stopButton = Button(this.context).apply {
                text = "🛑 DEACTIVATE ALARM (END OVERLAY)"
                setTextColor(Color.WHITE)
                setBackgroundColor(Color.parseColor("#C0392B"))
                setPadding(30, 20, 30, 20)
                setOnClickListener {
                    Log.d("AntiTheftService", "Deactivate button clicked inside overlay. Stopping service.")
                    stopSelf()
                }
            }
            val buttonParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 50
            }
            container.addView(stopButton, buttonParams)

            val layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER
            )
            addView(container, layoutParams)
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            } else {
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE
            },
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.TRANSLUCENT
        )

        try {
            windowManager?.addView(overlayView, params)
        } catch (e: Exception) {
            Log.e("AntiTheftService", "Failed to add overlay view", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _isRunning.value = false
        serviceScope.cancel()

        // Turn off torch
        cameraId?.let { id ->
            try {
                cameraManager?.setTorchMode(id, false)
            } catch (e: Exception) {
                Log.e("AntiTheftService", "Failed to turn off torch in onDestroy", e)
            }
        }

        // Remove overlay
        overlayView?.let { view ->
            try {
                windowManager?.removeView(view)
            } catch (e: Exception) {
                Log.e("AntiTheftService", "Failed to remove overlay view", e)
            }
        }

        // Stop TextToSpeech
        tts?.apply {
            stop()
            shutdown()
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
