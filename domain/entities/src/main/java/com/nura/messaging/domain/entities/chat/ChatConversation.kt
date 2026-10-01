package com.nura.messaging.domain.entities.chat

import kotlinx.serialization.Serializable

@Serializable
data class ChatConversation(
    val conversationId: String,
    val participantId: String,
    val participantName: String,
    val participantUsername: String,
    val participantAvatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isProfileShared: Boolean = false,
    val isAccepted: Boolean = false
)
