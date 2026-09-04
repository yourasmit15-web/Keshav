package com.keshav.ai.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.keshav.ai.data.remote.AnthropicRemote
import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.ChatSession
import com.keshav.ai.domain.model.MessageStatus
import com.keshav.ai.domain.model.PromptMessage
import com.keshav.ai.domain.model.StreamEvent
import com.keshav.ai.domain.repository.ChatRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val repository: ChatRepository,
    private val remote: AnthropicRemote
) : ViewModel() {
    private val _session = MutableStateFlow(ChatSession())
    val session: StateFlow<ChatSession> = _session.asStateFlow()

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private var streamJob: Job? = null

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || streamJob?.isActive == true) return
        streamJob = viewModelScope.launch {
            val current = _session.value
            repository.saveSession(current)
            val user = ChatMessage(sessionId = current.id, role = ChatRole.USER, content = clean)
            repository.saveMessage(user)
            _messages.update { it + user }

            val assistantId = java.util.UUID.randomUUID().toString()
            var response = ""
            val history = _messages.value.map { PromptMessage(it.role, it.content) }
            remote.stream(history).collect { event ->
                when (event) {
                    is StreamEvent.TextDelta -> {
                        response += event.text
                        _messages.update { list ->
                            list.filterNot { it.id == assistantId } + ChatMessage(assistantId, current.id, ChatRole.ASSISTANT, response, status = MessageStatus.STREAMING)
                        }
                    }
                    StreamEvent.MessageCompleted -> {
                        if (response.isNotBlank()) {
                            val assistant = ChatMessage(assistantId, current.id, ChatRole.ASSISTANT, response)
                            repository.saveMessage(assistant)
                            _messages.update { list -> list.filterNot { it.id == assistantId } + assistant }
                        }
                    }
                    is StreamEvent.Error -> {
                        _messages.update { it + ChatMessage(sessionId = current.id, role = ChatRole.ASSISTANT, content = event.message, status = MessageStatus.ERROR) }
                    }
                }
            }
        }
    }

    fun stop() { streamJob?.cancel() }
    fun newChat() { streamJob?.cancel(); _session.value = ChatSession(); _messages.value = emptyList() }

    override fun onCleared() {
        streamJob?.cancel()
        remote.close()
        super.onCleared()
    }
}
