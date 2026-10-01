package com.nura.messaging.core.database.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.nura.messaging.domain.entities.chat.ChatConversation

@Entity(
    tableName = "conversations",
    indices = [
        Index(value = ["participantId"], unique = true),
        Index(value = ["lastMessageTimestamp"])
    ]
)
data class ConversationEntity(
    @PrimaryKey
    val conversationId: String,
    val participantId: String,
    val participantName: String,
    val participantUsername: String,
    val participantAvatarUrl: String? = null,
    val lastMessage: String? = null,
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0
) {
    fun toDomain(): ChatConversation {
        return ChatConversation(
            conversationId = conversationId,
            participantId = participantId,
            participantName = participantName,
            participantUsername = participantUsername,
            participantAvatarUrl = participantAvatarUrl,
            lastMessage = lastMessage,
            lastMessageTimestamp = lastMessageTimestamp,
            unreadCount = unreadCount
        )
    }

    companion object {
        fun fromDomain(domain: ChatConversation): ConversationEntity {
            return ConversationEntity(
                conversationId = domain.conversationId,
                participantId = domain.participantId,
                participantName = domain.participantName,
                participantUsername = domain.participantUsername,
                participantAvatarUrl = domain.participantAvatarUrl,
                lastMessage = domain.lastMessage,
                lastMessageTimestamp = domain.lastMessageTimestamp,
                unreadCount = domain.unreadCount
            )
        }
    }
}
