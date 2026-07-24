package com.example.thieftprotection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "signal_lock_prefs")

class DataStoreManager(private val context: Context) {
    companion object {
        val TRIGGER_PHRASE = stringPreferencesKey("trigger_phrase")
        val STOP_PHRASE = stringPreferencesKey("stop_phrase")
        val TTS_MESSAGE = stringPreferencesKey("tts_message")

        val ENABLE_NETWORK_LOCATION = booleanPreferencesKey("enable_network_location")
        val ENABLE_SOUND_ALARM = booleanPreferencesKey("enable_sound_alarm")
        val ENABLE_FLASH_STROBE = booleanPreferencesKey("enable_flash_strobe")
        val ENABLE_OVERLAY = booleanPreferencesKey("enable_overlay")
        val ENABLE_SCREEN_LOCK = booleanPreferencesKey("enable_screen_lock")

        // App-specific trigger listening toggles
        val LISTEN_DEFAULT_SMS = booleanPreferencesKey("listen_default_sms")
        val LISTEN_WHATSAPP = booleanPreferencesKey("listen_whatsapp")
        val LISTEN_TELEGRAM = booleanPreferencesKey("listen_telegram")
        val LISTEN_INSTAGRAM = booleanPreferencesKey("listen_instagram")
        val LISTEN_X = booleanPreferencesKey("listen_x")
    }

    val triggerPhraseFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRIGGER_PHRASE] ?: "SECURE_LOCK"
    }

    val stopPhraseFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[STOP_PHRASE] ?: "STOP_LOCK"
    }

    val ttsMessageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TTS_MESSAGE] ?: "This device is stolen! Police are on the way!"
    }

    val enableNetworkLocationFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_NETWORK_LOCATION] ?: true
    }

    val enableSoundAlarmFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_SOUND_ALARM] ?: true
    }

    val enableFlashStrobeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_FLASH_STROBE] ?: true
    }

    val enableOverlayFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_OVERLAY] ?: true
    }

    val enableScreenLockFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[ENABLE_SCREEN_LOCK] ?: true
    }

    // App listening flows
    val listenDefaultSmsFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTEN_DEFAULT_SMS] ?: true
    }

    val listenWhatsappFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTEN_WHATSAPP] ?: true
    }

    val listenTelegramFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTEN_TELEGRAM] ?: true
    }

    val listenInstagramFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTEN_INSTAGRAM] ?: true
    }

    val listenXFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[LISTEN_X] ?: true
    }

    suspend fun saveTriggerPhrase(phrase: String) {
        context.dataStore.edit { preferences ->
            preferences[TRIGGER_PHRASE] = phrase
        }
    }

    suspend fun saveStopPhrase(phrase: String) {
        context.dataStore.edit { preferences ->
            preferences[STOP_PHRASE] = phrase
        }
    }

    suspend fun saveTtsMessage(message: String) {
        context.dataStore.edit { preferences ->
            preferences[TTS_MESSAGE] = message
        }
    }

    suspend fun saveEnableNetworkLocation(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_NETWORK_LOCATION] = enabled
        }
    }

    suspend fun saveEnableSoundAlarm(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SOUND_ALARM] = enabled
        }
    }

    suspend fun saveEnableFlashStrobe(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_FLASH_STROBE] = enabled
        }
    }

    suspend fun saveEnableOverlay(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_OVERLAY] = enabled
        }
    }

    suspend fun saveEnableScreenLock(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ENABLE_SCREEN_LOCK] = enabled
        }
    }

    // App listening toggle savers
    suspend fun saveListenDefaultSms(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTEN_DEFAULT_SMS] = enabled
        }
    }

    suspend fun saveListenWhatsapp(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTEN_WHATSAPP] = enabled
        }
    }

    suspend fun saveListenTelegram(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTEN_TELEGRAM] = enabled
        }
    }

    suspend fun saveListenInstagram(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTEN_INSTAGRAM] = enabled
        }
    }

    suspend fun saveListenX(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[LISTEN_X] = enabled
        }
    }
}
