package com.amozvz.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.amozvz.app.AmozVzApplication
import com.amozvz.app.data.models.AIProvider
import com.amozvz.app.data.models.DictationMode
import com.amozvz.app.ui.components.DictationModeSelector
import com.amozvz.app.ui.theme.AmozVzTheme

class SettingsActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AmozVzTheme {
                SettingsScreen(onBackClick = { finish() })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    val prefs = remember { (context.applicationContext as AmozVzApplication).preferencesManager }

    var selectedProvider by remember { mutableStateOf(prefs.aiProvider) }
    var selectedMode by remember { mutableStateOf(prefs.dictationMode) }
    var serverUrl by remember { mutableStateOf(prefs.serverUrl) }
    var apiKey by remember { mutableStateOf(prefs.apiKey) }
    var stripHesitations by remember { mutableStateOf(prefs.stripHesitations) }
    var resolveCorrections by remember { mutableStateOf(prefs.resolveCorrections) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AmozVz Settings") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text("Dictation Mode", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                DictationModeSelector(
                    selectedMode = selectedMode,
                    onModeSelected = { mode ->
                        selectedMode = mode
                        prefs.dictationMode = mode
                    }
                )
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("AI Polishing Engine", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                AIProvider.entries.forEach { provider ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = provider == selectedProvider,
                            onClick = {
                                selectedProvider = provider
                                prefs.aiProvider = provider
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(provider.displayName, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            if (selectedProvider != AIProvider.ON_DEVICE) {
                item {
                    OutlinedTextField(
                        value = serverUrl,
                        onValueChange = {
                            serverUrl = it
                            prefs.serverUrl = it
                        },
                        label = { Text("Server URL / Endpoint") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            prefs.apiKey = it
                        },
                        label = { Text("API Key (Groq / OpenAI / AmozVz)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            item {
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text("Speech Understanding Options", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Remove Hesitations & Fillers", style = MaterialTheme.typography.bodyLarge)
                                Text("Filters out 'um', 'uh', 'you know', 'basically'", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = stripHesitations,
                                onCheckedChange = {
                                    stripHesitations = it
                                    prefs.stripHesitations = it
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Resolve Self-Corrections", style = MaterialTheme.typography.bodyLarge)
                                Text("Fixes 'scratch that', 'no wait', 'make that'", style = MaterialTheme.typography.bodySmall)
                            }
                            Switch(
                                checked = resolveCorrections,
                                onCheckedChange = {
                                    resolveCorrections = it
                                    prefs.resolveCorrections = it
                                }
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onBackClick,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save & Return")
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
