package com.nura.messaging.data.repositories.chat

import android.util.Log
import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.core.database.dao.ConversationDao
import com.nura.messaging.core.database.dao.MessageDao
import com.nura.messaging.core.database.entity.ConversationEntity
import com.nura.messaging.core.database.entity.MessageEntity
import com.nura.messaging.data.local.contacts.ConnectionsLocalDataSource
import com.nura.messaging.data.remote.chat.MessageRelayDataSource
import com.nura.messaging.data.remote.chat.MessageRelayDto
import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus
import com.nura.messaging.domain.repositories.chat.ChatRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val relayDataSource: MessageRelayDataSource,
    private val localContactsSource: ConnectionsLocalDataSource,
    private val auth: Auth,
    private val dispatchers: DispatcherProvider
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    override fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByConversation(conversationId)
            .map { list -> list.map { it.toDomain() } }
            .flowOn(dispatchers.io)
    }

    override fun getConversations(): Flow<List<ChatConversation>> {
        return conversationDao.getAllConversations()
            .map { list ->
                list.map { conv ->
                    val isShared = messageDao.hasBothExchangedFirstMessage(conv.conversationId)
                    conv.toDomain().copy(isProfileShared = isShared)
                }
            }
            .flowOn(dispatchers.io)
    }

    override suspend fun sendMessage(
        conversationId: String,
        receiverId: String,
        content: String
    ): Result<ChatMessage> = withContext(dispatchers.io) {
        val currentUserId = auth.currentUserOrNull()?.id ?: "me"
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        // 1. Write to local Room database first so it immediately appears in the sender's chat
        val localMessage = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = receiverId,
            content = content,
            messageType = "text",
            timestamp = now,
            status = MessageStatus.PENDING.name,
            isOutgoing = true
        )
        messageDao.upsertMessage(localMessage)

        val existing = conversationDao.getConversationById(conversationId)
        if (existing != null) {
            conversationDao.updateLastMessage(conversationId, content, now)
        } else {
            // Check if contact exists in local connections for receiver details
            val contact = localContactsSource.getRecentConnections(currentUserId)
                .find { it.id == receiverId }
            val name = contact?.displayName ?: "User ${receiverId.take(4)}"
            val username = contact?.username ?: receiverId.take(6)
            val avatarUrl = contact?.avatarUri

            conversationDao.upsertConversation(
                ConversationEntity(
                    conversationId = conversationId,
                    participantId = receiverId,
                    participantName = name,
                    participantUsername = username,
                    participantAvatarUrl = avatarUrl,
                    lastMessage = content,
                    lastMessageTimestamp = now,
                    unreadCount = 0
                )
            )
        }
        Log.d(TAG, "Message $messageId saved to local database as PENDING")

        // 2. Transmit message via Supabase Realtime Broadcast & persistent relay
        val relayDto = MessageRelayDto(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = receiverId,
            conversationId = conversationId,
            content = content,
            messageType = "text",
            createdAt = now,
            deliveryStatus = "SENT_TO_SERVER"
        )

        val uploadResult = relayDataSource.sendMessageToRelay(relayDto)
        if (uploadResult.isSuccess) {
            messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
            Log.d(TAG, "Message $messageId successfully sent to relay, marked as SENT")
            Result.success(localMessage.copy(status = MessageStatus.SENT.name).toDomain())
        } else {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
            Log.w(TAG, "Message $messageId upload to relay failed: ${uploadResult.exceptionOrNull()?.message}")
            Result.success(localMessage.copy(status = MessageStatus.FAILED.name).toDomain())
        }
    }

    override suspend fun retrySendMessage(messageId: String): Result<Unit> = withContext(dispatchers.io) {
        val message = messageDao.getMessageById(messageId)
            ?: return@withContext Result.failure(IllegalArgumentException("Message not found"))

        val relayDto = MessageRelayDto(
            messageId = message.messageId,
            senderId = message.senderId,
            receiverId = message.receiverId,
            conversationId = message.conversationId,
            content = message.content,
            messageType = message.messageType,
            createdAt = message.timestamp,
            deliveryStatus = "SENT_TO_SERVER"
        )

        val result = relayDataSource.sendMessageToRelay(relayDto)
        if (result.isSuccess) {
            messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
            Result.success(Unit)
        } else {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
            Result.failure(result.exceptionOrNull() ?: RuntimeException("Upload failed"))
        }
    }

    override suspend fun syncPendingMessages(): Result<Unit> = withContext(dispatchers.io) {
        val currentUserId = auth.currentUserOrNull()?.id ?: return@withContext Result.success(Unit)
        runCatching {
            val pendingMessages = relayDataSource.fetchPendingMessages(currentUserId).getOrThrow()
            Log.d(TAG, "Sync: Processing ${pendingMessages.size} pending relay messages")

            for (dto in pendingMessages) {
                // Idempotent write into Room database
                val entity = MessageEntity(
                    messageId = dto.messageId,
                    conversationId = dto.conversationId,
                    senderId = dto.senderId,
                    receiverId = dto.receiverId,
                    content = dto.content,
                    messageType = dto.messageType,
                    timestamp = dto.createdAt,
                    status = MessageStatus.DELIVERED.name,
                    isOutgoing = false
                )
                messageDao.insertMessage(entity)

                // Verify local write succeeded before deleting from server
                val persisted = messageDao.getMessageById(dto.messageId)
                if (persisted != null) {
                    updateConversationOnIncoming(dto)
                    relayDataSource.acknowledgeAndRemoveMessage(
                        messageId = dto.messageId,
                        senderId = dto.senderId,
                        receiverId = currentUserId
                    )
                    Log.d(TAG, "Sync: Message ${dto.messageId} verified locally and removed from Supabase")
                } else {
                    Log.e(TAG, "Sync: Local write verification failed for message ${dto.messageId}")
                }
            }
        }.onFailure { e ->
            Log.w(TAG, "Sync: Could not fetch/process pending messages: ${e.message}")
        }
    }

    override fun observeIncomingMessages(): Flow<ChatMessage> = channelFlow {
        var currentListeningUserId: String? = null
        val activeJobs = mutableListOf<Job>()

        val startListeners = { userId: String ->
            if (currentListeningUserId != userId || activeJobs.none { it.isActive }) {
                currentListeningUserId = userId
                activeJobs.forEach { it.cancel() }
                activeJobs.clear()

                Log.d(TAG, "Starting message and ack listeners for user $userId")

                // 1. Listen for incoming messages
                val msgJob = launch(dispatchers.io) {
                    relayDataSource.observeIncomingMessages(userId).collect { dto ->
                        val entity = MessageEntity(
                            messageId = dto.messageId,
                            conversationId = dto.conversationId,
                            senderId = dto.senderId,
                            receiverId = dto.receiverId,
                            content = dto.content,
                            messageType = dto.messageType,
                            timestamp = dto.createdAt,
                            status = MessageStatus.DELIVERED.name,
                            isOutgoing = false
                        )
                        messageDao.insertMessage(entity)

                        val persisted = messageDao.getMessageById(dto.messageId)
                        if (persisted != null) {
                            updateConversationOnIncoming(dto)
                            relayDataSource.acknowledgeAndRemoveMessage(
                                messageId = dto.messageId,
                                senderId = dto.senderId,
                                receiverId = userId
                            )
                            Log.d(TAG, "Incoming message ${dto.messageId} saved, acknowledged, and emitted")
                            send(persisted.toDomain())
                        }
                    }
                }
                activeJobs.add(msgJob)

                // 2. Listen for delivery ACKs from receivers
                val ackJob = launch(dispatchers.io) {
                    relayDataSource.observeDeliveryAcks(userId).collect { ack ->
                        Log.d(TAG, "Delivery ACK received for message ${ack.messageId}")
                        messageDao.updateMessageStatus(ack.messageId, MessageStatus.DELIVERED.name)
                    }
                }
                activeJobs.add(ackJob)
            }
        }

        val initialUserId = auth.currentUserOrNull()?.id
        if (!initialUserId.isNullOrBlank()) {
            startListeners(initialUserId)
        }

        auth.sessionStatus.collect { status ->
            if (status is SessionStatus.Authenticated) {
                val uid = status.session.user?.id
                if (!uid.isNullOrBlank()) {
                    startListeners(uid)
                }
            }
        }
    }.flowOn(dispatchers.io)

    override suspend fun getOrCreateConversation(
        participantId: String,
        name: String,
        username: String,
        avatarUrl: String?
    ): ChatConversation = withContext(dispatchers.io) {
        val existing = conversationDao.getConversationByParticipant(participantId)
        if (existing != null) {
            return@withContext existing.toDomain()
        }

        val currentUserId = auth.currentUserOrNull()?.id ?: "me"
        val convId = if (currentUserId < participantId) "${currentUserId}_${participantId}" else "${participantId}_${currentUserId}"
        val newConv = ConversationEntity(
            conversationId = convId,
            participantId = participantId,
            participantName = name,
            participantUsername = username,
            participantAvatarUrl = avatarUrl,
            lastMessage = null,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0
        )
        conversationDao.upsertConversation(newConv)
        newConv.toDomain()
    }

    private suspend fun updateConversationOnIncoming(dto: MessageRelayDto) {
        val existingConv = conversationDao.getConversationById(dto.conversationId)
            ?: conversationDao.getConversationByParticipant(dto.senderId)

        if (existingConv != null) {
            conversationDao.updateLastMessage(existingConv.conversationId, dto.content, dto.createdAt)
        } else {
            val currentUserId = auth.currentUserOrNull()?.id.orEmpty()
            val contact = localContactsSource.getRecentConnections(currentUserId)
                .find { it.id == dto.senderId }

            val displayName = contact?.displayName ?: "User ${dto.senderId.take(4)}"
            val username = contact?.username ?: dto.senderId.take(6)
            val avatarUrl = contact?.avatarUri

            val newConv = ConversationEntity(
                conversationId = dto.conversationId,
                participantId = dto.senderId,
                participantName = displayName,
                participantUsername = username,
                participantAvatarUrl = avatarUrl,
                lastMessage = dto.content,
                lastMessageTimestamp = dto.createdAt,
                unreadCount = 1
            )
            conversationDao.upsertConversation(newConv)
        }
    }
}
