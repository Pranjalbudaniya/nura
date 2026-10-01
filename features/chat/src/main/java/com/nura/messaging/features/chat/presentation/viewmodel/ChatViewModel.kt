package com.nura.messaging.features.chat.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.usecases.chat.GetMessagesUseCase
import com.nura.messaging.domain.usecases.chat.GetOrCreateConversationUseCase
import com.nura.messaging.domain.usecases.chat.ObserveIncomingMessagesUseCase
import com.nura.messaging.domain.usecases.chat.RetrySendMessageUseCase
import com.nura.messaging.domain.usecases.chat.SendMessageUseCase
import com.nura.messaging.domain.usecases.chat.SyncPendingMessagesUseCase
import com.nura.messaging.features.chat.presentation.state.ChatUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val syncPendingMessagesUseCase: SyncPendingMessagesUseCase,
    private val observeIncomingMessagesUseCase: ObserveIncomingMessagesUseCase,
    private val retrySendMessageUseCase: RetrySendMessageUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var realtimeJob: Job? = null

    fun initChat(
        conversationId: String,
        participantId: String,
        participantName: String,
        participantUsername: String,
        participantAvatarUrl: String?
    ) {
        _uiState.update {
            it.copy(
                conversationId = conversationId,
                participantId = participantId,
                participantName = participantName,
                participantUsername = participantUsername,
                participantAvatarUrl = participantAvatarUrl,
                isLoading = true
            )
        }

        // Ensure conversation exists in local database
        viewModelScope.launch(dispatchers.io) {
            getOrCreateConversationUseCase(
                participantId = participantId,
                name = participantName,
                username = participantUsername,
                avatarUrl = participantAvatarUrl
            )
        }

        // Start observing local database messages (the permanent single source of truth)
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch(dispatchers.main) {
            getMessagesUseCase(conversationId).collect { msgList ->
                val hasOutgoing = msgList.any { it.isOutgoing }
                val hasIncoming = msgList.any { !it.isOutgoing }
                val isShared = hasOutgoing && hasIncoming
                _uiState.update {
                    it.copy(
                        messages = msgList,
                        isLoading = false,
                        isProfileShared = isShared
                    )
                }
            }
        }

        // Pull any undelivered messages from Supabase relay
        viewModelScope.launch(dispatchers.io) {
            syncPendingMessagesUseCase()
        }

        // Listen for realtime incoming messages
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch(dispatchers.io) {
            observeIncomingMessagesUseCase().collect {
                // Incoming messages are written to Room and automatically emitted to messagesJob
            }
        }
    }

    fun onInputTextChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
    }

    fun sendMessage() {
        val current = _uiState.value
        val text = current.inputText.trim()
        if (text.isBlank() || current.conversationId.isBlank() || current.participantId.isBlank()) return

        // Clear input immediately for smooth UX
        _uiState.update { it.copy(inputText = "", isSending = true) }

        viewModelScope.launch(dispatchers.io) {
            val result = sendMessageUseCase(
                conversationId = current.conversationId,
                receiverId = current.participantId,
                content = text
            )
            _uiState.update { it.copy(isSending = false) }
            if (result.isFailure) {
                _uiState.update { it.copy(error = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun retryMessage(messageId: String) {
        viewModelScope.launch(dispatchers.io) {
            retrySendMessageUseCase(messageId)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
