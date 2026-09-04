package com.keshav.ai.presentation

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keshav.ai.data.remote.AnthropicRemote
import com.keshav.ai.data.settings.ApiKeyStore
import com.keshav.ai.data.settings.AppSettings
import com.keshav.ai.data.settings.SettingsRepository
import com.keshav.ai.domain.model.AttachmentPayload
import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.ChatSession
import com.keshav.ai.domain.model.MessageStatus
import com.keshav.ai.domain.model.PromptMessage
import com.keshav.ai.domain.model.StreamEvent
import com.keshav.ai.domain.repository.ChatRepository
import java.util.Base64
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository, private val settingsRepository: SettingsRepository, private val apiKeyStore: ApiKeyStore, private val contentResolver: ContentResolver) : ViewModel() {
    private val _sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val sessions: StateFlow<List<ChatSession>> = _sessions.asStateFlow()
    private val _session = MutableStateFlow(ChatSession())
    val session: StateFlow<ChatSession> = _session.asStateFlow()
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()
    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    private var streamJob: Job? = null
    private var messageJob: Job? = null
    private var lastPrompt = ""
    private var initializedSession = false

    init {
        viewModelScope.launch { repository.observeSessions().collect { list -> _sessions.value = list; if (!initializedSession && list.isNotEmpty()) { initializedSession = true; selectSession(list.first().id) } } }
        viewModelScope.launch { settingsRepository.settings.collect { _settings.value = it } }
    }

    fun selectSession(id: String) {
        if (_session.value.id == id) return
        val target = _sessions.value.firstOrNull { it.id == id } ?: return
        messageJob?.cancel(); _session.value = target
        messageJob = viewModelScope.launch { repository.observeMessages(id).collectLatest { _messages.value = it } }
    }

    fun newChat() { stop(); messageJob?.cancel(); _session.value = ChatSession(); _messages.value = emptyList(); _error.value = null; lastPrompt = "" }

    fun send(text: String, uri: Uri? = null) {
        val clean = text.trim(); if ((clean.isEmpty() && uri == null) || _busy.value) return
        lastPrompt = clean
        streamJob?.cancel()
        streamJob = viewModelScope.launch {
            _busy.value = true; _error.value = null
            val current = _session.value
            val attachment = uri?.let(::loadImage)
            val title = if (current.title == "New chat") clean.take(48).ifBlank { "Image chat" } else current.title
            val updated = current.copy(title = title, updatedAt = System.currentTimeMillis(), messageCount = current.messageCount + 1)
            _session.value = updated; repository.saveSession(updated)
            val user = ChatMessage(sessionId = current.id, role = ChatRole.USER, content = clean.ifBlank { "Please analyze the attached image." }, attachmentNames = listOfNotNull(attachment?.first))
            repository.saveMessage(user); _messages.update { it + user }
            val history = _messages.value.map { PromptMessage(it.role, it.content) }.toMutableList()
            if (attachment != null) history[history.lastIndex] = PromptMessage(ChatRole.USER, user.content, listOf(attachment.second))
            val remote = AnthropicRemote(_settings.value.endpoint, apiKeyStore.get().orEmpty(), _settings.value.model)
            val assistantId = UUID.randomUUID().toString(); var response = ""
            try {
                remote.stream(history, if (_settings.value.agentMode) "You are Keshav Agent, an expert software engineer. Give reliable, executable solutions. When coding, produce complete files or precise patches, tests and verification steps. Never claim a command or build was run unless its result is provided." else null).collect { event ->
                    when (event) {
                        is StreamEvent.TextDelta -> { response += event.text; _messages.update { list -> list.filterNot { it.id == assistantId } + ChatMessage(assistantId, current.id, ChatRole.ASSISTANT, response, status = MessageStatus.STREAMING) } }
                        StreamEvent.MessageCompleted -> if (response.isNotBlank()) { val assistant = ChatMessage(assistantId, current.id, ChatRole.ASSISTANT, response); repository.saveMessage(assistant); _messages.update { list -> list.filterNot { it.id == assistantId } + assistant }; val finalSession = _session.value.copy(updatedAt = System.currentTimeMillis(), messageCount = _messages.value.size); _session.value = finalSession; repository.saveSession(finalSession) }
                        is StreamEvent.Error -> _error.value = event.message
                    }
                }
            } catch (e: CancellationException) {
                if (response.isNotBlank()) repository.saveMessage(ChatMessage(assistantId, current.id, ChatRole.ASSISTANT, response, status = MessageStatus.STREAMING))
                throw e
            } finally { remote.close(); _busy.value = false }
        }
    }

    fun retry() { if (lastPrompt.isNotBlank() && !_busy.value) send(lastPrompt) }
    fun stop() { streamJob?.cancel(); streamJob = null; _busy.value = false }
    fun deleteSession(id: String) { viewModelScope.launch { repository.deleteSession(id); if (_session.value.id == id) newChat() } }
    fun clearAll() { viewModelScope.launch { repository.deleteAll(); newChat() } }
    fun saveSettings(endpoint: String, model: String, darkMode: Boolean, agentMode: Boolean, apiKey: String) { viewModelScope.launch { settingsRepository.update(endpoint, model, darkMode, agentMode); if (apiKey.isNotBlank()) apiKeyStore.save(apiKey.trim()) } }
    fun hasApiKey(): Boolean = !apiKeyStore.get().isNullOrBlank()

    private fun loadImage(uri: Uri): Pair<String, AttachmentPayload>? = runCatching {
        val mime = contentResolver.getType(uri).orEmpty(); if (!mime.startsWith("image/")) return null
        val name = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "image"
        val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return null
        if (bytes.size > 8 * 1024 * 1024) return null
        name to AttachmentPayload(name, mime, Base64.getEncoder().encodeToString(bytes))
    }.getOrNull()
}
