package com.example.thieftprotection

import android.content.Context
import android.net.wifi.WifiManager
import android.provider.Settings
import android.util.Log

object NetworkLocationManager {
    private const val TAG = "NetworkLocationManager"

    fun activateNetworkAndLocation(context: Context) {
        val contentResolver = context.contentResolver

        // 1. Enable Wi-Fi
        try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
            if (wifiManager != null && !wifiManager.isWifiEnabled) {
                @Suppress("DEPRECATION")
                wifiManager.isWifiEnabled = true
                Log.d(TAG, "Wi-Fi enabled via WifiManager")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable Wi-Fi via WifiManager API", e)
        }

        try {
            Settings.Global.putInt(contentResolver, "wifi_on", 1)
            Log.d(TAG, "wifi_on updated via Settings.Global")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update wifi_on via Secure Settings", e)
        }

        // 2. Enable Mobile Data
        try {
            Settings.Global.putInt(contentResolver, "mobile_data", 1)
            Log.d(TAG, "mobile_data updated via Settings.Global")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update mobile_data via Secure Settings", e)
        }

        // 3. Enable Location (GPS)
        try {
            @Suppress("DEPRECATION")
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            )
            Log.d(TAG, "LOCATION_MODE_HIGH_ACCURACY updated via Settings.Secure")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location_mode via Secure Settings", e)
        }
    }
}
