package com.nura.messaging.domain.entities.auth

import kotlinx.serialization.Serializable

@Serializable
data class AuthUser(
    val id: String,
    val email: String,
    val name: String = "",
    val username: String = "",
    val isEmailVerified: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
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
