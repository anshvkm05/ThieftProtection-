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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.thieftprotection.ui.theme.ThieftProtectionTheme
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
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = Color(0xFF0B0F19)
                ) { innerPadding ->
                    SignalLockScreen(
                        modifier = Modifier.padding(innerPadding),
                        refreshTrigger = refreshTrigger.value
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignalLockScreen(modifier: Modifier = Modifier, refreshTrigger: Int) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val dataStoreManager = remember { DataStoreManager(context) }

    // Service running state
    val isServiceRunning by AntiTheftService.isRunning.collectAsState()

    // Settings states
    var triggerPhrase by remember { mutableStateOf("SECURE_LOCK") }
    var ttsMessage by remember { mutableStateOf("This device is stolen! Police are on the way!") }

    // Load settings from DataStore
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            dataStoreManager.triggerPhraseFlow.collect { triggerPhrase = it }
        }
        coroutineScope.launch {
            dataStoreManager.ttsMessageFlow.collect { ttsMessage = it }
        }
    }

    // Permission states computed reactively using the refreshTrigger
    val adminComponent = remember { ComponentName(context, SignalLockAdminReceiver::class.java) }
    val devicePolicyManager = remember { context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager }

    var hasSmsPermission by remember { mutableStateOf(false) }
    var hasCameraPermission by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember { mutableStateOf(false) }
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
        hasOverlayPermission = Settings.canDrawOverlays(context)
        hasDeviceAdmin = devicePolicyManager.isAdminActive(adminComponent)
    }

    // Permission request launchers
    val smsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        hasSmsPermission = result[Manifest.permission.RECEIVE_SMS] == true && result[Manifest.permission.READ_SMS] == true
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Header
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "⚡ SIGNAL LOCK",
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF6366F1),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Advanced Anti-Theft Lock & Alert System",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Status Card
        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "SYSTEM STATUS",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(
                                color = if (isServiceRunning) Color(0xFFEF4444) else Color(0xFF10B981),
                                shape = RoundedCornerShape(50)
                            )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isServiceRunning) "ALARM ACTIVE - DEVICE LOCKED" else "MONITORING ACTIVE (STANDBY)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isServiceRunning) Color(0xFFEF4444) else Color(0xFF10B981)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (isServiceRunning) {
                                // Stop Service
                                val serviceIntent = Intent(context, AntiTheftService::class.java)
                                context.stopService(serviceIntent)
                                Toast.makeText(context, "Anti-theft alert deactivated", Toast.LENGTH_SHORT).show()
                            } else {
                                // Test / Start Service
                                if (!hasOverlayPermission || !hasDeviceAdmin) {
                                    Toast.makeText(context, "Please grant Admin and Overlay permissions to test fully", Toast.LENGTH_LONG).show()
                                }
                                val serviceIntent = Intent(context, AntiTheftService::class.java)
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    context.startForegroundService(serviceIntent)
                                } else {
                                    context.startService(serviceIntent)
                                }
                                Toast.makeText(context, "Test Alert Started!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isServiceRunning) Color(0xFF10B981) else Color(0xFFEF4444)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isServiceRunning) "Deactivate Alert" else "Trigger Test Alert",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }

        // Settings Form Card
        Text(
            text = "CONFIGURATION",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = triggerPhrase,
                    onValueChange = { triggerPhrase = it },
                    label = { Text("SMS Trigger Phrase") },
                    placeholder = { Text("e.g. SECURE_LOCK") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF818CF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = ttsMessage,
                    onValueChange = { ttsMessage = it },
                    label = { Text("TTS Alarm Message") },
                    placeholder = { Text("The message spoken repeatedly") },
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedLabelColor = Color(0xFF818CF8),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            dataStoreManager.saveTriggerPhrase(triggerPhrase)
                            dataStoreManager.saveTtsMessage(ttsMessage)
                            Toast.makeText(context, "Settings Saved Successfully!", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save Settings", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Required Permissions Checklist Card
        Text(
            text = "REQUIRED PERMISSIONS",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF6366F1),
            modifier = Modifier
                .align(Alignment.Start)
                .padding(start = 4.dp, bottom = 8.dp)
        )

        Card(
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp)
                .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF111827))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. SMS Permission Row
                PermissionItemRow(
                    title = "Receive & Read SMS",
                    description = "Required to intercept incoming triggers.",
                    isGranted = hasSmsPermission,
                    onRequest = {
                        smsLauncher.launch(
                            arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
                        )
                    }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // 2. Overlay Permission Row
                PermissionItemRow(
                    title = "Draw Over Other Apps",
                    description = "Required to show fullscreen lockdown screen.",
                    isGranted = hasOverlayPermission,
                    onRequest = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // 3. Device Admin Policy
                PermissionItemRow(
                    title = "Device Administrator",
                    description = "Required to trigger immediate screen locking.",
                    isGranted = hasDeviceAdmin,
                    onRequest = {
                        val intent = Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
                            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
                            putExtra(
                                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                                "Activate to allow locking the device automatically on theft detection."
                            )
                        }
                        context.startActivity(intent)
                    }
                )

                HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)

                // 4. Camera Permission (Flash)
                PermissionItemRow(
                    title = "Camera Access (Flash)",
                    description = "Required to strobe back camera light.",
                    isGranted = hasCameraPermission,
                    onRequest = {
                        cameraLauncher.launch(Manifest.permission.CAMERA)
                    }
                )

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    HorizontalDivider(color = Color(0xFF1E293B), thickness = 1.dp)
                    
                    // 5. Post Notifications (Android 13+)
                    PermissionItemRow(
                        title = "Notifications Permission",
                        description = "Required to run foreground alert services.",
                        isGranted = hasNotificationPermission,
                        onRequest = {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun PermissionItemRow(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color(0xFF94A3B8)
            )
        }
        
        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onRequest,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isGranted) Color(0xFF065F46) else Color(0xFF4F46E5),
                disabledContainerColor = Color(0xFF065F46)
            ),
            shape = RoundedCornerShape(8.dp),
            enabled = !isGranted,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.defaultMinSize(minWidth = 80.dp)
        ) {
            Text(
                text = if (isGranted) "Granted" else "Grant",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFF34D399) else Color.White
            )
        }
    }
}