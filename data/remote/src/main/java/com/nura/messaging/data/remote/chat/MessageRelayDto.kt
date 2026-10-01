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
    val deliveryStatus: String = "SENT_TO_SERVER",
    @SerialName("sender_name")
    val senderName: String? = null,
    @SerialName("sender_avatar")
    val senderAvatar: String? = null,
    @SerialName("media_url")
    val mediaUrl: String? = null,
    @SerialName("reply_to_message_id")
    val replyToMessageId: String? = null,
    @SerialName("reply_to_content")
    val replyToContent: String? = null,
    @SerialName("reply_to_sender_name")
    val replyToSenderName: String? = null
)

@Serializable
data class MessageRelayTableDto(
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

fun MessageRelayDto.toTableDto(): MessageRelayTableDto = MessageRelayTableDto(
    messageId = messageId,
    senderId = senderId,
    receiverId = receiverId,
    conversationId = conversationId,
    content = content,
    messageType = messageType,
    createdAt = createdAt,
    deliveryStatus = deliveryStatus
)

@Serializable
data class ConversationAcceptedDto(
    @SerialName("conversation_id")
    val conversationId: String,
    @SerialName("acceptor_id")
    val acceptorId: String,
    @SerialName("acceptor_name")
    val acceptorName: String,
    @SerialName("acceptor_username")
    val acceptorUsername: String,
    @SerialName("acceptor_avatar_url")
    val acceptorAvatarUrl: String? = null
)

