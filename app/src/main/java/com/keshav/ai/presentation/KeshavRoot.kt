package com.keshav.ai.presentation

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.keshav.ai.data.settings.AppSettings
import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.MessageStatus
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeshavRoot(vm: ChatViewModel) {
    val messages by vm.messages.collectAsState(); val sessions by vm.sessions.collectAsState(); val settings by vm.settings.collectAsState(); val busy by vm.busy.collectAsState(); val error by vm.error.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed); val scope = rememberCoroutineScope(); val list = rememberLazyListState()
    var input by remember { mutableStateOf("") }; var image by remember { mutableStateOf<Uri?>(null) }; var settingsOpen by remember { mutableStateOf(false) }; var clearOpen by remember { mutableStateOf(false) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }
    LaunchedEffect(messages.size, messages.lastOrNull()?.content) { if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex) }

    ModalNavigationDrawer(drawerState = drawer, drawerContent = { ModalDrawerSheet {
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Text("keshav", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("AI assistant", color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { vm.newChat(); scope.launch { drawer.close() } }, modifier = Modifier.fillMaxWidth()) { Text("＋ New Chat") }
            Spacer(Modifier.height(12.dp)); Text("History", fontWeight = FontWeight.Bold)
            LazyColumn(modifier = Modifier.weight(1f)) { items(sessions, key = { it.id }) { s -> TextButton(onClick = { vm.selectSession(s.id); scope.launch { drawer.close() } }, modifier = Modifier.fillMaxWidth()) { Text(s.title, maxLines = 1) } } }
            HorizontalDivider()
            TextButton(onClick = { settingsOpen = true; scope.launch { drawer.close() } }, modifier = Modifier.fillMaxWidth()) { Text("⚙ Settings") }
            TextButton(onClick = { clearOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear all chats") }
        }
    } }) {
        Scaffold(topBar = { TopAppBar(title = { Text("keshav") }, navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Text("☰", fontSize = 22.sp) } }, actions = { Text(if (settings.agentMode) "AGENT" else "AI", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) }) }) { pad ->
            Column(Modifier.fillMaxSize().padding(pad).imePadding().navigationBarsPadding()) {
                if (messages.isEmpty()) EmptyState { input = it } else LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(12.dp)) { items(messages, key = { it.id }) { Bubble(it) } }
                error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, Modifier.weight(1f)); TextButton(onClick = { vm.retry() }) { Text("Retry") } } } }
                if (image != null) Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(10.dp)) { Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { Text("📎 Image ready", Modifier.weight(1f)); TextButton(onClick = { image = null }) { Text("Remove") } } }
                Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    IconButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text("📎") }
                    TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message keshav…") }, maxLines = 6)
                    Button(onClick = { if (busy) vm.stop() else { vm.send(input, image); input = ""; image = null } }, enabled = busy || input.isNotBlank() || image != null) { Text(if (busy) "Stop" else "Send") }
                }
            }
        }
    }
    if (settingsOpen) SettingsDialog(vm, settings, vm.hasApiKey()) { settingsOpen = false }
    if (clearOpen) AlertDialog(
        onDismissRequest = { clearOpen = false },
        title = { Text("Clear all chats?") },
        text = { Text("This removes local history.") },
        confirmButton = { TextButton(onClick = { vm.clearAll(); clearOpen = false }) { Text("Clear") } },
        dismissButton = { TextButton(onClick = { clearOpen = false }) { Text("Cancel") } }
    )
}

@Composable private fun EmptyState(onSuggestion: (String) -> Unit) { Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) { Text("K", fontSize = 64.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary); Text("keshav", fontSize = 32.sp, fontWeight = FontWeight.Bold); Text("Ask questions, write code, debug, explain or analyze an image.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp)); Spacer(Modifier.height(20.dp)); listOf("Explain this code", "Debug my error", "Build an Android app", "Create a study plan").forEach { TextButton(onClick = { onSuggestion(it) }) { Text(it) } } } }

@Composable private fun Bubble(m: ChatMessage) { val user = m.role == ChatRole.USER; Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) { Surface(shape = RoundedCornerShape(18.dp), color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth(if (user) .86f else .94f)) { Column(Modifier.padding(14.dp)) { Text(if (user) "You" else "keshav", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary); Spacer(Modifier.height(5.dp)); Markdown(m.content); if (m.attachmentNames.isNotEmpty()) Text("📎 ${m.attachmentNames.joinToString()}", color = MaterialTheme.colorScheme.primary); if (m.status == MessageStatus.STREAMING) Text("●●●", color = MaterialTheme.colorScheme.primary) } } } }

@Composable private fun Markdown(text: String) { Column { text.split('\n').forEach { line -> when { line.startsWith("### ") -> Text(line.drop(4), fontWeight = FontWeight.Bold); line.startsWith("## ") -> Text(line.drop(3), fontWeight = FontWeight.Bold, fontSize = 19.sp); line.startsWith("# ") -> Text(line.drop(2), fontWeight = FontWeight.Bold, fontSize = 22.sp); line.startsWith("- ") -> Text("• ${line.drop(2)}"); line.isBlank() -> Spacer(Modifier.height(4.dp)); else -> Text(inlineMarkdown(line)) } } } }
private fun inlineMarkdown(text: String): AnnotatedString = buildAnnotatedString { val r = Regex("(\\*\\*.+?\\*\\*)|(`.+?`)"); var last = 0; r.findAll(text).forEach { m -> append(text.substring(last, m.range.first)); if (m.value.startsWith("**")) withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(m.value.removeSurrounding("**")) } else withStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color(0x22000000))) { append(m.value.removeSurrounding("`")) }; last = m.range.last + 1 }; append(text.substring(last)) }

@Composable private fun SettingsDialog(vm: ChatViewModel, s: AppSettings, keyExists: Boolean, close: () -> Unit) { var endpoint by remember(s.endpoint) { mutableStateOf(s.endpoint) }; var model by remember(s.model) { mutableStateOf(s.model) }; var key by remember { mutableStateOf("") }; var dark by remember(s.darkMode) { mutableStateOf(s.darkMode) }; var agent by remember(s.agentMode) { mutableStateOf(s.agentMode) }; AlertDialog(onDismissRequest = close, title = { Text("keshav settings") }, text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { OutlinedTextField(value = endpoint, onValueChange = { endpoint = it }, label = { Text("API endpoint") }, singleLine = true); OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Model") }, singleLine = true); OutlinedTextField(value = key, onValueChange = { key = it }, label = { Text(if (keyExists) "API key (leave blank to keep)" else "Anthropic API key") }, singleLine = true); Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Dark theme", Modifier.weight(1f)); Switch(checked = dark, onCheckedChange = { dark = it }) }; Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { Text("Agent mode", Modifier.weight(1f)); Switch(checked = agent, onCheckedChange = { agent = it }) } } }, confirmButton = { TextButton(onClick = { vm.saveSettings(endpoint, model, dark, agent, key); close() }) { Text("Save") } }, dismissButton = { TextButton(onClick = close) { Text("Cancel") } }) }
