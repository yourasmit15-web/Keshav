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
import androidx.compose.ui.text.input.PasswordVisualTransformation
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
    val messages by vm.messages.collectAsState()
    val sessions by vm.sessions.collectAsState()
    val settings by vm.settings.collectAsState()
    val busy by vm.busy.collectAsState()
    val error by vm.error.collectAsState()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val list = rememberLazyListState()
    val snackbar = remember { SnackbarHostState() }
    var input by remember { mutableStateOf("") }
    var image by remember { mutableStateOf<Uri?>(null) }
    var settingsOpen by remember { mutableStateOf(false) }
    var clearOpen by remember { mutableStateOf(false) }
    var section by remember { mutableStateOf("Chat") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { image = it }

    LaunchedEffect(messages.size, messages.lastOrNull()?.content) {
        if (messages.isNotEmpty()) list.animateScrollToItem(messages.lastIndex)
    }
    LaunchedEffect(error) { error?.let { snackbar.showSnackbar(it) } }

    fun notify(text: String) { scope.launch { snackbar.showSnackbar(text) } }

    ModalNavigationDrawer(
        drawerState = drawer,
        drawerContent = {
            ModalDrawerSheet {
                Column(Modifier.fillMaxSize().padding(16.dp)) {
                    Text("keshav", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("AI assistant", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(16.dp))
                    NavigationDrawerItem(label = { Text("Chat") }, selected = section == "Chat", onClick = { section = "Chat"; scope.launch { drawer.close() } })
                    NavigationDrawerItem(label = { Text("History (${sessions.size})") }, selected = section == "History", onClick = { section = "History"; scope.launch { drawer.close() } })
                    NavigationDrawerItem(label = { Text("Settings") }, selected = false, onClick = { settingsOpen = true; scope.launch { drawer.close() } })
                    NavigationDrawerItem(label = { Text("About / Diagnostics") }, selected = section == "About", onClick = { section = "About"; scope.launch { drawer.close() } })
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { vm.newChat(); section = "Chat"; scope.launch { drawer.close(); snackbar.showSnackbar("New chat created") } }, modifier = Modifier.fillMaxWidth()) { Text("＋ New Chat") }
                    Spacer(Modifier.height(12.dp))
                    Text("Mode: ${settings.responseMode.uppercase()}", color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { clearOpen = true }, modifier = Modifier.fillMaxWidth()) { Text("Clear all chats") }
                }
            }
        }
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbar) },
            topBar = {
                TopAppBar(
                    title = { Text(if (section == "Chat") "keshav" else section) },
                    navigationIcon = { IconButton(onClick = { scope.launch { drawer.open() } }) { Text("☰", fontSize = 22.sp) } },
                    actions = { Text(settings.responseMode.uppercase(), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 16.dp)) }
                )
            }
        ) { pad ->
            when (section) {
                "History" -> HistoryScreen(sessions, vm) { section = "Chat"; notify("Chat opened") }
                "About" -> AboutScreen(settings, vm.hasApiKey(), ::notify)
                else -> Column(Modifier.fillMaxSize().padding(pad).imePadding().navigationBarsPadding()) {
                    if (messages.isEmpty()) EmptyState(onSuggestion = { input = it })
                    else LazyColumn(state = list, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(12.dp)) { items(messages, key = { it.id }) { Bubble(it) } }
                    error?.let { Surface(color = MaterialTheme.colorScheme.errorContainer, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) { Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) { Text(it, Modifier.weight(1f)); TextButton(onClick = { vm.retry() }) { Text("Retry") } } } }
                    if (image != null) Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp), shape = RoundedCornerShape(10.dp)) { Row(Modifier.padding(6.dp), verticalAlignment = Alignment.CenterVertically) { Text("📎 Image ready", Modifier.weight(1f)); TextButton(onClick = { image = null }) { Text("Remove") } } }
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        IconButton(onClick = { picker.launch("image/*") }, enabled = !busy) { Text("📎") }
                        TextField(value = input, onValueChange = { input = it }, modifier = Modifier.weight(1f), placeholder = { Text("Message keshav…") }, maxLines = 6)
                        Button(onClick = { if (busy) vm.stop() else if (!vm.hasApiKey()) notify("Open Settings and add your AgentRouter API key first") else { vm.send(input, image); input = ""; image = null } }, enabled = busy || input.isNotBlank() || image != null) { Text(if (busy) "Stop" else "Send") }
                    }
                }
            }
        }
    }

    if (settingsOpen) SettingsDialog(vm, settings, vm.hasApiKey()) { settingsOpen = false; notify("Settings saved") }
    if (clearOpen) AlertDialog(onDismissRequest = { clearOpen = false }, title = { Text("Clear all chats?") }, text = { Text("This removes local history.") }, confirmButton = { TextButton(onClick = { vm.clearAll(); clearOpen = false; section = "Chat"; notify("All chats cleared") }) { Text("Clear") } }, dismissButton = { TextButton(onClick = { clearOpen = false }) { Text("Cancel") } })
}

