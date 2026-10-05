package com.amozvz.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amozvz.app.AmozVzApplication
import com.amozvz.app.data.models.TranscriptionResult
import com.amozvz.app.engine.OnDeviceSpeechRecognizer
import com.amozvz.app.ui.components.DictationModeSelector
import com.amozvz.app.ui.components.WaveformVisualizer
import com.amozvz.app.ui.theme.*
import com.amozvz.app.utils.ClipboardHelper
import com.amozvz.app.utils.PermissionHelper
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AmozVzTheme {
                MainDashboardScreen(
                    onOpenPermissions = {
                        startActivity(Intent(this, PermissionsActivity::class.java))
                    },
                    onOpenSettings = {
                        startActivity(Intent(this, SettingsActivity::class.java))
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainDashboardScreen(
    onOpenPermissions: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    val app = remember { context.applicationContext as AmozVzApplication }
    val prefs = remember { app.preferencesManager }
    val engine = remember { app.engine }
    val scope = rememberCoroutineScope()

    var selectedMode by remember { mutableStateOf(prefs.dictationMode) }
    var selectedInputTab by remember { mutableIntStateOf(0) } // 0 = Voice, 1 = Paste Text
    var pastedRawText by remember { mutableStateOf("") }

    var isListening by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var lastResult by remember { mutableStateOf<TranscriptionResult?>(null) }
    var statusMessage by remember { mutableStateOf("Ready to post-process speech") }

    val speechRecognizer = remember {
        OnDeviceSpeechRecognizer(context).apply {
            onFinalResult = { raw ->
                isListening = false
                isProcessing = true
                statusMessage = "Post-processing speech..."
                scope.launch {
                    val result = engine.processSpeechText(raw)
                    lastResult = result
                    isProcessing = false
                    statusMessage = "Speech finalized & copied to clipboard! 📋"
                    ClipboardHelper.copyToClipboard(context, result.cleanedText, showToast = true)
                }
            }
            onError = { error ->
                isListening = false
                isProcessing = false
                statusMessage = error
            }
        }
    }

    val hasMicPermission = remember(context) {
        PermissionHelper.hasRecordAudioPermission(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(GreenSuccess)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("AmozVz Post-Processor")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenPermissions) {
                        Icon(Icons.Default.Security, contentDescription = "Permissions")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Safe Permission Banner (only if missing microphone in voice tab)
            if (!hasMicPermission && selectedInputTab == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Mic,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Microphone Access Needed", style = MaterialTheme.typography.titleSmall)
                                Text("Only needed for live voice recording.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                            Button(
                                onClick = onOpenPermissions,
                                colors = ButtonDefaults.buttonColors(containerColor = AccentPurple)
                            ) {
                                Text("Grant")
                            }
                        }
                    }
                }
            }

            // Input Selection TabRow (Voice vs Paste Text)
            item {
                TabRow(
                    selectedTabIndex = selectedInputTab,
                    modifier = Modifier.clip(RoundedCornerShape(12.dp)),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Tab(
                        selected = selectedInputTab == 0,
                        onClick = { selectedInputTab = 0 },
                        text = { Text("🎙️ Voice Dictate") }
                    )
                    Tab(
                        selected = selectedInputTab == 1,
                        onClick = { selectedInputTab = 1 },
                        text = { Text("📝 Paste Speech") }
                    )
                }
            }

            // Context Mode Selector Chips (Auto, Email, Chat, Code, Lists)
            item {
                Text("Target Formatting Context", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(4.dp))
                DictationModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = { mode ->
                        selectedMode = mode
                        prefs.dictationMode = mode
                    }
                )
            }

            // Tab 0: Voice Dictation Pad
            if (selectedInputTab == 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Voice Dictation Pad", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Speak naturally with hesitations or self-corrections. They will be resolved cleanly.",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            WaveformVisualizer(isRecording = isListening)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = statusMessage,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isListening) MicRecordingRed else TextSecondary
                            )

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (!PermissionHelper.hasRecordAudioPermission(context)) {
                                        onOpenPermissions()
                                        return@Button
                                    }

                                    if (!isListening) {
                                        isListening = true
                                        statusMessage = "Listening... Speak naturally"
                                        speechRecognizer.startListening()
                                    } else {
                                        speechRecognizer.stopListening()
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isListening) MicRecordingRed else AccentPurple
                                )
                            ) {
                                Icon(
                                    imageVector = if (isListening) Icons.Default.Stop else Icons.Default.Mic,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (isListening) "Stop & Finalize Text" else "Start Speaking")
                            }
                        }
                    }
                }
            }

            // Tab 1: Paste Raw Transcription Pad
            if (selectedInputTab == 1) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Raw Spoken Speech Input", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Paste unedited transcription (e.g. \"Let's meet at 4, wait, no, 5 PM\")",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            OutlinedTextField(
                                value = pastedRawText,
                                onValueChange = { pastedRawText = it },
                                placeholder = { Text("Paste raw speech transcription here...") },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp),
                                shape = RoundedCornerShape(12.dp)
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (pastedRawText.isNotBlank()) {
                                            isProcessing = true
                                            scope.launch {
                                                val result = engine.processSpeechText(pastedRawText)
                                                lastResult = result
                                                isProcessing = false
                                                ClipboardHelper.copyToClipboard(context, result.cleanedText, showToast = true)
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(48.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentPurple),
                                    enabled = pastedRawText.isNotBlank() && !isProcessing
                                ) {
                                    Icon(Icons.Default.AutoFixHigh, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isProcessing) "Post-Processing..." else "Clean & Finalize Text")
                                }

                                if (pastedRawText.isNotBlank()) {
                                    OutlinedButton(
                                        onClick = { pastedRawText = "" },
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text("Clear")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Results Card - Displaying ONLY the finalized text (Rule 6)
            item {
                AnimatedVisibility(visible = lastResult != null) {
                    lastResult?.let { result ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Finalized Written Text", style = MaterialTheme.typography.titleMedium)
                                    Row {
                                        IconButton(onClick = {
                                            ClipboardHelper.copyToClipboard(context, result.cleanedText)
                                        }) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                                        }
                                        IconButton(onClick = {
                                            PermissionHelper.shareText(context, result.cleanedText)
                                        }) {
                                            Icon(Icons.Default.Share, contentDescription = "Share")
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(10.dp),
                                    color = MaterialTheme.colorScheme.surface
                                ) {
                                    Text(
                                        text = result.cleanedText,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = Color.White,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    text = "Raw Input: \"${result.rawText}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = TextSecondary
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${result.hesitationsRemovedCount} fillers removed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentTeal
                                    )
                                    Text(
                                        text = "${result.correctionsResolvedCount} corrections resolved",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = AccentPurple
                                    )
                                    Text(
                                        text = "${result.processingTimeMs}ms",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
