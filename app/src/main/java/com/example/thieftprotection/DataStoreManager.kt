package com.example.thieftprotection

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "signal_lock_prefs")

class DataStoreManager(private val context: Context) {
    companion object {
        val TRIGGER_PHRASE = stringPreferencesKey("trigger_phrase")
        val TTS_MESSAGE = stringPreferencesKey("tts_message")
    }

    val triggerPhraseFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TRIGGER_PHRASE] ?: "SECURE_LOCK"
    }

    val ttsMessageFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[TTS_MESSAGE] ?: "This device is stolen! Police are on the way!"
    }

    suspend fun saveTriggerPhrase(phrase: String) {
        context.dataStore.edit { preferences ->
            preferences[TRIGGER_PHRASE] = phrase
        }
    }

    suspend fun saveTtsMessage(message: String) {
        context.dataStore.edit { preferences ->
            preferences[TTS_MESSAGE] = message
        }
    }
}
