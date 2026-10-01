package com.nura.messaging.domain.entities.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val name: String = "",
    val username: String = "",
    val about: String = "HI there i'm using nura",
    val avatarUrl: String? = null,
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class RemoteUserProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val about: String = "HI there i'm using nura",
    val avatarUrl: String? = null
)

enum class UsernameAvailability {
    IDLE,
    CHECKING,
    AVAILABLE,
    TAKEN,
    INVALID,
    ERROR
}

@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val user: AuthUser,
    val expiresAt: Long? = null
)

sealed interface AuthError {
    data object InvalidCredentials : AuthError
    data object UserAlreadyExists : AuthError
    data class WeakPassword(val reason: String) : AuthError
    data object EmailNotConfirmed : AuthError
    data object NetworkError : AuthError
    data object RateLimited : AuthError
    data object Cancelled : AuthError
    data class Unknown(val message: String) : AuthError
}
