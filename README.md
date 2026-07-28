# 🛡️ SignalLock - Offline Anti-Theft Protection System

**SignalLock** is an open-source, privacy-first, 100% offline Android anti-theft security system. It allows device owners to remotely trigger emergency lockdown alerts, max volume siren alarms, spoken text-to-speech warnings, camera strobe beacons, and automatic connectivity toggles (Mobile Data, GPS Location, Wi-Fi) using secret SMS commands or notification triggers across modern chat applications (Google Messages, WhatsApp, Telegram, Instagram, and X).

---

## 🌟 Key Features & Use Cases

* 📩 **Multi-App Notification & SMS Triggers**: Intercept secret trigger/stop phrases (`SECURE_LOCK` / `STOP_LOCK`) from standard SMS as well as push notifications from WhatsApp, Telegram, Instagram DMs, and X DMs.
* 📶 **Auto Network & Location Activation**: Automatically turns on **Mobile Data**, **GPS Location**, and **Wi-Fi** upon alarm activation to assist in device tracking.
* 🔊 **Max Volume Siren & Optimized TTS Engine**: Overrides silent/DND modes and streams high-volume spoken alarm warnings (e.g., *"This device is stolen! Police are on the way!"*) without stuttering or audio clipping.
* 🔦 **Camera Strobe Beacon**: Rapidly flashes the rear camera flashlight to locate the device in low light.
* 🔒 **Instant Screen Lockdown**: Leverages Android **Device Administrator** policies to execute an immediate screen lock (`lockNow()`).
* 🛑 **Fullscreen Touch-Blocking Security Overlay**: Renders a persistent `SYSTEM_ALERT_WINDOW` overlay over power menus and quick settings to hinder unauthorized interaction.
* 🎨 **Modern Beige & Moss Green UI**: Intuitive Jetpack Compose dashboard with interactive permission walkthroughs and ADB permission status indicators.

---

## 🔒 100% Offline & Privacy-First Philosophy

* 🚫 **Zero Data Collection**: SignalLock collects **NO** personal data, no analytics, no location logs, and no device identifiers.
* 🌐 **No Cloud / External Servers**: SignalLock communicates through local device intents and standard incoming SMS/notifications. It contains zero third-party telemetry, ads, or tracking SDKs.
* 🔑 **Total Local Control**: Your secret trigger phrases and settings remain encrypted locally on your phone via Android Jetpack DataStore.

---

## 🚀 Setup & Installation Guide

### Prerequisites
* Android device running **Android 7.0 (API 24)** or higher.
* Notification Listener and Device Administrator permissions granted for full lockdown features.

---

### Option A: Setup via PC / Laptop (Standard ADB)

If you have access to a computer with Android Debug Bridge (ADB) installed:

1. **Enable Developer Options & USB Debugging**:
   * Open `Settings` ➔ `About Phone` ➔ Tap `Build Number` 7 times.
   * Open `Settings` ➔ `System` ➔ `Developer Options` ➔ Enable `USB Debugging`.
2. **Connect Phone to PC** and open your terminal/command prompt.
3. **Grant Elevated System Permissions**:
   ```bash
   # 1. Grant WRITE_SECURE_SETTINGS (Required for auto-enabling Mobile Data, Wi-Fi & GPS)
   adb shell pm grant com.example.thieftprotection android.permission.WRITE_SECURE_SETTINGS

   # 2. Grant Notification Listener Access (Required for WhatsApp, Telegram, Instagram & X triggers)
   adb shell cmd notification allow_listener com.example.thieftprotection/.NotificationTriggerListenerService

   # 3. Activate Device Administrator (Required for instant screen lockdown)
   adb shell dpm set-active-admin com.example.thieftprotection/.SignalLockAdminReceiver

   # 4. Grant Runtime Location & Nearby Wi-Fi Permissions
   adb shell pm grant com.example.thieftprotection android.permission.ACCESS_FINE_LOCATION
   adb shell pm grant com.example.thieftprotection android.permission.ACCESS_COARSE_LOCATION
   adb shell pm grant com.example.thieftprotection android.permission.NEARBY_WIFI_DEVICES
   ```

