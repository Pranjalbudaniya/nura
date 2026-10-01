package com.nura.messaging.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus

@Entity(
    tableName = "messages",
    indices = [
        Index(value = ["conversationId"]),
        Index(value = ["timestamp"]),
        Index(value = ["status"])
    ]
)
data class MessageEntity(
    @PrimaryKey
    val messageId: String,
    val conversationId: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val messageType: String = "text",
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = MessageStatus.PENDING.name,
    val isOutgoing: Boolean = true,
    val replyToMessageId: String? = null,
    val replyToContent: String? = null,
    val replyToSenderName: String? = null
) {
    fun toDomain(): ChatMessage {
        return ChatMessage(
            id = messageId,
            conversationId = conversationId,
            senderId = senderId,
            receiverId = receiverId,
            content = content,
            messageType = messageType,
            timestamp = timestamp,
            status = runCatching { MessageStatus.valueOf(status) }.getOrDefault(MessageStatus.PENDING),
            isOutgoing = isOutgoing,
            replyToMessageId = replyToMessageId,
            replyToContent = replyToContent,
            replyToSenderName = replyToSenderName
        )
    }

    companion object {
        fun fromDomain(domain: ChatMessage): MessageEntity {
            return MessageEntity(
                messageId = domain.id,
                conversationId = domain.conversationId,
                senderId = domain.senderId,
                receiverId = domain.receiverId,
                content = domain.content,
                messageType = domain.messageType,
                timestamp = domain.timestamp,
                status = domain.status.name,
                isOutgoing = domain.isOutgoing,
                replyToMessageId = domain.replyToMessageId,
                replyToContent = domain.replyToContent,
                replyToSenderName = domain.replyToSenderName
            )
        }
    }
}
