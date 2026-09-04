package com.keshav.ai.data.local

import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(@PrimaryKey val id: String, val title: String, val createdAt: Long, val updatedAt: Long, val messageCount: Int)

@Entity(tableName = "chat_messages")
data class ChatMessageEntity(@PrimaryKey val id: String, val sessionId: String, val role: String, val content: String, val createdAt: Long, val status: String, val attachmentNames: String = "")

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY updatedAt DESC") fun observeSessions(): Flow<List<ChatSessionEntity>>
    @Query("SELECT * FROM chat_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC") fun observeMessages(sessionId: String): Flow<List<ChatMessageEntity>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertSession(session: ChatSessionEntity)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertMessage(message: ChatMessageEntity)
    @Query("DELETE FROM chat_messages WHERE sessionId = :sessionId") suspend fun deleteMessages(sessionId: String)
    @Query("DELETE FROM chat_sessions WHERE id = :sessionId") suspend fun deleteSession(sessionId: String)
    @Query("DELETE FROM chat_messages") suspend fun deleteAllMessages()
    @Query("DELETE FROM chat_sessions") suspend fun deleteAllSessions()
}

@Database(entities = [ChatSessionEntity::class, ChatMessageEntity::class], version = 2, exportSchema = false)
abstract class KeshavDatabase : RoomDatabase() { abstract fun chatDao(): ChatDao }
