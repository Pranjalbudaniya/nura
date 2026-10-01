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
import com.nura.messaging.data.remote.auth.SupabaseAuthDataSource
import com.nura.messaging.data.remote.chat.ConversationAcceptedDto
import com.nura.messaging.domain.repositories.notification.NotificationService
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class ChatRepositoryImpl @Inject constructor(
    private val messageDao: MessageDao,
    private val conversationDao: ConversationDao,
    private val relayDataSource: MessageRelayDataSource,
    private val localContactsSource: ConnectionsLocalDataSource,
    private val authDataSource: SupabaseAuthDataSource,
    private val notificationService: NotificationService,
    private val auth: Auth,
    private val dispatchers: DispatcherProvider
) : ChatRepository {

    companion object {
        private const val TAG = "ChatRepositoryImpl"
    }

    private val json = Json { ignoreUnknownKeys = true }

    private fun resolveTransferrableAvatar(avatar: String?): String? {
        if (avatar.isNullOrBlank()) return null
        if (avatar.startsWith("preset:") || avatar.startsWith("color:") ||
            avatar.startsWith("http://") || avatar.startsWith("https://") ||
            avatar.startsWith("data:")) {
            return avatar
        }
        return try {
            val uri = android.net.Uri.parse(avatar)
            val path = uri.path ?: avatar.removePrefix("file://")
            val file = java.io.File(path)
            if (file.exists()) {
                val bytes = file.readBytes()
                "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else {
                avatar
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not resolve transferrable avatar from $avatar: ${e.message}")
            avatar
        }
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
                    val isShared = conv.isAccepted || messageDao.hasBothExchangedFirstMessage(conv.conversationId)
                    conv.toDomain().copy(isAccepted = conv.isAccepted, isProfileShared = isShared)
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
                    unreadCount = 0,
                    isAccepted = false
                )
            )
        }
        Log.d(TAG, "Message $messageId saved to local database as PENDING")

        val currentUser = authDataSource.getCurrentUser()
        val senderName = currentUser?.userMetadata?.get("display_name")?.jsonPrimitive?.content
            ?: currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
            ?: "User"
        val rawAvatar = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        val senderAvatar = resolveTransferrableAvatar(rawAvatar)

        // 2. Transmit message via Supabase Realtime Broadcast & persistent relay
        val relayDto = MessageRelayDto(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = receiverId,
            conversationId = conversationId,
            content = content,
            messageType = "text",
            createdAt = now,
            deliveryStatus = "SENT_TO_SERVER",
            senderName = senderName,
            senderAvatar = senderAvatar
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

    override suspend fun sendMediaMessage(
        conversationId: String,
        receiverId: String,
        mediaUrl: String,
        messageType: String,
        caption: String
    ): Result<ChatMessage> = withContext(dispatchers.io) {
        val currentUserId = auth.currentUserOrNull()?.id ?: "me"
        val messageId = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val content = mediaUrl
        val localMessage = MessageEntity(
            messageId = messageId,
            conversationId = conversationId,
            senderId = currentUserId,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            timestamp = now,
            status = MessageStatus.PENDING.name,
            isOutgoing = true
        )
        messageDao.upsertMessage(localMessage)

        val snippet = when (messageType) {
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            "audio" -> "🎤 Voice message"
            else -> "Media"
        }

        val existing = conversationDao.getConversationById(conversationId)
        if (existing != null) {
            conversationDao.updateLastMessage(conversationId, snippet, now)
        } else {
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
                    lastMessage = snippet,
                    lastMessageTimestamp = now,
                    unreadCount = 0,
                    isAccepted = false
                )
            )
        }

        val currentUser = authDataSource.getCurrentUser()
        val senderName = currentUser?.userMetadata?.get("display_name")?.jsonPrimitive?.content
            ?: currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
            ?: "User"
        val rawAvatar = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
        val senderAvatar = resolveTransferrableAvatar(rawAvatar)

        val relayDto = MessageRelayDto(
            messageId = messageId,
            senderId = currentUserId,
            receiverId = receiverId,
            conversationId = conversationId,
            content = content,
            messageType = messageType,
            createdAt = now,
            deliveryStatus = "SENT_TO_SERVER",
            senderName = senderName,
            senderAvatar = senderAvatar,
            mediaUrl = mediaUrl
        )

        val uploadResult = relayDataSource.sendMessageToRelay(relayDto)
        if (uploadResult.isSuccess) {
            messageDao.updateMessageStatus(messageId, MessageStatus.SENT.name)
            Result.success(localMessage.copy(status = MessageStatus.SENT.name).toDomain())
        } else {
            messageDao.updateMessageStatus(messageId, MessageStatus.FAILED.name)
            Result.success(localMessage.copy(status = MessageStatus.FAILED.name).toDomain())
        }
    }

    override suspend fun uploadChatMedia(
        fileName: String,
        fileBytes: ByteArray,
        mimeType: String
    ): Result<String> = withContext(dispatchers.io) {
        val url = authDataSource.uploadMedia(fileName, fileBytes, mimeType)
        if (url != null) {
            Result.success(url)
        } else {
            Result.failure(RuntimeException("Failed to upload media to storage"))
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
                if (dto.messageType == "system_accept") {
                    try {
                        val acceptDto = json.decodeFromString(ConversationAcceptedDto.serializer(), dto.content)
                        handleConversationAccepted(acceptDto)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to decode system_accept message: ${e.message}")
                    }
                    relayDataSource.acknowledgeAndRemoveMessage(
                        messageId = dto.messageId,
                        senderId = dto.senderId,
                        receiverId = currentUserId
                    )
                    continue
                }

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
                        if (dto.messageType == "system_accept") {
                            try {
                                val acceptDto = json.decodeFromString(ConversationAcceptedDto.serializer(), dto.content)
                                handleConversationAccepted(acceptDto)
                            } catch (e: Exception) {
                                Log.w(TAG, "Failed to decode realtime system_accept message: ${e.message}")
                            }
                            relayDataSource.acknowledgeAndRemoveMessage(
                                messageId = dto.messageId,
                                senderId = dto.senderId,
                                receiverId = userId
                            )
                            return@collect
                        }

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

                // 3. Listen for conversation accepted realtime events
                val acceptJob = launch(dispatchers.io) {
                    relayDataSource.observeConversationAccepted(userId).collect { acceptDto ->
                        Log.d(TAG, "Realtime conversation accepted received for ${acceptDto.conversationId}")
                        handleConversationAccepted(acceptDto)
                    }
                }
                activeJobs.add(acceptJob)
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
            if (existing.participantName.startsWith("User ") || existing.participantAvatarUrl.isNullOrBlank()) {
                val remote = authDataSource.fetchRemoteProfile(participantId)
                if (remote != null) {
                    val updated = existing.copy(
                        participantName = remote.displayName.ifBlank { existing.participantName },
                        participantUsername = remote.username.ifBlank { existing.participantUsername },
                        participantAvatarUrl = remote.avatarUrl ?: existing.participantAvatarUrl
                    )
                    conversationDao.upsertConversation(updated)
                    return@withContext updated.toDomain()
                }
            }
            return@withContext existing.toDomain()
        }

        var resolvedName = name
        var resolvedUsername = username
        var resolvedAvatar = avatarUrl
        if (name.startsWith("User ") || avatarUrl.isNullOrBlank()) {
            val remote = authDataSource.fetchRemoteProfile(participantId)
            if (remote != null) {
                resolvedName = remote.displayName.ifBlank { name }
                resolvedUsername = remote.username.ifBlank { username }
                resolvedAvatar = remote.avatarUrl ?: avatarUrl
            }
        }

        val currentUserId = auth.currentUserOrNull()?.id ?: "me"
        val convId = if (currentUserId < participantId) "${currentUserId}_${participantId}" else "${participantId}_${currentUserId}"
        val newConv = ConversationEntity(
            conversationId = convId,
            participantId = participantId,
            participantName = resolvedName,
            participantUsername = resolvedUsername,
            participantAvatarUrl = resolvedAvatar,
            lastMessage = null,
            lastMessageTimestamp = System.currentTimeMillis(),
            unreadCount = 0,
            isAccepted = false
        )
        conversationDao.upsertConversation(newConv)
        newConv.toDomain()
    }

    override suspend fun getParticipantProfile(participantId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile? = withContext(dispatchers.io) {
        authDataSource.fetchRemoteProfile(participantId)
    }

    override suspend fun deleteConversation(conversationId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            messageDao.deleteMessagesByConversation(conversationId)
            conversationDao.deleteConversation(conversationId)
        }
    }

    override suspend fun acceptConversation(conversationId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            conversationDao.acceptConversation(conversationId)
            val conv = conversationDao.getConversationById(conversationId) ?: return@runCatching Unit

            // 1. Fetch partner's profile and save partner's avatar to local mobile storage
            val remotePartner = authDataSource.fetchRemoteProfile(conv.participantId)
            val partnerAvatarSource = remotePartner?.avatarUrl ?: conv.participantAvatarUrl
            val localPartnerAvatar = if (!partnerAvatarSource.isNullOrBlank()) {
                localContactsSource.saveUserProfilePicture(conv.participantId, partnerAvatarSource)
            } else {
                null
            }

            val partnerName = remotePartner?.displayName?.ifBlank { conv.participantName } ?: conv.participantName
            val partnerUsername = remotePartner?.username?.ifBlank { conv.participantUsername } ?: conv.participantUsername

            conversationDao.updateParticipantDetails(
                conversationId = conversationId,
                name = partnerName,
                username = partnerUsername,
                avatarUrl = localPartnerAvatar ?: partnerAvatarSource
            )

            // 2. Prepare current user's (acceptor's) details & transferrable avatar to travel to partner's mobile
            val currentUser = authDataSource.getCurrentUser()
            val acceptorId = currentUser?.id ?: auth.currentUserOrNull()?.id.orEmpty()
            val acceptorName = currentUser?.userMetadata?.get("display_name")?.jsonPrimitive?.content
                ?: currentUser?.userMetadata?.get("full_name")?.jsonPrimitive?.content
                ?: "User"
            val acceptorUsername = currentUser?.userMetadata?.get("username")?.jsonPrimitive?.content.orEmpty()
            val rawAcceptorAvatar = currentUser?.userMetadata?.get("avatar_url")?.jsonPrimitive?.content
            val transferrableAvatar = resolveTransferrableAvatar(rawAcceptorAvatar)

            // 3. Transmit acceptance event to partner (realtime broadcast + relay table)
            val acceptDto = ConversationAcceptedDto(
                conversationId = conversationId,
                acceptorId = acceptorId,
                acceptorName = acceptorName,
                acceptorUsername = acceptorUsername,
                acceptorAvatarUrl = transferrableAvatar
            )
            relayDataSource.sendConversationAccepted(acceptDto, conv.participantId)
            Log.d(TAG, "Sent conversation accepted event to partner ${conv.participantId}")
            Unit
        }
    }

    private suspend fun handleConversationAccepted(dto: ConversationAcceptedDto) {
        conversationDao.acceptConversation(dto.conversationId)
        val conv = conversationDao.getConversationById(dto.conversationId)
            ?: conversationDao.getConversationByParticipant(dto.acceptorId)

        // Save acceptor's avatar into local mobile data storage
        val rawAvatar = dto.acceptorAvatarUrl
        val localAvatarUri = if (!rawAvatar.isNullOrBlank()) {
            localContactsSource.saveUserProfilePicture(dto.acceptorId, rawAvatar)
        } else {
            null
        }

        val name = dto.acceptorName.ifBlank { conv?.participantName ?: "User" }
        val username = dto.acceptorUsername.ifBlank { conv?.participantUsername.orEmpty() }

        if (conv != null) {
            conversationDao.updateParticipantDetails(
                conversationId = conv.conversationId,
                name = name,
                username = username,
                avatarUrl = localAvatarUri ?: dto.acceptorAvatarUrl
            )
            conversationDao.acceptConversation(conv.conversationId)
        } else {
            conversationDao.upsertConversation(
                ConversationEntity(
                    conversationId = dto.conversationId,
                    participantId = dto.acceptorId,
                    participantName = name,
                    participantUsername = username,
                    participantAvatarUrl = localAvatarUri ?: dto.acceptorAvatarUrl,
                    lastMessage = null,
                    lastMessageTimestamp = System.currentTimeMillis(),
                    unreadCount = 0,
                    isAccepted = true
                )
            )
        }
        Log.d(TAG, "Conversation ${dto.conversationId} accepted and partner avatar stored locally: $localAvatarUri")
    }

    override suspend fun markConversationAsRead(conversationId: String): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            conversationDao.markAsRead(conversationId)
            Unit
        }
    }

    private suspend fun updateConversationOnIncoming(dto: MessageRelayDto) {
        val currentUserId = auth.currentUserOrNull()?.id.orEmpty()
        val existingConv = conversationDao.getConversationById(dto.conversationId)
            ?: conversationDao.getConversationByParticipant(dto.senderId)

        val lastMsgPreview = when (dto.messageType) {
            "image" -> "📷 Photo"
            "video" -> "🎥 Video"
            "audio" -> "🎤 Voice message"
            else -> dto.content
        }

        if (existingConv != null) {
            val resolvedAvatar = dto.senderAvatar ?: existingConv.participantAvatarUrl
            val resolvedName = dto.senderName?.ifBlank { null } ?: existingConv.participantName

            conversationDao.updateLastMessageWithUnread(existingConv.conversationId, lastMsgPreview, dto.createdAt)

            if (resolvedAvatar != existingConv.participantAvatarUrl ||
                (existingConv.participantName.startsWith("User ") && !dto.senderName.isNullOrBlank())) {
                conversationDao.upsertConversation(
                    existingConv.copy(
                        participantName = resolvedName,
                        participantAvatarUrl = resolvedAvatar,
                        lastMessage = lastMsgPreview,
                        lastMessageTimestamp = dto.createdAt
                    )
                )
            } else if (existingConv.participantAvatarUrl.isNullOrBlank() || existingConv.participantName.startsWith("User ")) {
                val remote = authDataSource.fetchRemoteProfile(dto.senderId)
                if (remote != null && remote.displayName.isNotBlank()) {
                    conversationDao.upsertConversation(
                        existingConv.copy(
                            participantName = remote.displayName,
                            participantUsername = remote.username.ifBlank { existingConv.participantUsername },
                            participantAvatarUrl = remote.avatarUrl ?: existingConv.participantAvatarUrl,
                            lastMessage = lastMsgPreview,
                            lastMessageTimestamp = dto.createdAt
                        )
                    )
                }
            }

            if (dto.senderId != currentUserId) {
                notificationService.showMessageNotification(
                    title = resolvedName,
                    content = lastMsgPreview,
                    conversationId = existingConv.conversationId,
                    senderId = dto.senderId
                )
            }
        } else {
            val contact = localContactsSource.getRecentConnections(currentUserId)
                .find { it.id == dto.senderId }

            // Query Supabase directly if sender name or avatar wasn't included in packet
            val remote = if (dto.senderName.isNullOrBlank() || dto.senderAvatar.isNullOrBlank()) {
                authDataSource.fetchRemoteProfile(dto.senderId)
            } else null

            val displayName = dto.senderName
                ?: remote?.displayName?.ifBlank { null }
                ?: contact?.displayName
                ?: "User ${dto.senderId.take(4)}"
            val username = remote?.username?.ifBlank { null }
                ?: contact?.username
                ?: dto.senderId.take(6)
            val avatarUrl = dto.senderAvatar ?: remote?.avatarUrl ?: contact?.avatarUri

            val newConv = ConversationEntity(
                conversationId = dto.conversationId,
                participantId = dto.senderId,
                participantName = displayName,
                participantUsername = username,
                participantAvatarUrl = avatarUrl,
                lastMessage = lastMsgPreview,
                lastMessageTimestamp = dto.createdAt,
                unreadCount = 1
            )
            conversationDao.upsertConversation(newConv)

            if (dto.senderId != currentUserId) {
                notificationService.showMessageNotification(
                    title = displayName,
                    content = lastMsgPreview,
                    conversationId = dto.conversationId,
                    senderId = dto.senderId
                )
            }
        }
    }

}
