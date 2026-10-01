package com.nura.messaging.features.chat.presentation.viewmodel

import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus
import com.nura.messaging.domain.repositories.chat.ChatRepository
import com.nura.messaging.domain.usecases.chat.AcceptConversationUseCase
import com.nura.messaging.domain.usecases.chat.DeleteConversationUseCase
import com.nura.messaging.domain.usecases.chat.GetParticipantProfileUseCase
import com.nura.messaging.domain.usecases.chat.GetConversationsUseCase
import com.nura.messaging.domain.usecases.chat.GetMessagesUseCase
import com.nura.messaging.domain.usecases.chat.GetOrCreateConversationUseCase
import com.nura.messaging.domain.usecases.chat.ObserveIncomingMessagesUseCase
import com.nura.messaging.domain.usecases.chat.RetrySendMessageUseCase
import com.nura.messaging.domain.usecases.chat.SendMessageUseCase
import com.nura.messaging.domain.usecases.chat.SyncPendingMessagesUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

import com.nura.messaging.domain.usecases.chat.MarkConversationAsReadUseCase
import com.nura.messaging.domain.usecases.chat.SetActiveConversationUseCase
import com.nura.messaging.domain.usecases.chat.SendMediaMessageUseCase
import com.nura.messaging.domain.usecases.chat.UploadChatMediaUseCase
import com.nura.messaging.domain.repositories.notification.NotificationService

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository
    private lateinit var fakeNotificationService: FakeNotificationService
    private lateinit var viewModel: ChatViewModel

    private val testDispatcherProvider = object : DispatcherProvider {
        override val main: CoroutineDispatcher = testDispatcher
        override val io: CoroutineDispatcher = testDispatcher
        override val default: CoroutineDispatcher = testDispatcher
        override val unconfined: CoroutineDispatcher = testDispatcher
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeChatRepository()
        fakeNotificationService = FakeNotificationService()
        viewModel = ChatViewModel(
            getMessagesUseCase = GetMessagesUseCase(fakeRepository),
            getOrCreateConversationUseCase = GetOrCreateConversationUseCase(fakeRepository),
            getConversationsUseCase = GetConversationsUseCase(fakeRepository),
            sendMessageUseCase = SendMessageUseCase(fakeRepository),
            syncPendingMessagesUseCase = SyncPendingMessagesUseCase(fakeRepository),
            observeIncomingMessagesUseCase = ObserveIncomingMessagesUseCase(fakeRepository),
            retrySendMessageUseCase = RetrySendMessageUseCase(fakeRepository),
            deleteConversationUseCase = DeleteConversationUseCase(fakeRepository),
            acceptConversationUseCase = AcceptConversationUseCase(fakeRepository),
            getParticipantProfileUseCase = GetParticipantProfileUseCase(fakeRepository),
            markConversationAsReadUseCase = MarkConversationAsReadUseCase(fakeRepository),
            setActiveConversationUseCase = SetActiveConversationUseCase(fakeNotificationService),
            sendMediaMessageUseCase = SendMediaMessageUseCase(fakeRepository),
            uploadChatMediaUseCase = UploadChatMediaUseCase(fakeRepository),
            dispatchers = testDispatcherProvider
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initChat updates state with participant details and loads messages`() = runTest {
        viewModel.initChat("conv_123", "user_456", "Maya Lin", "mayal", null)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("conv_123", state.conversationId)
        assertEquals("user_456", state.participantId)
        assertEquals("Maya Lin", state.participantName)
        assertEquals(1, state.messages.size)
        assertEquals("Hey there!", state.messages[0].content)
        assertEquals("conv_123", fakeNotificationService.currentActiveConversationId)
    }

    @Test
    fun `onInputTextChanged updates inputText`() {
        viewModel.onInputTextChanged("Hello Nura")
        assertEquals("Hello Nura", viewModel.uiState.value.inputText)
    }

    @Test
    fun `emoji selection and backspace properly updates inputText`() {
        viewModel.onEmojiSelected("✨")
        viewModel.onEmojiSelected("🔥")
        assertEquals("✨🔥", viewModel.uiState.value.inputText)

        viewModel.onEmojiBackspace()
        assertEquals("✨", viewModel.uiState.value.inputText)
    }

    @Test
    fun `sendVideo strictly limits video size to 25 MB`() = runTest {
        viewModel.initChat("conv_123", "user_456", "Maya Lin", "mayal", null)
        advanceUntilIdle()

        val largeVideoBytes = ByteArray(100)
        val over25Mb = 26L * 1024L * 1024L
        viewModel.sendVideo(largeVideoBytes, over25Mb)

        val state = viewModel.uiState.value
        assertEquals("Videos cannot be larger than 25 MB.", state.error)
    }

    @Test
    fun `audio recording state transitions correctly`() {
        viewModel.startRecordingAudio()
        assertTrue(viewModel.uiState.value.isRecordingAudio)

        viewModel.cancelRecordingAudio()
        assertEquals(false, viewModel.uiState.value.isRecordingAudio)
        assertEquals(0, viewModel.uiState.value.recordingDurationSeconds)
    }

    @Test
    fun `sendMessage clears input and dispatches message`() = runTest {
        viewModel.initChat("conv_123", "user_456", "Maya Lin", "mayal", null)
        advanceUntilIdle()

        viewModel.onInputTextChanged("New message")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("", state.inputText)
        assertEquals(2, state.messages.size)
        assertEquals("New message", state.messages[1].content)
    }

    @Test
    fun `onReplyMessage and cancelReply properly manage reply state`() = runTest {
        viewModel.initChat("conv_123", "user_456", "Maya Lin", "mayal", null)
        advanceUntilIdle()

        val targetMessage = viewModel.uiState.value.messages[0]
        viewModel.onReplyMessage(targetMessage)
        assertEquals(targetMessage, viewModel.uiState.value.replyingToMessage)

        viewModel.cancelReply()
        assertEquals(null, viewModel.uiState.value.replyingToMessage)
    }

    @Test
    fun `sendMessage with reply attaches reply metadata and clears replyingToMessage`() = runTest {
        viewModel.initChat("conv_123", "user_456", "Maya Lin", "mayal", null)
        advanceUntilIdle()

        val targetMessage = viewModel.uiState.value.messages[0]
        viewModel.onReplyMessage(targetMessage)
        viewModel.onInputTextChanged("Replying back")
        viewModel.sendMessage()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(null, state.replyingToMessage)
        assertEquals(2, state.messages.size)
        val sentMessage = state.messages[1]
        assertEquals("Replying back", sentMessage.content)
        assertEquals("m1", sentMessage.replyToMessageId)
        assertEquals("Hey there!", sentMessage.replyToContent)
        assertEquals("Maya Lin", sentMessage.replyToSenderName)
    }

    private class FakeChatRepository : ChatRepository {
        val messages = mutableListOf(
            ChatMessage(
                id = "m1",
                conversationId = "conv_123",
                senderId = "user_456",
                receiverId = "me",
                content = "Hey there!",
                status = MessageStatus.DELIVERED,
                isOutgoing = false
            )
        )

        override fun getMessages(conversationId: String): Flow<List<ChatMessage>> {
            return flowOf(messages)
        }

        override fun getConversations(): Flow<List<ChatConversation>> = flowOf(emptyList())

        override suspend fun sendMessage(
            conversationId: String,
            receiverId: String,
            content: String,
            replyToMessageId: String?,
            replyToContent: String?,
            replyToSenderName: String?
        ): Result<ChatMessage> {
            val msg = ChatMessage(
                id = "m2",
                conversationId = conversationId,
                senderId = "me",
                receiverId = receiverId,
                content = content,
                status = MessageStatus.SENT,
                isOutgoing = true,
                replyToMessageId = replyToMessageId,
                replyToContent = replyToContent,
                replyToSenderName = replyToSenderName
            )
            messages.add(msg)
            return Result.success(msg)
        }

        override suspend fun sendMediaMessage(
            conversationId: String,
            receiverId: String,
            mediaUrl: String,
            messageType: String,
            caption: String,
            replyToMessageId: String?,
            replyToContent: String?,
            replyToSenderName: String?
        ): Result<ChatMessage> {
            val msg = ChatMessage(
                id = "m_media",
                conversationId = conversationId,
                senderId = "me",
                receiverId = receiverId,
                content = mediaUrl,
                messageType = messageType,
                status = MessageStatus.SENT,
                isOutgoing = true,
                replyToMessageId = replyToMessageId,
                replyToContent = replyToContent,
                replyToSenderName = replyToSenderName
            )
            messages.add(msg)
            return Result.success(msg)
        }

        override suspend fun uploadChatMedia(
            fileName: String,
            fileBytes: ByteArray,
            mimeType: String
        ): Result<String> {
            return Result.success("https://storage.supabase.co/chat_media/$fileName")
        }

        override suspend fun retrySendMessage(messageId: String): Result<Unit> = Result.success(Unit)

        override suspend fun syncPendingMessages(): Result<Unit> = Result.success(Unit)

        override fun observeIncomingMessages(): Flow<ChatMessage> = flowOf()

        override suspend fun getOrCreateConversation(
            participantId: String,
            name: String,
            username: String,
            avatarUrl: String?
        ): ChatConversation {
            return ChatConversation(
                conversationId = "conv_$participantId",
                participantId = participantId,
                participantName = name,
                participantUsername = username
            )
        }

        override suspend fun getParticipantProfile(participantId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile? {
            return com.nura.messaging.domain.entities.auth.RemoteUserProfile(
                id = participantId,
                username = "mayal",
                displayName = "Maya Lin",
                about = "Designer & Maker",
                avatarUrl = null
            )
        }

        override suspend fun deleteConversation(conversationId: String): Result<Unit> {
            messages.clear()
            return Result.success(Unit)
        }

        override suspend fun acceptConversation(conversationId: String): Result<Unit> {
            return Result.success(Unit)
        }

        override suspend fun markConversationAsRead(conversationId: String): Result<Unit> {
            return Result.success(Unit)
        }
    }

    private class FakeNotificationService : NotificationService {
        var currentActiveConversationId: String? = null

        override fun showMessageNotification(
            title: String,
            content: String,
            conversationId: String,
            senderId: String
        ) {}

        override fun setActiveConversation(conversationId: String?) {
            currentActiveConversationId = conversationId
        }

        override fun cancelConversationNotifications(conversationId: String) {}
    }
}
