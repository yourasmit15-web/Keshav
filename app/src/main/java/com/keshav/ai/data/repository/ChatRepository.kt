package com.keshav.ai.data.repository

import com.keshav.ai.data.local.ChatDao
import com.keshav.ai.data.local.ChatMessageEntity
import com.keshav.ai.data.local.ChatSessionEntity
import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.ChatSession
import com.keshav.ai.domain.model.MessageStatus
import com.keshav.ai.domain.repository.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ChatRepositoryImpl(private val dao: ChatDao) : ChatRepository {
    override fun observeSessions(): Flow<List<ChatSession>> = dao.observeSessions().map { list ->
        list.map { ChatSession(it.id, it.title, it.createdAt, it.updatedAt, it.messageCount) }
    }

    override fun observeMessages(sessionId: String): Flow<List<ChatMessage>> = dao.observeMessages(sessionId).map { list ->
        list.map { ChatMessage(it.id, it.sessionId, ChatRole.valueOf(it.role), it.content, it.createdAt, MessageStatus.valueOf(it.status)) }
    }

    override suspend fun saveSession(session: ChatSession) = dao.insertSession(
        ChatSessionEntity(session.id, session.title, session.createdAt, session.updatedAt, session.messageCount)
    )

    override suspend fun saveMessage(message: ChatMessage) = dao.insertMessage(
        ChatMessageEntity(message.id, message.sessionId, message.role.name, message.content, message.createdAt, message.status.name)
    )

    override suspend fun deleteAll() {
        dao.deleteMessages()
        dao.deleteSessions()
    }
}
