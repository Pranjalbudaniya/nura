package com.nura.messaging.domain.usecases.chat

import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus
import com.nura.messaging.domain.repositories.chat.ChatRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatUseCasesTest {

    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var sendMessageUseCase: SendMessageUseCase
    private lateinit var getMessagesUseCase: GetMessagesUseCase
    private lateinit var getConversationsUseCase: GetConversationsUseCase
    private lateinit var syncPendingMessagesUseCase: SyncPendingMessagesUseCase
    private lateinit var getOrCreateConversationUseCase: GetOrCreateConversationUseCase
    private lateinit var markConversationAsReadUseCase: MarkConversationAsReadUseCase
    private lateinit var setActiveConversationUseCase: SetActiveConversationUseCase
    private lateinit var fakeNotificationService: FakeNotificationService

    @Before
    fun setUp() {
        fakeRepository = FakeChatRepository()
        fakeNotificationService = FakeNotificationService()
        sendMessageUseCase = SendMessageUseCase(fakeRepository)
        getMessagesUseCase = GetMessagesUseCase(fakeRepository)
        getConversationsUseCase = GetConversationsUseCase(fakeRepository)
        syncPendingMessagesUseCase = SyncPendingMessagesUseCase(fakeRepository)
        getOrCreateConversationUseCase = GetOrCreateConversationUseCase(fakeRepository)
        markConversationAsReadUseCase = MarkConversationAsReadUseCase(fakeRepository)
        setActiveConversationUseCase = SetActiveConversationUseCase(fakeNotificationService)
    }

    @Test
    fun `sendMessageUseCase sends message with non-empty content successfully`() = runTest {
        val result = sendMessageUseCase("conv_1", "user_2", "Hello World")
        assertTrue(result.isSuccess)
        val msg = result.getOrNull()
        assertEquals("Hello World", msg?.content)
        assertEquals("conv_1", msg?.conversationId)
        assertEquals(MessageStatus.SENT, msg?.status)
    }

    @Test
    fun `sendMessageUseCase fails when content is blank`() = runTest {
        val result = sendMessageUseCase("conv_1", "user_2", "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun `getMessagesUseCase returns message list flow`() = runTest {
        val messages = getMessagesUseCase("conv_1").first()
        assertEquals(1, messages.size)
        assertEquals("Initial message", messages[0].content)
    }

    @Test
    fun `getConversationsUseCase returns conversation list flow`() = runTest {
        val conversations = getConversationsUseCase().first()
        assertEquals(1, conversations.size)
        assertEquals("Alice", conversations[0].participantName)
    }

    @Test
    fun `getOrCreateConversationUseCase returns conversation`() = runTest {
        val conv = getOrCreateConversationUseCase("user_3", "Bob", "bob", null)
        assertEquals("user_3", conv.participantId)
        assertEquals("Bob", conv.participantName)
    }

    @Test
    fun `markConversationAsReadUseCase marks conversation as read successfully`() = runTest {
        val result = markConversationAsReadUseCase("conv_1")
        assertTrue(result.isSuccess)
    }

    @Test
    fun `setActiveConversationUseCase updates notification service`() {
        setActiveConversationUseCase("conv_active_123")
        assertEquals("conv_active_123", fakeNotificationService.currentActiveId)
        setActiveConversationUseCase(null)
        assertEquals(null, fakeNotificationService.currentActiveId)
    }

    private class FakeChatRepository : ChatRepository {
        val messagesList = mutableListOf(
            ChatMessage(
                id = "m1",
                conversationId = "conv_1",
                senderId = "user_1",
                receiverId = "user_2",
                content = "Initial message",
                status = MessageStatus.DELIVERED,
                isOutgoing = false
            )
        )

        val conversationsList = mutableListOf(
            ChatConversation(
                conversationId = "conv_1",
                participantId = "user_2",
                participantName = "Alice",
                participantUsername = "alice",
                lastMessage = "Initial message",
                lastMessageTimestamp = System.currentTimeMillis()
            )
        )

        override fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
            return flowOf(messagesList.filter { it.conversationId == conversationId })
        }

        override fun getConversations(): Flow<List<ChatConversation>> {
            return flowOf(conversationsList)
        }

        override suspend fun sendMessage(
            conversationId: String,
            receiverId: String,
            content: String
        ): Result<ChatMessage> {
            val msg = ChatMessage(
                id = "m_${System.currentTimeMillis()}",
                conversationId = conversationId,
                senderId = "user_1",
                receiverId = receiverId,
                content = content,
                status = MessageStatus.SENT,
                isOutgoing = true
            )
            messagesList.add(msg)
            return Result.success(msg)
        }

        override suspend fun sendMediaMessage(
            conversationId: String,
            receiverId: String,
            mediaUrl: String,
            messageType: String,
            caption: String
        ): Result<ChatMessage> {
            val msg = ChatMessage(
                id = "m_${System.currentTimeMillis()}",
                conversationId = conversationId,
                senderId = "user_1",
                receiverId = receiverId,
                content = mediaUrl,
                status = MessageStatus.SENT,
                isOutgoing = true,
                messageType = messageType
            )
            messagesList.add(msg)
            return Result.success(msg)
        }

        override suspend fun uploadChatMedia(
            fileName: String,
            fileBytes: ByteArray,
            mimeType: String
        ): Result<String> {
            return Result.success("https://fake.url/$fileName")
        }

        override suspend fun retrySendMessage(messageId: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun syncPendingMessages(): Result<Unit> {
            return Result.success(Unit)
        }

        override fun observeIncomingMessages(): Flow<ChatMessage> {
            return flowOf()
        }

        override suspend fun getOrCreateConversation(
            participantId: String,
            name: String,
            username: String,
            avatarUrl: String?
        ): ChatConversation {
            val existing = conversationsList.find { it.participantId == participantId }
            if (existing != null) return existing
            val newConv = ChatConversation(
                conversationId = "conv_$participantId",
                participantId = participantId,
                participantName = name,
                participantUsername = username,
                participantAvatarUrl = avatarUrl
            )
            conversationsList.add(newConv)
            return newConv
        }

        override suspend fun getParticipantProfile(participantId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile? {
            return com.nura.messaging.domain.entities.auth.RemoteUserProfile(
                id = participantId,
                username = "test",
                displayName = "Test Participant",
                about = "HI there i'm using nura",
                avatarUrl = null
            )
        }

        override suspend fun deleteConversation(conversationId: String): Result<Unit> {
            conversationsList.removeAll { it.conversationId == conversationId }
            messagesList.removeAll { it.conversationId == conversationId }
            return Result.success(Unit)
        }

        override suspend fun acceptConversation(conversationId: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun markConversationAsRead(conversationId: String): Result<Unit> {
            return Result.success(Unit)
        }
    }

    private class FakeNotificationService : com.nura.messaging.domain.repositories.notification.NotificationService {
        var currentActiveId: String? = null

        override fun showMessageNotification(
            title: String,
            content: String,
            conversationId: String,
            senderId: String
        ) {}

        override fun setActiveConversation(conversationId: String?) {
            currentActiveId = conversationId
        }

        override fun cancelConversationNotifications(conversationId: String) {}
    }
}
