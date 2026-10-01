package com.nura.messaging.domain.repositories.auth

import com.nura.messaging.domain.entities.auth.AuthUser
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun signInWithEmail(email: String, password: String): Result<AuthUser>
    suspend fun checkUsernameAvailability(username: String): Result<Boolean>
    suspend fun signUpWithEmail(name: String, username: String, email: String, password: String): Result<AuthUser>
    suspend fun signUpWithEmail(name: String, email: String, password: String): Result<AuthUser> =
        signUpWithEmail(name, name.lowercase().replace(" ", "_"), email, password)
    suspend fun signInWithGoogle(): Result<AuthUser>
    suspend fun signOut(): Result<Unit>
    suspend fun sendPasswordReset(email: String): Result<Unit>
    suspend fun resendEmailVerification(email: String): Result<Unit>
    suspend fun sendEmailOtp(email: String): Result<Unit>
    suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser>
    suspend fun getCurrentUser(): AuthUser?
    fun observeAuthState(): Flow<AuthUser?>
    suspend fun handleDeepLink(uriString: String): Boolean
    suspend fun updateProfile(name: String, about: String, avatarUrl: String? = null): Result<AuthUser>
    suspend fun getRemoteUserProfile(userId: String): Result<com.nura.messaging.domain.entities.auth.RemoteUserProfile?>
}
