package com.example.thieftprotection

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.util.Log

object NetworkLocationManager {
    private const val TAG = "NetworkLocationManager"

    fun activateNetworkAndLocation(context: Context) {
        val contentResolver = context.contentResolver

        Log.d(TAG, "Starting activateNetworkAndLocation...")

        // 1. Update Settings.Global & Settings.Secure keys for Wi-Fi
        try {
            Settings.Global.putInt(contentResolver, "wifi_on", 1)
            Settings.Global.putInt(contentResolver, Settings.Global.WIFI_ON, 1)
            Settings.Global.putInt(contentResolver, "wifi", 1)
            Settings.Global.putInt(contentResolver, "wifi_state", 3)
            Settings.Global.putInt(contentResolver, "wifi_saved_state", 1)
            Settings.Secure.putInt(contentResolver, "wifi_on", 1)
            Log.d(TAG, "Updated all wifi Global & Secure settings keys to 1")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update wifi Global/Secure settings keys", e)
        }

        // 2. Enable Wi-Fi via WifiManager API + Reflection
        val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager
        try {
            if (wifiManager != null) {
                try {
                    @Suppress("DEPRECATION")
                    wifiManager.isWifiEnabled = true
                    Log.d(TAG, "wifiManager.isWifiEnabled set to true")
                } catch (e: Exception) {
                    Log.e(TAG, "wifiManager.isWifiEnabled = true failed", e)
                }

                try {
                    val method = wifiManager.javaClass.getMethod("setWifiEnabled", Boolean::class.javaPrimitiveType)
                    method.isAccessible = true
                    method.invoke(wifiManager, true)
                    Log.d(TAG, "wifiManager.setWifiEnabled(true) invoked via reflection")
                } catch (e: Exception) {
                    Log.e(TAG, "Reflection setWifiEnabled failed", e)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enable Wi-Fi via WifiManager API", e)
        }

        // 3. Request Wi-Fi network connection via ConnectivityManager
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            if (connectivityManager != null) {
                val request = NetworkRequest.Builder()
                    .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
                    .build()
                connectivityManager.requestNetwork(request, object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        Log.d(TAG, "Wi-Fi network became available via ConnectivityManager request!")
                    }
                })
                Log.d(TAG, "ConnectivityManager.requestNetwork(WIFI) executed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "ConnectivityManager requestNetwork failed", e)
        }

        // 4. Fallback for Android 10+ (API 29+): launch Wi-Fi Settings Panel overlay if Wi-Fi remains disabled
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && wifiManager != null && !wifiManager.isWifiEnabled) {
            try {
                val panelIntent = Intent(Settings.Panel.ACTION_WIFI).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(panelIntent)
                Log.d(TAG, "Launched Settings.Panel.ACTION_WIFI overlay panel for Android 10+")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to launch Settings.Panel.ACTION_WIFI", e)
            }
        }

        // 5. Enable Mobile Data
        try {
            Settings.Global.putInt(contentResolver, "mobile_data", 1)
            Settings.Global.putInt(contentResolver, "mobile_data0", 1)
            Settings.Global.putInt(contentResolver, "mobile_data1", 1)
            Settings.Global.putInt(contentResolver, "data_roaming", 1)
            Log.d(TAG, "mobile_data updated via Settings.Global")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update mobile_data via Secure Settings", e)
        }

        // 6. Enable Location (GPS)
        try {
            @Suppress("DEPRECATION")
            Settings.Secure.putInt(
                contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_HIGH_ACCURACY
            )
            Settings.Secure.putString(
                contentResolver,
                Settings.Secure.LOCATION_PROVIDERS_ALLOWED,
                "+gps,+network"
            )
            Log.d(TAG, "LOCATION_MODE_HIGH_ACCURACY updated via Settings.Secure")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update location_mode via Secure Settings", e)
        }
    }
}
