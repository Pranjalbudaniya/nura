package com.nura.messaging.features.contacts.presentation.state

import com.nura.messaging.domain.entities.contacts.ConnectionUser

data class ConnectUiState(
    val currentUserId: String = "",
    val currentUserHandle: String = "",
    val userKey: String = "",
    val usernameInput: String = "",
    val isConnecting: Boolean = false,
    val recentConnections: List<ConnectionUser> = emptyList(),
    val toastMessage: String? = null,
    val showToast: Boolean = false,
    val showYourQrSheet: Boolean = false,
    val showScanner: Boolean = false
)
