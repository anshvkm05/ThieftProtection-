package com.example.thieftprotection

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsTriggerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    if (messages.isNullOrEmpty()) return@launch

                    val dataStoreManager = DataStoreManager(context)
                    val triggerPhrase = dataStoreManager.triggerPhraseFlow.first()
                    
                    for (sms in messages) {
                        val body = sms.messageBody
                        if (body != null && body.trim().equals(triggerPhrase.trim(), ignoreCase = true)) {
                            Log.d("SmsTriggerReceiver", "Trigger match found! Starting AntiTheftService.")
                            val serviceIntent = Intent(context, AntiTheftService::class.java)
                            context.startForegroundService(serviceIntent)
                            break
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SmsTriggerReceiver", "Error intercepting SMS trigger phrase", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
