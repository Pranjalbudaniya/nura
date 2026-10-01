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
import com.nura.messaging.domain.usecases.chat.AcceptConversationUseCase
import com.nura.messaging.domain.usecases.chat.DeleteConversationUseCase
import com.nura.messaging.domain.usecases.chat.GetParticipantProfileUseCase
import com.nura.messaging.domain.usecases.chat.MarkConversationAsReadUseCase
import com.nura.messaging.domain.usecases.chat.SetActiveConversationUseCase
import com.nura.messaging.domain.usecases.chat.SendMediaMessageUseCase
import com.nura.messaging.domain.usecases.chat.UploadChatMediaUseCase
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val getMessagesUseCase: GetMessagesUseCase,
    private val getOrCreateConversationUseCase: GetOrCreateConversationUseCase,
    private val sendMessageUseCase: SendMessageUseCase,
    private val sendMediaMessageUseCase: SendMediaMessageUseCase,
    private val uploadChatMediaUseCase: UploadChatMediaUseCase,
    private val syncPendingMessagesUseCase: SyncPendingMessagesUseCase,
    private val observeIncomingMessagesUseCase: ObserveIncomingMessagesUseCase,
    private val retrySendMessageUseCase: RetrySendMessageUseCase,
    private val deleteConversationUseCase: DeleteConversationUseCase,
    private val acceptConversationUseCase: AcceptConversationUseCase,
    private val getParticipantProfileUseCase: GetParticipantProfileUseCase,
    private val markConversationAsReadUseCase: MarkConversationAsReadUseCase,
    private val setActiveConversationUseCase: SetActiveConversationUseCase,
    private val dispatchers: DispatcherProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesJob: Job? = null
    private var realtimeJob: Job? = null
    private var recordingTimerJob: Job? = null


    fun initChat(
        conversationId: String,
        participantId: String,
        participantName: String,
        participantUsername: String,
        participantAvatarUrl: String?,
        participantAbout: String? = null,
        isRequest: Boolean = false
    ) {
        _uiState.update {
            it.copy(
                conversationId = conversationId,
                participantId = participantId,
                participantName = participantName,
                participantUsername = participantUsername,
                participantAvatarUrl = participantAvatarUrl,
                participantAbout = participantAbout ?: "HI there i'm using nura",
                isRequest = isRequest,
                isAccepted = !isRequest,
                isLoading = true
            )
        }

        setActiveConversationUseCase(conversationId)

        // Fetch fresh remote profile from Supabase to ensure accurate real name, about, and avatar
        viewModelScope.launch(dispatchers.io) {
            val remote = getParticipantProfileUseCase(participantId)
            if (remote != null) {
                _uiState.update { current ->
                    current.copy(
                        participantName = remote.displayName.ifBlank { current.participantName },
                        participantUsername = remote.username.ifBlank { current.participantUsername },
                        participantAbout = remote.about.ifBlank { current.participantAbout },
                        participantAvatarUrl = remote.avatarUrl ?: current.participantAvatarUrl
                    )
                }
            }
        }

        // Ensure conversation exists in local database
        viewModelScope.launch(dispatchers.io) {
            val conv = getOrCreateConversationUseCase(
                participantId = participantId,
                name = participantName,
                username = participantUsername,
                avatarUrl = participantAvatarUrl
            )
            if (conv.isAccepted) {
                _uiState.update { it.copy(isAccepted = true, isProfileShared = true) }
            }
        }

        // Start observing local database messages (the permanent single source of truth)
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch(dispatchers.main) {
            getMessagesUseCase(conversationId).collect { msgList ->
                val hasOutgoing = msgList.any { it.isOutgoing }
                val hasIncoming = msgList.any { !it.isOutgoing }
                _uiState.update { current ->
                    val isShared = current.isAccepted || (hasOutgoing && hasIncoming)
                    current.copy(
                        messages = msgList,
                        isLoading = false,
                        isProfileShared = isShared
                    )
                }
            }
        }

        // Mark conversation as read immediately on open
        viewModelScope.launch(dispatchers.io) {
            markConversationAsReadUseCase(conversationId)
        }

        // Pull any undelivered messages from Supabase relay
        viewModelScope.launch(dispatchers.io) {
            syncPendingMessagesUseCase()
        }

        // Listen for realtime incoming messages
        realtimeJob?.cancel()
        realtimeJob = viewModelScope.launch(dispatchers.io) {
            observeIncomingMessagesUseCase().collect { msg ->
                if (msg.conversationId == _uiState.value.conversationId) {
                    markConversationAsReadUseCase(msg.conversationId)
                }
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

        // Clear input immediately for smooth UX, and if it was a request, mark as accepted & shared
        _uiState.update {
            it.copy(
                inputText = "",
                isSending = true,
                isAccepted = true,
                isProfileShared = true
            )
        }

        viewModelScope.launch(dispatchers.io) {
            acceptConversationUseCase(current.conversationId)
            markConversationAsReadUseCase(current.conversationId)
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

    fun acceptRequest() {
        val convId = _uiState.value.conversationId
        _uiState.update { it.copy(isAccepted = true, isProfileShared = true) }
        viewModelScope.launch(dispatchers.io) {
            acceptConversationUseCase(convId)
            markConversationAsReadUseCase(convId)
        }
    }

    fun rejectRequest(onRejected: () -> Unit) {
        val convId = _uiState.value.conversationId
        viewModelScope.launch(dispatchers.io) {
            deleteConversationUseCase(convId)
            viewModelScope.launch(dispatchers.main) {
                onRejected()
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

    fun toggleEmojiPicker() {
        _uiState.update { it.copy(isEmojiPickerVisible = !it.isEmojiPickerVisible) }
    }

    fun setEmojiPickerVisible(visible: Boolean) {
        _uiState.update { it.copy(isEmojiPickerVisible = visible) }
    }

    fun onEmojiSelected(emoji: String) {
        _uiState.update { it.copy(inputText = it.inputText + emoji) }
    }

    fun onEmojiBackspace() {
        _uiState.update {
            if (it.inputText.isNotEmpty()) {
                val length = it.inputText.length
                val lastChar = it.inputText.last()
                val dropCount = if (Character.isSurrogate(lastChar) && length >= 2) 2 else 1
                it.copy(inputText = it.inputText.dropLast(dropCount))
            } else {
                it
            }
        }
    }

    fun sendMedia(bytes: ByteArray, mimeType: String, messageType: String, extension: String) {
        val current = _uiState.value
        if (current.conversationId.isBlank() || current.participantId.isBlank()) return

        _uiState.update {
            it.copy(
                isUploadingMedia = true,
                isAccepted = true,
                isProfileShared = true
            )
        }

        viewModelScope.launch(dispatchers.io) {
            acceptConversationUseCase(current.conversationId)
            markConversationAsReadUseCase(current.conversationId)

            val fileName = "chat_${java.util.UUID.randomUUID()}.$extension"
            val uploadResult = uploadChatMediaUseCase(fileName, bytes, mimeType)
            if (uploadResult.isSuccess) {
                val mediaUrl = uploadResult.getOrThrow()
                val sendResult = sendMediaMessageUseCase(
                    conversationId = current.conversationId,
                    receiverId = current.participantId,
                    mediaUrl = mediaUrl,
                    messageType = messageType
                )
                _uiState.update { it.copy(isUploadingMedia = false) }
                if (sendResult.isFailure) {
                    _uiState.update { it.copy(error = sendResult.exceptionOrNull()?.message) }
                }
            } else {
                _uiState.update {
                    it.copy(
                        isUploadingMedia = false,
                        error = "Failed to upload $messageType: ${uploadResult.exceptionOrNull()?.message}"
                    )
                }
            }
        }
    }

    fun sendVideo(bytes: ByteArray, sizeBytes: Long, mimeType: String = "video/mp4") {
        val maxSizeBytes = 25L * 1024L * 1024L // 25 MB strict limit
        if (sizeBytes > maxSizeBytes || bytes.size > maxSizeBytes) {
            _uiState.update { it.copy(error = "Videos cannot be larger than 25 MB.") }
            return
        }
        sendMedia(bytes, mimeType, "video", "mp4")
    }

    fun sendPhoto(bytes: ByteArray, mimeType: String = "image/jpeg") {
        sendMedia(bytes, mimeType, "image", "jpg")
    }

    fun sendPhotoUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(dispatchers.io) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
                sendPhoto(bytes, mimeType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load photo: ${e.message}") }
            }
        }
    }

    fun sendVideoUri(context: android.content.Context, uri: android.net.Uri) {
        viewModelScope.launch(dispatchers.io) {
            try {
                val cursor = context.contentResolver.query(uri, null, null, null, null)
                val size = cursor?.use {
                    if (it.moveToFirst()) {
                        val sizeIndex = it.getColumnIndex(android.provider.OpenableColumns.SIZE)
                        if (sizeIndex != -1) it.getLong(sizeIndex) else null
                    } else null
                } ?: 0L
                val maxSizeBytes = 25L * 1024L * 1024L
                if (size > maxSizeBytes) {
                    _uiState.update { it.copy(error = "Videos cannot be larger than 25 MB.") }
                    return@launch
                }
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: return@launch
                if (bytes.size > maxSizeBytes) {
                    _uiState.update { it.copy(error = "Videos cannot be larger than 25 MB.") }
                    return@launch
                }
                val mimeType = context.contentResolver.getType(uri) ?: "video/mp4"
                sendVideo(bytes, size, mimeType)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to load video: ${e.message}") }
            }
        }
    }

    fun startRecordingAudio() {
        recordingTimerJob?.cancel()
        _uiState.update { it.copy(isRecordingAudio = true, recordingDurationSeconds = 0) }
        recordingTimerJob = viewModelScope.launch(dispatchers.main) {
            while (true) {
                kotlinx.coroutines.delay(1000)
                _uiState.update { it.copy(recordingDurationSeconds = it.recordingDurationSeconds + 1) }
            }
        }
    }

    fun cancelRecordingAudio() {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _uiState.update { it.copy(isRecordingAudio = false, recordingDurationSeconds = 0) }
    }

    fun sendVoiceNote(file: java.io.File, durationSeconds: Int = 0) {
        recordingTimerJob?.cancel()
        recordingTimerJob = null
        _uiState.update { it.copy(isRecordingAudio = false, recordingDurationSeconds = 0) }
        viewModelScope.launch(dispatchers.io) {
            val bytes = file.readBytes()
            sendMedia(bytes, "audio/mp4", "audio", "m4a")
            file.delete()
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingTimerJob?.cancel()
        setActiveConversationUseCase(null)
    }
}

