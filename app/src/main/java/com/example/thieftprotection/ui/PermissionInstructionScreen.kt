package com.example.thieftprotection.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.thieftprotection.ui.theme.*

enum class PermissionType {
    SMS,
    DEVICE_ADMIN,
    SYSTEM_OVERLAY,
    CAMERA,
    NOTIFICATIONS,
    NOTIFICATION_LISTENER
}

data class PermissionStepInfo(
    val type: PermissionType,
    val stepIndex: Int,
    val totalSteps: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val whyRequiredText: String,
    val howToAllowSteps: List<String>,
    val isGranted: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionInstructionScreen(
    stepInfo: PermissionStepInfo,
    onBack: () -> Unit,
    onGrant: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        containerColor = BeigeBackground,
        bottomBar = {
            PermissionBottomNavigationBar(
                isGranted = stepInfo.isGranted,
                isLastStep = stepInfo.stepIndex == stepInfo.totalSteps - 1,
                onBack = onBack,
                onGrant = onGrant,
                onNext = onNext
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header & Step Indicator
            StepHeader(
                currentStep = stepInfo.stepIndex + 1,
                totalSteps = stepInfo.totalSteps
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Main Permission Icon Badge & Status
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(if (stepInfo.isGranted) StatusGreenLight else MossGreenLight)
                    .border(
                        width = 2.dp,
                        color = if (stepInfo.isGranted) StatusGreen else MossGreenSecondary,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = stepInfo.icon,
                    contentDescription = stepInfo.title,
                    tint = if (stepInfo.isGranted) StatusGreen else MossGreenPrimary,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = stepInfo.title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextDarkForest
            )

            Text(
                text = stepInfo.subtitle,
                fontSize = 14.sp,
                color = TextMutedForest,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Granted Status Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (stepInfo.isGranted) StatusGreenLight else WarningAmberLight,
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    if (stepInfo.isGranted) StatusGreen else WarningAmber
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (stepInfo.isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (stepInfo.isGranted) StatusGreen else WarningAmber,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (stepInfo.isGranted) "Permission Granted" else "Action Required",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (stepInfo.isGranted) StatusGreen else WarningAmber
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Card 1: Why is this permission required?
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MossGreenLight,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "?",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MossGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "WHY IS THIS REQUIRED?",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MossGreenSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stepInfo.whyRequiredText,
                        fontSize = 14.sp,
                        color = TextDarkForest,
                        lineHeight = 20.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Card 2: How to allow this permission?
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = BeigeCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = MossGreenLight,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "!",
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 16.sp,
                                    color = MossGreenPrimary
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "HOW TO ALLOW IT",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MossGreenSecondary,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    stepInfo.howToAllowSteps.forEachIndexed { index, instruction ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MossGreenPrimary,
                                modifier = Modifier.size(22.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "${index + 1}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = instruction,
                                fontSize = 14.sp,
                                color = TextDarkForest,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepHeader(currentStep: Int, totalSteps: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "PERMISSION INSTRUCTION ($currentStep / $totalSteps)",
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MossGreenSecondary,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            for (i in 1..totalSteps) {
                Box(
                    modifier = Modifier
                        .height(6.dp)
                        .weight(1f)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (i <= currentStep) MossGreenPrimary else BeigeBorder
                        )
                )
            }
        }
    }
}

@Composable
private fun PermissionBottomNavigationBar(
    isGranted: Boolean,
    isLastStep: Boolean,
    onBack: () -> Unit,
    onGrant: () -> Unit,
    onNext: () -> Unit
) {
    Surface(
        color = BeigeSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, BeigeBorder),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Button 1: Back
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MossGreenSecondary),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MossGreenPrimary)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = "Back", fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Button 2: Grant Permission (Main Action)
            Button(
                onClick = onGrant,
                modifier = Modifier.weight(1.3f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isGranted) StatusGreen else MossGreenPrimary,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Security,
                    contentDescription = "Grant Permission",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isGranted) "Re-Grant" else "Grant",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Button 3: Next
            Button(
                onClick = onNext,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MossGreenSecondary,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isLastStep) "Done" else "Next",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Next",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
