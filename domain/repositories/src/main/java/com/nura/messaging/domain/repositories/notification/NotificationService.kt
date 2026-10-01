package com.nura.messaging.domain.repositories.notification

interface NotificationService {
    fun showMessageNotification(
        title: String,
        content: String,
        conversationId: String,
        senderId: String
    )

    fun setActiveConversation(conversationId: String?)

    fun cancelConversationNotifications(conversationId: String)
}
