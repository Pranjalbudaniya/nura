package com.nura.messaging.data.remote.chat

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DeliveryAckDto(
    @SerialName("message_id")
    val messageId: String,
    @SerialName("receiver_id")
    val receiverId: String
)
