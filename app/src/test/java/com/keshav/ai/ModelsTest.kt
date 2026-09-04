package com.keshav.ai

import com.keshav.ai.domain.model.ChatRole
import com.keshav.ai.domain.model.ChatSession
import com.keshav.ai.domain.model.MessageStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ModelsTest {
    @Test
    fun newSession_hasStableIdentityAndDefaults() {
        val session = ChatSession()
        assertEquals("New chat", session.title)
        assertEquals(0, session.messageCount)
        assertNotEquals(ChatSession().id, session.id)
    }

    @Test
    fun roleAndStatusEnums_areStable() {
        assertEquals(ChatRole.USER, ChatRole.valueOf("USER"))
        assertEquals(MessageStatus.COMPLETE, MessageStatus.valueOf("COMPLETE"))
    }
}
