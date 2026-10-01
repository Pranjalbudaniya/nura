package com.nura.messaging.domain.repositories.chat

import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import kotlinx.coroutines.flow.Flow

interface ChatRepository {
    fun getMessages(conversationId: String): Flow<List<ChatMessage>>
    fun getConversations(): Flow<List<ChatConversation>>
    suspend fun sendMessage(conversationId: String, receiverId: String, content: String): Result<ChatMessage>
    suspend fun sendMediaMessage(
        conversationId: String,
        receiverId: String,
        mediaUrl: String,
        messageType: String,
        caption: String = ""
    ): Result<ChatMessage>
    suspend fun uploadChatMedia(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Result<String>
    suspend fun retrySendMessage(messageId: String): Result<Unit>


    suspend fun syncPendingMessages(): Result<Unit>
    fun observeIncomingMessages(): Flow<ChatMessage>
    suspend fun getOrCreateConversation(participantId: String, name: String, username: String, avatarUrl: String?): ChatConversation
    suspend fun getParticipantProfile(participantId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile?
    suspend fun deleteConversation(conversationId: String): Result<Unit>
    suspend fun acceptConversation(conversationId: String): Result<Unit>
    suspend fun markConversationAsRead(conversationId: String): Result<Unit>
}
