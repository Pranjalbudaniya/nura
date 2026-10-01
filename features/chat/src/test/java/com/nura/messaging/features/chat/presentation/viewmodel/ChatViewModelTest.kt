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
            sendMessageUseCase = SendMessageUseCase(fakeRepository),
            syncPendingMessagesUseCase = SyncPendingMessagesUseCase(fakeRepository),
            observeIncomingMessagesUseCase = ObserveIncomingMessagesUseCase(fakeRepository),
            retrySendMessageUseCase = RetrySendMessageUseCase(fakeRepository),
            deleteConversationUseCase = DeleteConversationUseCase(fakeRepository),
            acceptConversationUseCase = AcceptConversationUseCase(fakeRepository),
            getParticipantProfileUseCase = GetParticipantProfileUseCase(fakeRepository),
            markConversationAsReadUseCase = MarkConversationAsReadUseCase(fakeRepository),
            setActiveConversationUseCase = SetActiveConversationUseCase(fakeNotificationService),
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
            content: String
        ): Result<ChatMessage> {
            val msg = ChatMessage(
                id = "m2",
                conversationId = conversationId,
                senderId = "me",
                receiverId = receiverId,
                content = content,
                status = MessageStatus.SENT,
                isOutgoing = true
            )
            messages.add(msg)
            return Result.success(msg)
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
