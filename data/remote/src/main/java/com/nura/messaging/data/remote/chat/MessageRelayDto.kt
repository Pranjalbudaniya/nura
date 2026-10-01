package com.nura.messaging.data.remote.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MessageRelayDto(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("sender_id")
    val senderId: String,
    @SerialName("receiver_id")
    val receiverId: String,
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("content")
    val content: String,
    @SerialName("message_type")
    val messageType: String = "text",
    @SerialName("created_at")
    val createdAt: Long = System.currentTimeMillis(),
    @SerialName("delivery_status")
    val deliveryStatus: String = "SENT_TO_SERVER"
)
