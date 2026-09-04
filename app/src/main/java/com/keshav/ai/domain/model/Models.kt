package com.keshav.ai.domain.model

import java.time.Instant
import java.util.UUID

enum class ChatRole { USER, ASSISTANT }
enum class MessageStatus { COMPLETE, STREAMING, ERROR }

data class ChatSession(
    val id: String = UUID.randomUUID().toString(),
    val title: String = "New chat",
    val createdAt: Long = Instant.now().toEpochMilli(),
    val updatedAt: Long = createdAt,
    val messageCount: Int = 0
)

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val role: ChatRole,
    val content: String,
    val createdAt: Long = Instant.now().toEpochMilli(),
    val status: MessageStatus = MessageStatus.COMPLETE
)

data class PromptMessage(val role: ChatRole, val content: String)

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data object MessageCompleted : StreamEvent
    data class Error(val message: String) : StreamEvent
}
