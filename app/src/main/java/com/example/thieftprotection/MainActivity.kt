package com.example.thieftprotection

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.thieftprotection.ui.PermissionInstructionScreen
import com.example.thieftprotection.ui.PermissionStepInfo
import com.example.thieftprotection.ui.PermissionType
import com.example.thieftprotection.ui.theme.*
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val refreshTrigger = mutableStateOf(0)

    override fun onResume() {
        super.onResume()
        refreshTrigger.value += 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ThieftProtectionTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainContainerScreen(refreshTrigger = refreshTrigger.value)
                }
            }
        }
    }
}

@Composable
fun MainContainerScreen(refreshTrigger: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    // Service running state
    val isServiceRunning by AntiTheftService.isRunning.collectAsState()

    // Settings & Feature Toggle states loaded safely via collectAsState
    val savedTriggerPhrase by dataStoreManager.triggerPhraseFlow.collectAsState(initial = "SECURE_LOCK")
    val savedStopPhrase by dataStoreManager.stopPhraseFlow.collectAsState(initial = "STOP_LOCK")
    val savedTtsMessage by dataStoreManager.ttsMessageFlow.collectAsState(initial = "This device is stolen! Police are on the way!")

    val enableNetworkLocation by dataStoreManager.enableNetworkLocationFlow.collectAsState(initial = true)
    val enableSoundAlarm by dataStoreManager.enableSoundAlarmFlow.collectAsState(initial = true)
    val enableFlashStrobe by dataStoreManager.enableFlashStrobeFlow.collectAsState(initial = true)
    val enableOverlay by dataStoreManager.enableOverlayFlow.collectAsState(initial = true)
    val enableScreenLock by dataStoreManager.enableScreenLockFlow.collectAsState(initial = true)

    // Messaging App Toggles
    val listenDefaultSms by dataStoreManager.listenDefaultSmsFlow.collectAsState(initial = true)
    val listenWhatsapp by dataStoreManager.listenWhatsappFlow.collectAsState(initial = true)
    val listenTelegram by dataStoreManager.listenTelegramFlow.collectAsState(initial = true)
    val listenInstagram by dataStoreManager.listenInstagramFlow.collectAsState(initial = true)
    val listenX by dataStoreManager.listenXFlow.collectAsState(initial = true)

    var triggerPhrase by remember(savedTriggerPhrase) { mutableStateOf(savedTriggerPhrase) }
    var stopPhrase by remember(savedStopPhrase) { mutableStateOf(savedStopPhrase) }
    var ttsMessage by remember(savedTtsMessage) { mutableStateOf(savedTtsMessage) }

    // Permission states
    val adminComponent = remember { ComponentName(context, SignalLockAdminReceiver::class.java) }
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
    var hasNotificationListener by remember { mutableStateOf(false) }
    var hasOverlayPermission by remember { mutableStateOf(false) }
    var hasDeviceAdmin by remember { mutableStateOf(false) }

    LaunchedEffect(refreshTrigger) {
        hasSmsPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        hasCameraPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        hasNotificationListener = NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
    }

    // Permission Request Launchers
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasSmsPermission = result[Manifest.permission.RECEIVE_SMS] == true && result[Manifest.permission.READ_SMS] == true
        if (hasSmsPermission) {
            Toast.makeText(context, "SMS permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (granted) {
            Toast.makeText(context, "Camera permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
        if (granted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        }
    }

    // Navigation state: null = Main Dashboard, 0..5 = Permission Instruction Screen step
    var activePermissionStep by remember { mutableStateOf<Int?>(null) }

    // List of permission steps
    val permissionSteps = listOf(
        PermissionStepInfo(
            type = PermissionType.SMS,
            stepIndex = 0,
            totalSteps = 6,
            title = "SMS Interception",
            subtitle = "Detect emergency SMS triggers when lost",
            icon = Icons.Default.Sms,
            whyRequiredText = "SignalLock monitors incoming SMS messages to detect your secret trigger phrase (e.g. 'SECURE_LOCK') when your phone is lost or stolen. This enables remote alarm activation without an internet connection.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to open the system dialog.",
                "Tap 'Allow' on the Android permission prompt.",
                "SignalLock will instantly begin monitoring for emergency SMS commands."
            ),
            isGranted = hasSmsPermission
        ),
        PermissionStepInfo(
            type = PermissionType.NOTIFICATION_LISTENER,
            stepIndex = 1,
            totalSteps = 6,
            title = "Notification Access (RCS, WhatsApp, Telegram, etc)",
            subtitle = "Detect triggers from default SMS & chat apps",
            icon = Icons.Default.Chat,
            whyRequiredText = "Modern Android messaging apps (Google Messages RCS, WhatsApp, Telegram, Instagram, X) receive messages via push notifications. Notification Access allows SignalLock to detect your secret trigger phrase across all chat platforms.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to open Notification Access settings.",
                "Locate SignalLock in the list and toggle to ON.",
                "Tap 'Allow' on the confirmation warning prompt.",
                "WSA / Headless Alt: Run 'adb shell cmd notification allow_listener com.example.thieftprotection/.NotificationTriggerListenerService'"
            ),
            isGranted = hasNotificationListener
        ),
        PermissionStepInfo(
            type = PermissionType.DEVICE_ADMIN,
            stepIndex = 2,
            totalSteps = 6,
            title = "Device Administrator",
            subtitle = "Lock screen instantly upon theft alert",
            icon = Icons.Default.Security,
            whyRequiredText = "Allows SignalLock to execute DevicePolicyManager.lockNow() to lock the phone screen immediately upon receiving a trigger SMS, securing user data from unauthorized access.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to open Device Admin settings.",
                "Review the lock screen capability notice and tap 'Activate'.",
                "WSA / Headless Alt: Run 'adb shell dpm set-active-admin com.example.thieftprotection/.SignalLockAdminReceiver'"
            ),
            isGranted = hasDeviceAdmin
        ),
        PermissionStepInfo(
            type = PermissionType.SYSTEM_OVERLAY,
            stepIndex = 3,
            totalSteps = 6,
            title = "System Alert Overlay",
            subtitle = "Block thief touch & power menu interactions",
            icon = Icons.Default.Lock,
            whyRequiredText = "Draws a persistent full-screen security overlay over lock screens and power options. This prevents thieves from turning off the phone or altering quick settings during an active alarm.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to open Android Special App Access settings.",
                "Locate SignalLock in the app list.",
                "Toggle 'Allow display over other apps' to ON."
            ),
            isGranted = hasOverlayPermission
        ),
        PermissionStepInfo(
            type = PermissionType.CAMERA,
            stepIndex = 4,
            totalSteps = 6,
            title = "Camera Strobe Beacon",
            subtitle = "Flash camera torch repeatedly in dark",
            icon = Icons.Default.FlashlightOn,
            whyRequiredText = "SignalLock toggles the camera flashlight on and off rapidly during an active alarm to create an emergency visual strobe beacon for locating the device in low light.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to trigger camera access request.",
                "Select 'While using the app' or 'Allow'.",
                "Torch strobe alert will be ready."
            ),
            isGranted = hasCameraPermission
        ),
        PermissionStepInfo(
            type = PermissionType.NOTIFICATIONS,
            stepIndex = 5,
            totalSteps = 6,
            title = "Background Notifications",
            subtitle = "Ensure persistent monitoring on Android 13+",
            icon = Icons.Default.Notifications,
            whyRequiredText = "Required on Android 13+ to post foreground service notifications, ensuring SignalLock remains active in background standby without being terminated by OS battery optimization.",
            howToAllowSteps = listOf(
                "Tap 'Grant' below to request notification rights.",
                "Tap 'Allow' on the system popup prompt."
            ),
            isGranted = hasNotificationPermission
        )
    )

    // Helper to launch grant action for a specific step
    val launchGrantAction: (PermissionStepInfo) -> Unit = { step ->
        when (step.type) {
            PermissionType.SMS -> {
                smsLauncher.launch(
                    arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                )
            }
            PermissionType.NOTIFICATION_LISTENER -> {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
            }
            PermissionType.DEVICE_ADMIN -> {
                val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                    putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                    putExtra(
                        DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                        "Enable Device Administrator to lock the screen instantly during a theft alert."
                    )
                }
                context.startActivity(intent)
            }
            PermissionType.SYSTEM_OVERLAY -> {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${context.packageName}")
                )
                context.startActivity(intent)
            }
            PermissionType.CAMERA -> {
                cameraLauncher.launch(Manifest.permission.CAMERA)
            }
            PermissionType.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    Toast.makeText(context, "Notification permission auto-granted on this Android version", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (activePermissionStep != null) {
        val currentStepInfo = permissionSteps[activePermissionStep!!]
        PermissionInstructionScreen(
            stepInfo = currentStepInfo,
            onBack = {
                if (activePermissionStep!! > 0) {
                    activePermissionStep = activePermissionStep!! - 1
                } else {
                    activePermissionStep = null
                }
            },
            onGrant = {
                launchGrantAction(currentStepInfo)
            },
            onNext = {
                if (activePermissionStep!! < permissionSteps.size - 1) {
                    activePermissionStep = activePermissionStep!! + 1
                } else {
                    activePermissionStep = null
                    Toast.makeText(context, "Permission setup completed!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    } else {
        MainDashboardScreen(
            isServiceRunning = isServiceRunning,
            triggerPhrase = triggerPhrase,
            stopPhrase = stopPhrase,
            ttsMessage = ttsMessage,
            enableNetworkLocation = enableNetworkLocation,
            enableSoundAlarm = enableSoundAlarm,
            enableFlashStrobe = enableFlashStrobe,
            enableOverlay = enableOverlay,
            enableScreenLock = enableScreenLock,
            listenDefaultSms = listenDefaultSms,
            listenWhatsapp = listenWhatsapp,
            listenTelegram = listenTelegram,
            listenInstagram = listenInstagram,
            listenX = listenX,
            permissionSteps = permissionSteps,
            hasOverlayPermission = hasOverlayPermission,
            hasDeviceAdmin = hasDeviceAdmin,
            onTriggerPhraseChange = { triggerPhrase = it },
            onStopPhraseChange = { stopPhrase = it },
            onTtsMessageChange = { ttsMessage = it },
            onToggleNetworkLocation = { coroutineScope.launch { dataStoreManager.saveEnableNetworkLocation(it) } },
            onToggleSoundAlarm = { coroutineScope.launch { dataStoreManager.saveEnableSoundAlarm(it) } },
            onToggleFlashStrobe = { coroutineScope.launch { dataStoreManager.saveEnableFlashStrobe(it) } },
            onToggleOverlay = { coroutineScope.launch { dataStoreManager.saveEnableOverlay(it) } },
            onToggleScreenLock = { coroutineScope.launch { dataStoreManager.saveEnableScreenLock(it) } },
            onToggleListenDefaultSms = { coroutineScope.launch { dataStoreManager.saveListenDefaultSms(it) } },
            onToggleListenWhatsapp = { coroutineScope.launch { dataStoreManager.saveListenWhatsapp(it) } },
            onToggleListenTelegram = { coroutineScope.launch { dataStoreManager.saveListenTelegram(it) } },
            onToggleListenInstagram = { coroutineScope.launch { dataStoreManager.saveListenInstagram(it) } },
            onToggleListenX = { coroutineScope.launch { dataStoreManager.saveListenX(it) } },
            onSaveSettings = {
                coroutineScope.launch {
                    dataStoreManager.saveTriggerPhrase(triggerPhrase)
                    dataStoreManager.saveStopPhrase(stopPhrase)
                    dataStoreManager.saveTtsMessage(ttsMessage)
                    Toast.makeText(context, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show()
                }
            },
            onOpenInstructionStep = { stepIndex ->
                activePermissionStep = stepIndex
            },
            onStartPermissionWalkthrough = {
                activePermissionStep = 0
            },
            onToggleTestService = {
                if (isServiceRunning) {
                    val serviceIntent = Intent(context, AntiTheftService::class.java).apply {
                        action = AntiTheftService.ACTION_STOP_ALARM
                    }
                    context.startService(serviceIntent)
                    context.stopService(serviceIntent)
                    Toast.makeText(context, "Anti-theft alert deactivated", Toast.LENGTH_SHORT).show()
                } else {
                    if (!hasOverlayPermission || !hasDeviceAdmin) {
                        Toast.makeText(context, "Grant Admin & Overlay permissions for full lockdown test", Toast.LENGTH_LONG).show()
                    }
                    val serviceIntent = Intent(context, AntiTheftService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                    Toast.makeText(context, "Test Alert Started!", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    isServiceRunning: Boolean,
    triggerPhrase: String,
    stopPhrase: String,
    ttsMessage: String,
    enableNetworkLocation: Boolean,
    enableSoundAlarm: Boolean,
    enableFlashStrobe: Boolean,
    enableOverlay: Boolean,
    enableScreenLock: Boolean,
    listenDefaultSms: Boolean,
    listenWhatsapp: Boolean,
    listenTelegram: Boolean,
    listenInstagram: Boolean,
    listenX: Boolean,
    permissionSteps: List<PermissionStepInfo>,
    hasOverlayPermission: Boolean,
    hasDeviceAdmin: Boolean,
    onTriggerPhraseChange: (String) -> Unit,
    onStopPhraseChange: (String) -> Unit,
    onTtsMessageChange: (String) -> Unit,
    onToggleNetworkLocation: (Boolean) -> Unit,
    onToggleSoundAlarm: (Boolean) -> Unit,
    onToggleFlashStrobe: (Boolean) -> Unit,
    onToggleOverlay: (Boolean) -> Unit,
    onToggleScreenLock: (Boolean) -> Unit,
    onToggleListenDefaultSms: (Boolean) -> Unit,
    onToggleListenWhatsapp: (Boolean) -> Unit,
    onToggleListenTelegram: (Boolean) -> Unit,
    onToggleListenInstagram: (Boolean) -> Unit,
    onToggleListenX: (Boolean) -> Unit,
    onSaveSettings: () -> Unit,
    onOpenInstructionStep: (Int) -> Unit,
    onStartPermissionWalkthrough: () -> Unit,
    onToggleTestService: () -> Unit
) {
    val totalGranted = permissionSteps.count { it.isGranted }
    val totalPermissions = permissionSteps.size

    Scaffold(
        containerColor = BeigeBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Branding Header
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = MossGreenPrimary,
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "SIGNAL LOCK",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MossGreenPrimary,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Anti-Theft Security System",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMutedForest
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 1: Supported Messaging Platforms Toggle Card
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "SUPPORTED MESSAGING APPS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MossGreenSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Select which app messages can trigger or stop the alarm",
                        fontSize = 12.sp,
                        color = TextMutedForest,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    FeatureToggleRow(
                        title = "Default SMS / RCS App",
                        subtitle = "Google Messages, Samsung Messages, standard SMS",
                        icon = Icons.Default.Sms,
                        checked = listenDefaultSms,
                        onCheckedChange = onToggleListenDefaultSms
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "WhatsApp & WhatsApp Business",
                        subtitle = "Detect trigger phrases in WhatsApp chats",
                        icon = Icons.Default.Chat,
                        checked = listenWhatsapp,
                        onCheckedChange = onToggleListenWhatsapp
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Telegram",
                        subtitle = "Detect trigger phrases in Telegram messages",
                        icon = Icons.Default.Send,
                        checked = listenTelegram,
                        onCheckedChange = onToggleListenTelegram
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Instagram Direct",
                        subtitle = "Detect trigger phrases in Instagram DMs",
                        icon = Icons.Default.CameraAlt,
                        checked = listenInstagram,
                        onCheckedChange = onToggleListenInstagram
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "X (formerly Twitter)",
                        subtitle = "Detect trigger phrases in X Direct Messages",
                        icon = Icons.Default.Tag,
                        checked = listenX,
                        onCheckedChange = onToggleListenX
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 2: Feature Customization Toggles Card
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "ALARM FEATURE TOGGLES",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MossGreenSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = "Customize active protections upon SMS/chat alarm trigger",
                        fontSize = 12.sp,
                        color = TextMutedForest,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    FeatureToggleRow(
                        title = "Auto Network & GPS",
                        subtitle = "Activate Wi-Fi, Mobile Data & Location on trigger",
                        icon = Icons.Default.Wifi,
                        checked = enableNetworkLocation,
                        onCheckedChange = onToggleNetworkLocation
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Sound Alarm & TTS",
                        subtitle = "Loop spoken alarm text at maximum volume",
                        icon = Icons.Default.VolumeUp,
                        checked = enableSoundAlarm,
                        onCheckedChange = onToggleSoundAlarm
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Flashlight Strobe Beacon",
                        subtitle = "Rapidly flash back camera torch light",
                        icon = Icons.Default.FlashlightOn,
                        checked = enableFlashStrobe,
                        onCheckedChange = onToggleFlashStrobe
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Security Screen Overlay",
                        subtitle = "Display full-screen touch-blocking overlay",
                        icon = Icons.Default.Lock,
                        checked = enableOverlay,
                        onCheckedChange = onToggleOverlay
                    )

                    HorizontalDivider(color = BeigeBorder, modifier = Modifier.padding(vertical = 4.dp))

                    FeatureToggleRow(
                        title = "Instant Screen Lockdown",
                        subtitle = "Lock screen immediately via Device Admin",
                        icon = Icons.Default.Security,
                        checked = enableScreenLock,
                        onCheckedChange = onToggleScreenLock
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 3: Permissions Overview & Instruction Entry Card
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "REQUIRED PERMISSIONS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MossGreenSecondary,
                                letterSpacing = 0.8.sp
                            )
                            Text(
                                text = "$totalGranted of $totalPermissions Granted",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextDarkForest
                            )
                        }

                        Button(
                            onClick = onStartPermissionWalkthrough,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MossGreenPrimary,
                                contentColor = Color.White
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(text = "Walkthrough", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    permissionSteps.forEachIndexed { index, step ->
                        PermissionItemRow(
                            step = step,
                            onClickDetails = { onOpenInstructionStep(index) }
                        )
                        if (index < permissionSteps.size - 1) {
                            HorizontalDivider(
                                color = BeigeBorder,
                                modifier = Modifier.padding(vertical = 6.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 4: Configuration Card (Trigger & Stop Phrases + TTS Message)
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "MESSAGE TRIGGER & STOP CONFIGURATION",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MossGreenSecondary,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = triggerPhrase,
                        onValueChange = onTriggerPhraseChange,
                        label = { Text("Trigger Phrase (Start Alarm)") },
                        placeholder = { Text("e.g. SECURE_LOCK") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MossGreenPrimary,
                            unfocusedBorderColor = BeigeBorder,
                            focusedLabelColor = MossGreenSecondary,
                            focusedTextColor = TextDarkForest,
                            unfocusedTextColor = TextDarkForest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = stopPhrase,
                        onValueChange = onStopPhraseChange,
                        label = { Text("Stop Phrase (Deactivate Alarm)") },
                        placeholder = { Text("e.g. STOP_LOCK") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MossGreenPrimary,
                            unfocusedBorderColor = BeigeBorder,
                            focusedLabelColor = MossGreenSecondary,
                            focusedTextColor = TextDarkForest,
                            unfocusedTextColor = TextDarkForest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = ttsMessage,
                        onValueChange = onTtsMessageChange,
                        label = { Text("TTS Spoken Alarm Text") },
                        placeholder = { Text("Alarm text spoken repeatedly") },
                        maxLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MossGreenPrimary,
                            unfocusedBorderColor = BeigeBorder,
                            focusedLabelColor = MossGreenSecondary,
                            focusedTextColor = TextDarkForest,
                            unfocusedTextColor = TextDarkForest
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onSaveSettings,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MossGreenPrimary,
                            contentColor = Color.White
                        )
                    ) {
                        Text(text = "Save Configuration", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 5: Security ADB Command Reference
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text(
                        text = "OPTIONAL ADB HARDENING & NOTIFICATION ACCESS",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MossGreenSecondary,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Grant Notification Access and WRITE_SECURE_SETTINGS via ADB:",
                        fontSize = 13.sp,
                        color = TextMutedForest
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        color = BeigeSurface,
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = "adb shell cmd notification allow_listener com.example.thieftprotection/.NotificationTriggerListenerService",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MossGreenPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "adb shell pm grant com.example.thieftprotection android.permission.WRITE_SECURE_SETTINGS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MossGreenPrimary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Card 6: System Status & Manual Alarm Test (At Bottom)
            Card(
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "SYSTEM STATUS & ALARM TEST",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MossGreenSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (isServiceRunning) AlertRed else StatusGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isServiceRunning) "ALARM ACTIVE - DEVICE LOCKED" else "MONITORING ACTIVE (STANDBY)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isServiceRunning) AlertRed else StatusGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = onToggleTestService,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) StatusGreen else AlertRed,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isServiceRunning) "Deactivate Alarm Service" else "Trigger Test Anti-Theft Alert",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FeatureToggleRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (checked) MossGreenLight else BeigeSurface,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) MossGreenPrimary else TextMutedForest,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkForest
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = TextMutedForest
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = MossGreenPrimary,
                uncheckedThumbColor = TextMutedForest,
                uncheckedTrackColor = BeigeSurface
            )
        )
    }
}

@Composable
private fun PermissionItemRow(
    step: PermissionStepInfo,
    onClickDetails: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClickDetails() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = if (step.isGranted) StatusGreenLight else MossGreenLight,
            modifier = Modifier.size(36.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = step.icon,
                    contentDescription = null,
                    tint = if (step.isGranted) StatusGreen else MossGreenPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = step.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkForest
            )
            Text(
                text = if (step.isGranted) "Granted" else "Tap for instructions & grant",
                fontSize = 12.sp,
                color = if (step.isGranted) StatusGreen else WarningAmber
            )
        }

        OutlinedButton(
            onClick = onClickDetails,
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MossGreenSecondary)
        ) {
            Text(
                text = "Guide",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MossGreenPrimary
            )
        }
    }
}