package com.keshav.ai.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshav.ai.data.settings.AppSettings
import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.MessageStatus
import kotlinx.coroutines.launch

@Composable
fun KeshavRoot(viewModel: ChatViewModel) {
    val messages by viewModel.messages.collectAsState(); val sessions by viewModel.sessions.collectAsState(); val settings by viewModel.settings.collectAsState(); val busy by viewModel.busy.collectAsState(); val error by viewModel.error.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope(); val listState = rememberLazyListState()
    var input by remember { mutableStateOf("") }; var attachment by remember { mutableStateOf<Uri?>(null) }; var showSettings by remember { mutableStateOf(false) }; var showClear by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { attachment = it }
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    ModalNavigationDrawer(drawerState = drawer, drawerContent = {
        ModalDrawerSheet { Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("keshav", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold); Text("AI assistant", color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(18.dp))
            Button(onClick = { viewModel.newChat(); scope.launch { drawer.close() } }, Modifier.fillMaxWidth()) { Text("＋  New Chat") }; Spacer(Modifier.height(14.dp)); Text("Chat history", fontWeight = FontWeight.SemiBold)
            LazyColumn(Modifier.weight(1f)) { items(sessions, key = { it.id }) { s -> TextButton(onClick = { viewModel.selectSession(s.id); scope.launch { drawer.close() } }, Modifier.fillMaxWidth()) { Text(s.title, maxLines = 1) } } }
            HorizontalDivider(); TextButton(onClick = { showSettings = true; scope.launch { drawer.close() } }, Modifier.fillMaxWidth()) { Text("⚙  Settings") }; TextButton(onClick = { showClear = true }, Modifier.fillMaxWidth()) { Text("Clear all chats") }
        } }
    }) {
        Scaffold(topBar = { SmallTopAppBar(title = { Text("keshav") }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Text("☰", fontSize = 22.sp) } }, actions = { Text(if (settings.agentMode) "AGENT" else "AI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) }) }) { padding ->
            Column(Modifier.fillMaxSize().padding(padding).imePadding().navigationBarsPadding()) {
                if (messages.isEmpty()) EmptyState { input = it } else LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 12.dp)) { items(messages, key = { it.id }) { ChatBubble(it) } }
                error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { Row(Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, Modifier.weight(1f), color = MaterialTheme.colorScheme.onErrorContainer); TextButton(onClick = { viewModel.retry() }) { Text("Retry") } } }; Spacer(Modifier.height(6.dp)) }
                if (attachment != null) Surface(Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(12.dp)) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text("📎 Image attached", Modifier.weight(1f)); TextButton(onClick = { attachment = null }) { Text("Remove") } } }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text("📎", fontSize = 20.sp) }
                    TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message keshav…") }, maxLines = 6)
                    Button(onClick = { if (busy) viewModel.stop() else { viewModel.send(input, attachment); input = ""; attachment = null } }, enabled = busy || input.isNotBlank() || attachment != null) { Text(if (busy) "Stop" else "Send") }
                }
            }
        }
    }
    if (showSettings) SettingsDialog(viewModel, settings, viewModel.hasApiKey()) { showSettings = false }
    if (showClear) AlertDialog(onDismissRequest = { showClear = false }, title = { Text("Clear all chats?") }, text = { Text("This permanently removes your local chat history.") }, confirmButton = { TextButton(onClick = { viewModel.clearAll(); showClear = false }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { showClear = false }) { Text("Cancel") } })
}

@Composable private fun EmptyState(onSuggestion: (String) -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) { Text("K", fontSize = 64.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text("keshav", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Ask questions, analyze images, write code, debug, explain and plan.", modifier = Modifier.padding(top = 8.dp), color = MaterialTheme.colorScheme.onSurfaceVariant); Spacer(Modifier.height(24.dp)); listOf("Explain this code", "Build an Android app", "Debug my error", "Create a study plan").forEach { q -> TextButton(onClick = { onSuggestion(q) }) { Text(q) } } } }

@Composable private fun ChatBubble(message: ChatMessage) { val user = message.role == ChatRole.USER; Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(18.dp), color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth(if (user) .86f else .94f)) { Column(Modifier.padding(14.dp)) { Text(if (user) "You" else "keshav", fontWeight = FontWeight.Bold, color = if (user) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary); Spacer(Modifier.height(6.dp)); MarkdownText(message.content); if (message.attachmentNames.isNotEmpty()) Text("📎 ${message.attachmentNames.joinToString()}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 6.dp)); if (message.status == MessageStatus.STREAMING) Text("●●●", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 4.dp)) } } } }

@Composable private fun MarkdownText(markdown: String) { Column { markdown.split('\n').forEach { line -> when { line.startsWith("### ") -> Text(line.removePrefix("### "), fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.padding(vertical = 3.dp)); line.startsWith("## ") -> Text(line.removePrefix("## "), fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.padding(vertical = 4.dp)); line.startsWith("# ") -> Text(line.removePrefix("# "), fontWeight = FontWeight.Bold, fontSize = 22.sp, modifier = Modifier.padding(vertical = 5.dp)); line.startsWith("- ") || line.startsWith("* ") -> Text("• " + line.drop(2)); line.isBlank() -> Spacer(Modifier.height(5.dp)); else -> Text(parseInlineMarkdown(line)) } } } }
private fun parseInlineMarkdown(text: String): AnnotatedString = buildAnnotatedString { val regex = Regex("(\\*\\*.+?\\*\\*)|(`.+?`)"); var last = 0; regex.findAll(text).forEach { m -> append(text.substring(last, m.range.first)); val token = m.value; if (token.startsWith("**")) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(token.removeSurrounding("**")) } else withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000))) { append(token.removeSurrounding("`")) }; last = m.range.last + 1 }; append(text.substring(last)) }

@Composable private fun SettingsDialog(viewModel: ChatViewModel, settings: AppSettings, keyExists: Boolean, onDismiss: () -> Unit) { var endpoint by remember(settings.endpoint) { mutableStateOf(settings.endpoint) }; var model by remember(settings.model) { mutableStateOf(settings.model) }; var key by remember { mutableStateOf("") }; var dark by remember(settings.darkMode) { mutableStateOf(settings.darkMode) }; var agent by remember(settings.agentMode) { mutableStateOf(settings.agentMode) }; AlertDialog(onDismissRequest = onDismiss, title = { Text("keshav settings") }, text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) { OutlinedTextField(endpoint, { endpoint = it }, label = { Text("API endpoint") }, singleLine = true); OutlinedTextField(model, { model = it }, label = { Text("Model") }, singleLine = true); OutlinedTextField(key, { key = it }, label = { Text(if (keyExists) "API key (leave blank to keep)" else "Anthropic API key") }, singleLine = true); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Dark theme", Modifier.weight(1f)); Switch(dark, { dark = it }) }; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Agent mode", Modifier.weight(1f)); Switch(agent, { agent = it }) }; if (agent) Text("Agent mode provides coding-focused reasoning, complete files/patches and verification steps. Full OS-level Claude Code execution needs a dedicated sandbox/backend.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) } }, confirmButton = { TextButton(onClick = { viewModel.saveSettings(endpoint, model, dark, agent, key); onDismiss() }) { Text("Save") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }) }
