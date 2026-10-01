package com.nura.messaging.domain.entities.contacts

import kotlinx.serialization.Serializable

@Serializable
data class ConnectionUser(
    val id: String,
    val username: String,
    val displayName: String = "",
    val avatarUri: String? = null,
    val isPro: Boolean = false,
    val connectedAt: Long = System.currentTimeMillis()
)
