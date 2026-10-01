package com.nura.messaging.features.home.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.entities.chat.ChatConversation
import com.nura.messaging.domain.usecases.chat.GetConversationsUseCase
import com.nura.messaging.domain.usecases.chat.ObserveIncomingMessagesUseCase
import com.nura.messaging.domain.usecases.chat.SyncPendingMessagesUseCase
import com.nura.messaging.features.home.presentation.HomeConversationItem
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getConversationsUseCase: GetConversationsUseCase,
    private val syncPendingMessagesUseCase: SyncPendingMessagesUseCase,
    private val observeIncomingMessagesUseCase: ObserveIncomingMessagesUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    val conversations: StateFlow<List<HomeConversationItem>> = getConversationsUseCase()
        .map { list -> list.map { it.toHomeItem() } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Sync pending messages and start realtime listener for incoming messages
        syncAndListen()
    }

    fun refresh() {
        viewModelScope.launch(dispatchers.io) {
            _isRefreshing.value = true
            syncPendingMessagesUseCase()
            _isRefreshing.value = false
        }
    }

    private fun syncAndListen() {
        // 1. Fetch undelivered messages from Supabase relay
        viewModelScope.launch(dispatchers.io) {
            syncPendingMessagesUseCase()
        }

        // 2. Listen to incoming realtime messages
        viewModelScope.launch(dispatchers.io) {
            observeIncomingMessagesUseCase().collect {
                // Room update will automatically trigger getConversationsUseCase flow emission
            }
        }
    }

    private fun ChatConversation.toHomeItem(): HomeConversationItem {
        val now = System.currentTimeMillis()
        val formattedTime = if (lastMessageTimestamp > 0L) {
            val diff = now - lastMessageTimestamp
            if (diff < 24 * 60 * 60 * 1000L) {
                timeFormat.format(Date(lastMessageTimestamp))
            } else {
                dateFormat.format(Date(lastMessageTimestamp))
            }
        } else {
            ""
        }

        val initials = participantName.take(2).uppercase(Locale.getDefault()).ifEmpty { "?" }

        return HomeConversationItem(
            id = conversationId,
            name = participantName,
            avatarUrl = participantAvatarUrl,
            initials = initials,
            lastMessage = lastMessage ?: "No messages yet",
            timestamp = formattedTime,
            unreadCount = unreadCount,
            participantId = participantId,
            participantUsername = participantUsername,
            isProfileShared = isProfileShared,
            isAccepted = isAccepted
        )
    }
}
