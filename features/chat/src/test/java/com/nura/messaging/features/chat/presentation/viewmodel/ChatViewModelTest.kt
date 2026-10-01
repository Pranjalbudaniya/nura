package com.nura.messaging.features.chat.presentation.viewmodel

import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus
import com.nura.messaging.domain.repositories.chat.ChatRepository
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

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeChatRepository
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
        viewModel = ChatViewModel(
            getMessagesUseCase = GetMessagesUseCase(fakeRepository),
            getOrCreateConversationUseCase = GetOrCreateConversationUseCase(fakeRepository),
            sendMessageUseCase = SendMessageUseCase(fakeRepository),
            syncPendingMessagesUseCase = SyncPendingMessagesUseCase(fakeRepository),
            observeIncomingMessagesUseCase = ObserveIncomingMessagesUseCase(fakeRepository),
            retrySendMessageUseCase = RetrySendMessageUseCase(fakeRepository),
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
    }
}
