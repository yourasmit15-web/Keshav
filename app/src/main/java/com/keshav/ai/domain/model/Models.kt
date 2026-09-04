package com.keshav.ai.domain.model

import java.time.Instant
import java.util.UUID

enum class ChatRole { USER, ASSISTANT }
enum class MessageStatus { COMPLETE, STREAMING, ERROR }

data class AttachmentPayload(
    val name: String,
    val mimeType: String,
    val base64: String
)

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
    val status: MessageStatus = MessageStatus.COMPLETE,
    val attachmentNames: List<String> = emptyList()
)

data class PromptMessage(
    val role: ChatRole,
    val content: String,
    val attachments: List<AttachmentPayload> = emptyList()
)

sealed interface StreamEvent {
    data class TextDelta(val text: String) : StreamEvent
    data object MessageCompleted : StreamEvent
    data class Error(val message: String, val retryable: Boolean = false) : StreamEvent
}
