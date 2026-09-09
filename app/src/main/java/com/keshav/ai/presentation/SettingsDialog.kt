package com.keshav.ai.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.keshav.ai.data.settings.AppSettings

@Composable
internal fun SettingsDialog(vm: ChatViewModel, s: AppSettings, keyExists: Boolean, close: () -> Unit) {
    var endpoint by remember(s.endpoint) { mutableStateOf(s.endpoint) }
    var model by remember(s.model) { mutableStateOf(s.model) }
    var key by remember { mutableStateOf("") }
    var dark by remember(s.darkMode) { mutableStateOf(s.darkMode) }
    var agent by remember(s.agentMode) { mutableStateOf(s.agentMode) }
    var mode by remember(s.responseMode) { mutableStateOf(s.responseMode) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val modes = listOf("brief", "normal", "expert", "ultra", "explain", "lite", "full")

    AlertDialog(
        onDismissRequest = close,
        title = { Text("keshav settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = endpoint,
                    onValueChange = { endpoint = it; validationError = null },
                    label = { Text("API endpoint") },
                    supportingText = { Text("AgentRouter: https://co.agentrouter.org") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it; validationError = null },
                    label = { Text("Model") },
                    supportingText = { Text("Example: claude-opus-4-8") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text(if (keyExists) "API key (leave blank to keep)" else "AgentRouter API key") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Response mode", fontWeight = FontWeight.Bold)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(modes) { m ->
                        FilterChip(
                            selected = mode == m,
                            onClick = { mode = m },
                            label = { Text(m) }
                        )
                    }
                }
                Row(Modifier.fillMaxWidth()) {
                    Text("Dark theme", Modifier.weight(1f))
                    Switch(checked = dark, onCheckedChange = { dark = it })
                }
                Row(Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text("Agent mode")
                        Text("Better coding instructions; not a local code executor.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = agent, onCheckedChange = { agent = it })
                }
                validationError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val cleanEndpoint = endpoint.trim()
                val cleanModel = model.trim()
                validationError = when {
                    cleanEndpoint.isBlank() -> "API endpoint cannot be empty."
                    !cleanEndpoint.startsWith("https://") -> "Use a secure HTTPS API endpoint."
                    cleanModel.isBlank() -> "Model cannot be empty."
                    !keyExists && key.isBlank() -> "Add an AgentRouter API key before saving."
                    else -> null
                }
                if (validationError == null) {
                    vm.saveSettings(cleanEndpoint, cleanModel, dark, agent, mode, key)
                    close()
                }
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
    )
}
