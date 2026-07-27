package com.example.thieftprotection

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NotificationTriggerListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationListener"

        // Package identifiers
        val DEFAULT_SMS_PACKAGES = setOf(
            "com.google.android.apps.messaging",
            "com.android.mms",
            "com.samsung.android.messaging",
            "com.google.android.messaging",
            "com.htc.sense.mms",
            "com.sonyericsson.conversations"
        )
        val WHATSAPP_PACKAGES = setOf("com.whatsapp", "com.whatsapp.w4b")
        val TELEGRAM_PACKAGES = setOf("org.telegram.messenger", "org.telegram.messenger.web", "org.telegram.plus")
        val INSTAGRAM_PACKAGES = setOf("com.instagram.android")
        val X_PACKAGES = setOf("com.twitter.android")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""
        val fullContent = "$title $text $bigText"

        if (fullContent.isBlank()) return

        serviceScope.launch {
            try {
                val dataStoreManager = DataStoreManager(applicationContext)
                val triggerPhrase = dataStoreManager.triggerPhraseFlow.first()
                val stopPhrase = dataStoreManager.stopPhraseFlow.first()

                val listenDefaultSms = dataStoreManager.listenDefaultSmsFlow.first()
                val listenWhatsapp = dataStoreManager.listenWhatsappFlow.first()
                val listenTelegram = dataStoreManager.listenTelegramFlow.first()
                val listenInstagram = dataStoreManager.listenInstagramFlow.first()
                val listenX = dataStoreManager.listenXFlow.first()

                val isAllowedPackage = when {
                    listenDefaultSms && (DEFAULT_SMS_PACKAGES.contains(packageName) || packageName.contains("mms") || packageName.contains("sms") || packageName.contains("messaging")) -> true
                    listenWhatsapp && WHATSAPP_PACKAGES.contains(packageName) -> true
                    listenTelegram && TELEGRAM_PACKAGES.contains(packageName) -> true
                    listenInstagram && INSTAGRAM_PACKAGES.contains(packageName) -> true
                    listenX && X_PACKAGES.contains(packageName) -> true
                    else -> false
                }

                if (!isAllowedPackage) return@launch

                Log.d(TAG, "Inspecting notification from $packageName: $fullContent")

                val cleanContent = fullContent.lowercase()
                val cleanTrigger = triggerPhrase.trim().lowercase()
                val cleanStop = stopPhrase.trim().lowercase()

                if (cleanStop.isNotEmpty() && cleanContent.contains(cleanStop)) {
                    Log.d(TAG, "Stop phrase detected in notification from $packageName! Deactivating AntiTheftService.")
                    val serviceIntent = Intent(applicationContext, AntiTheftService::class.java).apply {
                        action = AntiTheftService.ACTION_STOP_ALARM
                    }
                    startService(serviceIntent)
                    stopService(serviceIntent)
                } else if (cleanTrigger.isNotEmpty() && cleanContent.contains(cleanTrigger)) {
                    Log.d(TAG, "Trigger phrase detected in notification from $packageName! Starting AntiTheftService.")
                    val serviceIntent = Intent(applicationContext, AntiTheftService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(serviceIntent)
                    } else {
                        startService(serviceIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing notification trigger", e)
            }
        }
    }
}
