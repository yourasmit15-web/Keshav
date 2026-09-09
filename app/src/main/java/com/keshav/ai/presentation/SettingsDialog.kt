package com.keshav.ai.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
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
    val modes = listOf("brief", "normal", "expert", "ultra", "explain", "lite", "full")
    AlertDialog(
        onDismissRequest = close,
        title = { Text("keshav settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(endpoint, { endpoint = it }, label = { Text("API endpoint") }, singleLine = true)
                OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true)
                OutlinedTextField(key, { key = it }, label = { Text(if (keyExists) "API key (leave blank to keep)" else "AgentRouter API key") }, visualTransformation = PasswordVisualTransformation(), singleLine = true)
                Text("Response mode", fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    modes.forEach { m -> FilterChip(selected = mode == m, onClick = { mode = m }, label = { Text(m) }) }
                }
                Row(Modifier.fillMaxWidth()) { Text("Dark theme", Modifier.weight(1f)); Switch(dark, { dark = it }) }
                Row(Modifier.fillMaxWidth()) { Text("Agent mode", Modifier.weight(1f)); Switch(agent, { agent = it }) }
            }
        },
        confirmButton = { TextButton(onClick = { vm.saveSettings(endpoint, model, dark, agent, mode, key); close() }) { Text("Save") } },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
    )
}
