package com.amozvz.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amozvz.app.ui.components.PermissionItemCard
import com.amozvz.app.ui.theme.AmozVzTheme
import com.amozvz.app.utils.PermissionHelper

class PermissionsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AmozVzTheme {
                PermissionsScreen(
                    onBackClick = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current

    var hasMic by remember { mutableStateOf(PermissionHelper.hasRecordAudioPermission(context)) }
    var hasOverlay by remember { mutableStateOf(PermissionHelper.hasOverlayPermission(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionHelper.hasAccessibilityPermission(context)) }
    var hasNotification by remember { mutableStateOf(PermissionHelper.hasNotificationPermission(context)) }
    var hasBattery by remember { mutableStateOf(PermissionHelper.isIgnoringBatteryOptimizations(context)) }

    val micLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasMic = granted
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotification = granted
    }

    // Refresh permissions state when returning to screen
    DisposableEffect(Unit) {
        onDispose { }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Permissions & Setup") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "AmozVz requires these permissions to provide seamless voice-to-text dictation across all your apps like Wispr Flow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                PermissionItemCard(
                    title = "Microphone Access",
                    description = "Required to capture your voice dictation and recognize natural speech.",
                    isGranted = hasMic,
                    onGrantClick = {
                        micLauncher.launch(android.Manifest.permission.RECORD_AUDIO)
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Display Over Other Apps (Overlay)",
                    description = "Enables the floating mic widget so you can dictate inside WhatsApp, Gmail, Slack, and notes.",
                    isGranted = hasOverlay,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getOverlayPermissionIntent(context))
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Accessibility Service",
                    description = "Allows AmozVz to automatically type your cleaned, punctuated words directly into active input fields.",
                    isGranted = hasAccessibility,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getAccessibilitySettingsIntent())
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Notifications",
                    description = "Shows recording status and quick stop controls in your notification shade.",
                    isGranted = hasNotification,
                    onGrantClick = {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            notificationLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                        }
                    }
                )
            }

            item {
                PermissionItemCard(
                    title = "Battery Optimization Exemption",
                    description = "Prevents Android from killing the floating widget when running in the background.",
                    isGranted = hasBattery,
                    onGrantClick = {
                        context.startActivity(PermissionHelper.getBatteryOptimizationIntent(context))
                    }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Done")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
