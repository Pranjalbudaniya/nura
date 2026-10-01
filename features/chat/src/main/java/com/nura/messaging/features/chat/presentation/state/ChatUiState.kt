package com.nura.messaging.features.chat.presentation.state

import androidx.compose.runtime.Immutable
import com.nura.messaging.domain.entities.chat.ChatMessage

@Immutable
data class ChatUiState(
    val conversationId: String = "",
    val participantId: String = "",
    val participantName: String = "",
    val participantUsername: String = "",
    val participantAvatarUrl: String? = null,
    val participantAbout: String = "HI there i'm using nura",
    val inputText: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isSending: Boolean = false,
    val isUploadingMedia: Boolean = false,
    val isProfileShared: Boolean = false,
    val isRequest: Boolean = false,
    val isAccepted: Boolean = false,
    val isEmojiPickerVisible: Boolean = false,
    val isRecordingAudio: Boolean = false,
    val recordingDurationSeconds: Int = 0,
    val error: String? = null

)
