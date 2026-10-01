package com.nura.messaging.features.contacts.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.domain.usecases.contacts.ConnectUserUseCase
import com.nura.messaging.domain.usecases.contacts.GetRecentConnectionsUseCase
import com.nura.messaging.features.contacts.presentation.state.ConnectUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface ConnectUiEvent {
    data class NavigateToChat(val user: ConnectionUser) : ConnectUiEvent
}

@HiltViewModel
class ConnectViewModel @Inject constructor(
    private val getRecentConnectionsUseCase: GetRecentConnectionsUseCase,
    private val connectUserUseCase: ConnectUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConnectUiState())
    val uiState: StateFlow<ConnectUiState> = _uiState.asStateFlow()

    fun initialize(userId: String, username: String, name: String) {
        val cleanHandle = when {
            username.isNotBlank() -> if (username.startsWith("@")) username else "@$username"
            name.isNotBlank() -> "@${name.lowercase().replace(" ", "_")}"
            else -> "@user"
        }
        val generatedKey = "NU-" + (userId.take(4).uppercase().ifEmpty { "7K4P" }) +
                "-" + (userId.takeLast(4).uppercase().ifEmpty { "92MX" })

        _uiState.update {
            it.copy(
                currentUserId = userId,
                currentUserHandle = cleanHandle,
                userKey = generatedKey
            )
        }
        loadRecentConnections(userId)
    }

    fun loadRecentConnections(userId: String = _uiState.value.currentUserId) {
        if (userId.isBlank()) return
        viewModelScope.launch {
            val connections = getRecentConnectionsUseCase(userId)
            _uiState.update { it.copy(recentConnections = connections) }
        }
    }

    fun onUsernameChanged(input: String) {
        _uiState.update { it.copy(usernameInput = input) }
    }

    fun onConnectClicked() {
        val input = _uiState.value.usernameInput.trim()
        if (input.isBlank()) {
            showToast("Please enter a valid Nura username")
            return
        }

        connectWithUsername(input)
    }

    private val _events = MutableSharedFlow<ConnectUiEvent>()
    val events: SharedFlow<ConnectUiEvent> = _events.asSharedFlow()

    fun onQrScanned(qrContent: String) {
        _uiState.update { it.copy(showScanner = false) }
        val parsed = parseQrPayload(qrContent)
        if (parsed.first.isNotBlank()) {
            _uiState.update { it.copy(usernameInput = "@${parsed.first}") }
            connectWithUsername(
                username = parsed.first,
                preferredUserId = parsed.second,
                autoOpenChat = true
            )
        } else {
            showToast("Invalid QR code format")
        }
    }

    private fun parseQrPayload(content: String): Pair<String, String?> {
        return try {
            if (content.startsWith("nura://user/")) {
                val path = content.removePrefix("nura://user/")
                val username = path.substringBefore("?").trim().removePrefix("@")
                val query = path.substringAfter("?", "")
                var userId: String? = null
                if (query.isNotBlank()) {
                    for (param in query.split("&")) {
                        val kv = param.split("=")
                        if (kv.size == 2 && kv[0] == "id" && kv[1].isNotBlank()) {
                            userId = kv[1].trim()
                        }
                    }
                }
                Pair(username, userId)
            } else {
                Pair(content.trim().removePrefix("@").substringBefore(" "), null)
            }
        } catch (_: Exception) {
            Pair(content.trim().removePrefix("@"), null)
        }
    }

    private fun connectWithUsername(
        username: String,
        preferredUserId: String? = null,
        autoOpenChat: Boolean = false
    ) {
        val userId = _uiState.value.currentUserId
        _uiState.update { it.copy(isConnecting = true) }

        viewModelScope.launch {
            val result = connectUserUseCase(
                currentUserId = userId,
                targetUsername = username,
                preferredUserId = preferredUserId
            )
            result.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isConnecting = false,
                        usernameInput = ""
                    )
                }
                loadRecentConnections(userId)
                showToast("Connected securely to @${user.username}")
                if (autoOpenChat) {
                    _events.emit(ConnectUiEvent.NavigateToChat(user))
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isConnecting = false) }
                showToast(error.message ?: "Failed to connect")
            }
        }
    }

    fun setShowYourQr(show: Boolean) {
        _uiState.update { it.copy(showYourQrSheet = show) }
    }

    fun setShowScanner(show: Boolean) {
        _uiState.update { it.copy(showScanner = show) }
    }

    fun showToast(message: String) {
        _uiState.update {
            it.copy(
                toastMessage = message,
                showToast = true
            )
        }
    }

    fun dismissToast() {
        _uiState.update { it.copy(showToast = false) }
    }
}
