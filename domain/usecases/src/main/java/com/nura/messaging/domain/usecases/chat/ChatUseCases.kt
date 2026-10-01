package com.nura.messaging.domain.usecases.chat

import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.repositories.chat.ChatRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(conversationId: String): Flow<List<ChatMessage>> {
        return repository.getMessages(conversationId)
    }
}

class SendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        receiverId: String,
        content: String
    ): Result<ChatMessage> {
        if (content.isBlank()) {
            return Result.failure(IllegalArgumentException("Message content cannot be empty"))
        }
        return repository.sendMessage(conversationId, receiverId, content.trim())
    }
}

class GetConversationsUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<List<ChatConversation>> {
        return repository.getConversations()
    }
}

class SyncPendingMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.syncPendingMessages()
    }
}

class ObserveIncomingMessagesUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    operator fun invoke(): Flow<ChatMessage> {
        return repository.observeIncomingMessages()
    }
}

class GetOrCreateConversationUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        participantId: String,
        name: String,
        username: String,
        avatarUrl: String?
    ): ChatConversation {
        return repository.getOrCreateConversation(participantId, name, username, avatarUrl)
    }
}

class RetrySendMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(messageId: String): Result<Unit> {
        return repository.retrySendMessage(messageId)
    }
}

class DeleteConversationUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return repository.deleteConversation(conversationId)
    }
}

class AcceptConversationUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return repository.acceptConversation(conversationId)
    }
}

class GetParticipantProfileUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(participantId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile? {
        return repository.getParticipantProfile(participantId)
    }
}

class MarkConversationAsReadUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(conversationId: String): Result<Unit> {
        return repository.markConversationAsRead(conversationId)
    }
}

class SetActiveConversationUseCase @Inject constructor(
    private val notificationService: com.nura.messaging.domain.repositories.notification.NotificationService
) {
    operator fun invoke(conversationId: String?) {
        notificationService.setActiveConversation(conversationId)
    }
}

class SendMediaMessageUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        conversationId: String,
        receiverId: String,
        mediaUrl: String,
        messageType: String,
        caption: String = ""
    ): Result<ChatMessage> {
        return repository.sendMediaMessage(conversationId, receiverId, mediaUrl, messageType, caption)
    }
}

class UploadChatMediaUseCase @Inject constructor(
    private val repository: ChatRepository
) {
    suspend operator fun invoke(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Result<String> {
        return repository.uploadChatMedia(fileName, fileBytes, mimeType)
    }
}


