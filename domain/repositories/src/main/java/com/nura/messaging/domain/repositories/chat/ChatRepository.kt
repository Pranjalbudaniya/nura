package com.nura.messaging.domain.repositories.chat

import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    fun getConversations(): Flow<List<ChatConversation>>
    suspend fun sendMessage(conversationId: String, receiverId: String, content: String): Result<ChatMessage>
    suspend fun retrySendMessage(messageId: String): Result<Unit>
    suspend fun syncPendingMessages(): Result<Unit>
    fun observeIncomingMessages(): Flow<ChatMessage>
    suspend fun getOrCreateConversation(participantId: String, name: String, username: String, avatarUrl: String?): ChatConversation
}
