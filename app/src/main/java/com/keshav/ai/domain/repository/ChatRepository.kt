package com.keshav.ai.domain.repository

import com.keshav.ai.domain.model.ChatMessage
import com.keshav.ai.domain.model.ChatSession
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun observeSessions(): Flow<List<ChatSession>>
    fun observeMessages(sessionId: String): Flow<List<ChatMessage>>
    suspend fun saveSession(session: ChatSession)
    suspend fun saveMessage(message: ChatMessage)
    suspend fun deleteSession(sessionId: String)
    suspend fun deleteAll()
}