@Composable private fun HistoryScreen(sessions: List<com.keshav.ai.domain.model.ChatSession>, vm: ChatViewModel, opened: () -> Unit) {
    if (sessions.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No saved chats yet") }
    else LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { items(sessions, key = { it.id }) { s -> ElevatedCard(onClick = { vm.selectSession(s.id); opened() }, modifier = Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text(s.title, fontWeight = FontWeight.Bold); Text("${s.messageCount} messages", color = MaterialTheme.colorScheme.onSurfaceVariant) }; Text("Open") } } } }
}

@Composable private fun AboutScreen(settings: AppSettings, keyExists: Boolean, onNotify: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("Keshav diagnostics", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("UI status: Ready")
        Text("API key: ${if (keyExists) "Configured" else "Missing"}")
        Text("Endpoint: ${settings.endpoint}")
        Text("Model: ${settings.model}")
        Text("Agent mode: ${if (settings.agentMode) "On" else "Off"}")
        Button(onClick = { onNotify(if (keyExists) "API key is configured. Send a test message from Chat." else "Add an API key in Settings before testing AI.") }) { Text("Run connection check") }
    }
}

@Composable private fun EmptyState(onSuggestion: (String) -> Unit) {
    Column(Modifier.fillMaxSize().padding(24.dp), Arrangement.Center, Alignment.CenterHorizontally) {
        Text("K", fontSize = 64.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.primary)
        Text("keshav", fontSize = 32.sp, fontWeight = FontWeight.Bold)
        Text("Ask questions, write code, debug, explain or analyze an image.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 8.dp))
        Spacer(Modifier.height(20.dp))
        listOf("Explain this code", "Debug my error", "Build an Android app", "Create a study plan").forEach { TextButton(onClick = { onSuggestion(it) }) { Text(it) } }
    }
}

@Composable private fun Bubble(m: ChatMessage) {
    val user = m.role == ChatRole.USER
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Surface(shape = RoundedCornerShape(18.dp), color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.fillMaxWidth(if (user) .86f else .94f)) {
            Column(Modifier.padding(14.dp)) {
                if (m.attachmentNames.isNotEmpty()) Text("📎 ${m.attachmentNames.joinToString()}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                Markdown(m.content)
                if (m.status == MessageStatus.STREAMING) { Spacer(Modifier.height(6.dp)); LinearProgressIndicator(Modifier.fillMaxWidth()) }
            }
        }
    }
}

@Composable private fun Markdown(text: String) {
    if (text.isBlank()) Text("…", color = MaterialTheme.colorScheme.onSurfaceVariant)
    else Text(inlineMarkdown(text), style = LocalTextStyle.current.copy(fontFamily = FontFamily.Default))
}

private fun inlineMarkdown(value: String): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val lines = value.split('\n')
    lines.forEachIndexed { index, line ->
        val clean = line.replace(Regex("^#{1,6}\\s*"), "").replace("**", "")
        builder.append(clean)
        if (index < lines.lastIndex) builder.append("\n")
    }
    return builder.toAnnotatedString()
}