---

### Option B: PC-Less Setup (Without a Computer) using Shizuku

If you do **NOT** have access to a computer, you can grant all required system permissions directly on your Android phone using **Shizuku** and a local terminal application (e.g., **aShell** or **LADB**).

#### Step 1: Install & Start Shizuku
1. Install [Shizuku](https://shizuku.rikka.app/) from the Google Play Store or GitHub.
2. Open **Shizuku** on your phone.
3. **On Android 11+ (Wireless Debugging)**:
   * Connect your phone to any Wi-Fi network.
   * Open `Developer Options` ➔ Enable `Wireless Debugging`.
   * Tap `Pair device with pairing code` inside Wireless Debugging settings.
   * Enter the pairing code in the Shizuku notification prompt.
   * Return to Shizuku and tap **Start**.
4. **On Rooted Devices**: Simply tap **Start** under *Start via Root*.

#### Step 2: Install a Shizuku Terminal App (aShell)
1. Install **aShell** (or **LADB** / **Termux with Shizuku**).
2. Open **aShell** and grant it Shizuku access when prompted.

#### Step 3: Run Setup Commands in aShell
Paste and execute the following commands inside **aShell**:

```bash
# Grant WRITE_SECURE_SETTINGS
pm grant com.example.thieftprotection android.permission.WRITE_SECURE_SETTINGS

# Grant Notification Access
cmd notification allow_listener com.example.thieftprotection/.NotificationTriggerListenerService

# Activate Device Administrator
dpm set-active-admin com.example.thieftprotection/.SignalLockAdminReceiver

# Grant Location & Nearby Wi-Fi Runtime Permissions
pm grant com.example.thieftprotection android.permission.ACCESS_FINE_LOCATION
pm grant com.example.thieftprotection android.permission.ACCESS_COARSE_LOCATION
pm grant com.example.thieftprotection android.permission.NEARBY_WIFI_DEVICES
```

Once executed, open **SignalLock**—the dashboard will display green status badges (`✓ Shell Granted`) confirming full elevation!

---

## 🔮 Future Scope & Roadmap

We are actively researching and planning advanced anti-theft defense mechanisms:

1. 📶 **Hardware-Level Direct Wi-Fi Force**: Overcoming OS vendor-specific restrictions on Android 10+ to guarantee immediate Wi-Fi hardware state changes across all OEM ROMs.
2. 🤖 **AI/ML Snatch Detection Model**: Integrating a lightweight, local on-device machine learning model (or Android System Theft Detection API) analyzing accelerometer and gyroscope patterns to detect sudden phone snatching from a user's hand or pocket, automatically initiating an emergency lockdown.
3. 🚨 **SIM Ejection Defense & Emergency Wi-Fi Brute-Force**:
   * If a thief ejects the SIM card, SignalLock will continuously scan for nearby open Wi-Fi networks or attempt emergency connection routines using common public Wi-Fi credentials to maintain internet connectivity so the owner can trace the device location.
4. 🔒 **Anti-Power-Off Protection**: Preventing thieves from invoking the physical hardware power menu or forcing a shutdown while the device is in active lockdown mode.

---

## 🤝 How to Contribute

Contributions, bug reports, and feature proposals are welcome! Follow these steps to get started:

1. **Fork the Repository**:
   Click the **Fork** button at the top right of this repository.

2. **Clone your Fork**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/ThieftProtection.git
   cd ThieftProtection
   ```

3. **Open in Android Studio**:
   * Open Android Studio (Ladybug or newer recommended).
   * Sync the Gradle project dependencies (`Java 11`, `Gradle 8.x`, `Compose Compiler`).

4. **Create a Feature Branch**:
   ```bash
   git checkout -b feature/your-feature-name
   ```

5. **Commit & Push your Changes**:
   ```bash
   git add .
   git commit -m "Add: detailed description of your contribution"
   git push origin feature/your-feature-name
   ```

6. **Submit a Pull Request (PR)**:
   Open a Pull Request against the `main` branch with a summary of changes, motivation, and verification test results on physical devices or emulators.

---

## 📜 License

This project is licensed under the [MIT License](LICENSE).
