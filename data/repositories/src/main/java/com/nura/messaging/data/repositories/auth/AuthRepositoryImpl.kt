package com.nura.messaging.data.repositories.auth

import android.net.Uri
import com.nura.messaging.data.remote.auth.SupabaseAuthDataSource
import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.repositories.auth.AuthRepository
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: SupabaseAuthDataSource
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
        return runCatching {
            val user = remoteDataSource.signInWithEmail(email, password)
            user.toDomain()
        }.mapFailure()
    }

    override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<AuthUser> {
        return signUpWithEmail(name = name, username = "", email = email, password = password)
    }

    override suspend fun signUpWithEmail(
        name: String,
        username: String,
        email: String,
        password: String
    ): Result<AuthUser> {
        return runCatching {
            val user = remoteDataSource.signUpWithEmail(name, username, email, password)
            // If email is already registered in Supabase, identities list is empty to prevent duplicates
            if (user != null && user.identities?.isEmpty() == true) {
                throw IllegalStateException("An account with this email already exists. Please log in instead.")
            }
            user?.toDomain() ?: AuthUser(
                id = "",
                email = email,
                name = name,
                username = username,
                isEmailVerified = false
            )
        }.mapFailure()
    }

    override suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return runCatching {
            remoteDataSource.checkUsernameAvailability(username)
        }.mapFailure()
    }

    override suspend fun signInWithGoogle(): Result<AuthUser> {
        return runCatching {
            val user = remoteDataSource.signInWithGoogle()
            user?.toDomain() ?: throw IllegalStateException("Google sign-in cancelled or interrupted")
        }.mapFailure()
    }

    override suspend fun signOut(): Result<Unit> {
        return runCatching {
            remoteDataSource.signOut()
        }.mapFailure()
    }

    override suspend fun sendPasswordReset(email: String): Result<Unit> {
        return runCatching {
            remoteDataSource.sendPasswordReset(email)
        }.mapFailure()
    }

    override suspend fun resendEmailVerification(email: String): Result<Unit> {
        return runCatching {
            remoteDataSource.resendEmailVerification(email)
        }.mapFailure()
    }

    override suspend fun sendEmailOtp(email: String): Result<Unit> {
        return runCatching {
            remoteDataSource.sendEmailOtp(email)
        }.mapFailure()
    }

    override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> {
        return runCatching {
            val user = remoteDataSource.verifyEmailOtp(email, token)
            user.toDomain()
        }.mapFailure()
    }

    override suspend fun getCurrentUser(): AuthUser? {
        return remoteDataSource.getCurrentUser()?.toDomain()
    }

    override fun observeAuthState(): Flow<AuthUser?> {
        return remoteDataSource.observeSessionStatus().map { status ->
            when (status) {
                is SessionStatus.Authenticated -> status.session.user?.toDomain()
                else -> null
            }
        }
    }

    override suspend fun handleDeepLink(uriString: String): Boolean {
        return try {
            val uri = Uri.parse(uriString)
            remoteDataSource.handleDeepLink(uri)
        } catch (_: Exception) {
            false
        }
    }

    private fun UserInfo.toDomain(): AuthUser {
        val fullName = userMetadata?.get("full_name")?.jsonPrimitive?.content
            ?: userMetadata?.get("display_name")?.jsonPrimitive?.content
            ?: email?.substringBefore("@")
            ?: ""
        val username = userMetadata?.get("username")?.jsonPrimitive?.content
            ?: ""

        return AuthUser(
            id = id,
            email = email ?: "",
            name = fullName,
            username = username,
            isEmailVerified = emailConfirmedAt != null,
            createdAt = System.currentTimeMillis()
        )
    }

    private fun <T> Result<T>.mapFailure(): Result<T> {
        return onFailure { error ->
            val mappedMessage = when (error) {
                is RestException -> {
                    val msg = error.message ?: ""
                    when {
                        error.statusCode == 422 ||
                        msg.contains("already", ignoreCase = true) ||
                        msg.contains("registered", ignoreCase = true) ||
                        msg.contains("exists", ignoreCase = true) ->
                            "An account with this email already exists. Please log in instead."
                        error.statusCode == 429 ||
                        msg.contains("rate_limit", ignoreCase = true) ||
                        msg.contains("rate limit", ignoreCase = true) ->
                            "Email rate limit exceeded. Disable 'Confirm email' in Supabase."
                        error.statusCode == 400 -> "Invalid email or password. Please check your credentials."
                        else -> error.message ?: "Authentication service error. Please try again."
                    }
                }
                is HttpRequestException -> "Network connection unavailable. Please check your internet."
                else -> error.message ?: "An unexpected error occurred. Please try again."
            }
            return Result.failure(Exception(mappedMessage, error))
        }
    }
}
