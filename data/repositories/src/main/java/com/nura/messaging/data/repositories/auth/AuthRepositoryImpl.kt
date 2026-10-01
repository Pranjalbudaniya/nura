package com.nura.messaging.data.repositories.auth

import android.content.Context
import android.net.Uri
import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.core.database.NuraDatabase
import com.nura.messaging.data.local.contacts.ConnectionsLocalDataSource
import com.nura.messaging.data.remote.auth.SupabaseAuthDataSource
import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.repositories.auth.AuthRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import io.github.jan.supabase.exceptions.HttpRequestException
import io.github.jan.supabase.exceptions.RestException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonPrimitive
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val remoteDataSource: SupabaseAuthDataSource,
    private val database: NuraDatabase,
    private val localContactsSource: ConnectionsLocalDataSource,
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider
) : AuthRepository {

    override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> = withContext(dispatchers.io) {
        runCatching {
            val user = remoteDataSource.signInWithEmail(email, password)
            val domain = user.toDomain()
            checkAndIsolateAccount(domain.id)
            domain
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
    ): Result<AuthUser> = withContext(dispatchers.io) {
        runCatching {
            val user = remoteDataSource.signUpWithEmail(name, username, email, password)
            // If email is already registered in Supabase, identities list is empty to prevent duplicates
            if (user != null && user.identities?.isEmpty() == true) {
                throw IllegalStateException("An account with this email already exists. Please log in instead.")
            }
            val domain = user?.toDomain() ?: AuthUser(
                id = "",
                email = email,
                name = name,
                username = username,
                isEmailVerified = false
            )
            if (domain.id.isNotBlank()) {
                checkAndIsolateAccount(domain.id)
            }
            domain
        }.mapFailure()
    }

    override suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
        return runCatching {
            remoteDataSource.checkUsernameAvailability(username)
        }.mapFailure()
    }

    override suspend fun signInWithGoogle(): Result<AuthUser> = withContext(dispatchers.io) {
        runCatching {
            val user = remoteDataSource.signInWithGoogle()
            val domain = user?.toDomain() ?: throw IllegalStateException("Google sign-in cancelled or interrupted")
            checkAndIsolateAccount(domain.id)
            domain
        }.mapFailure()
    }

    override suspend fun signOut(): Result<Unit> = withContext(dispatchers.io) {
        runCatching {
            try {
                database.clearAllTables()
            } catch (_: Exception) {}
            try {
                localContactsSource.clearAllConnections()
            } catch (_: Exception) {}
            clearActiveUserId()
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

    override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> = withContext(dispatchers.io) {
        runCatching {
            val user = remoteDataSource.verifyEmailOtp(email, token)
            val domain = user.toDomain()
            checkAndIsolateAccount(domain.id)
            domain
        }.mapFailure()
    }

    override suspend fun getCurrentUser(): AuthUser? = withContext(dispatchers.io) {
        val user = remoteDataSource.awaitInitialSession()?.toDomain()
        if (user != null && user.id.isNotBlank()) {
            checkAndIsolateAccount(user.id)
        }
        user
    }

    override fun observeAuthState(): Flow<AuthUser?> {
        return remoteDataSource.observeSessionStatus()
            .filter { it !is SessionStatus.Initializing }
            .map { status ->
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

    override suspend fun updateProfile(name: String, about: String, avatarUrl: String?): Result<AuthUser> = withContext(dispatchers.io) {
        runCatching {
            val current = remoteDataSource.getCurrentUser() ?: throw IllegalStateException("Not authenticated")
            val updated = remoteDataSource.updateProfile(current.id, name, about, avatarUrl)
            val domain = (updated ?: current).toDomain()
            domain.copy(
                name = name,
                about = about,
                avatarUrl = avatarUrl ?: domain.avatarUrl
            )
        }.mapFailure()
    }

    override suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): Result<String> = withContext(dispatchers.io) {
        runCatching {
            remoteDataSource.uploadAvatar(userId, imageBytes)
                ?: throw IllegalStateException("Failed to upload avatar to storage")
        }.mapFailure()
    }

    override suspend fun getRemoteUserProfile(userId: String): Result<com.nura.messaging.domain.entities.auth.RemoteUserProfile?> = runCatching {
        remoteDataSource.fetchRemoteProfile(userId)
    }.mapFailure()

    private suspend fun checkAndIsolateAccount(newUserId: String) {
        if (newUserId.isBlank()) return
        val lastUserId = getActiveUserId()
        if (lastUserId != null && lastUserId != newUserId) {
            try {
                database.clearAllTables()
            } catch (_: Exception) {}
            try {
                localContactsSource.clearAllConnections()
            } catch (_: Exception) {}
        }
        saveActiveUserId(newUserId)
    }

    private fun getActiveUserId(): String? {
        val prefs = context.getSharedPreferences("nura_auth_state", Context.MODE_PRIVATE)
        return prefs.getString("last_active_user_id", null)
    }

    private fun saveActiveUserId(userId: String) {
        val prefs = context.getSharedPreferences("nura_auth_state", Context.MODE_PRIVATE)
        prefs.edit().putString("last_active_user_id", userId).apply()
    }

    private fun clearActiveUserId() {
        val prefs = context.getSharedPreferences("nura_auth_state", Context.MODE_PRIVATE)
        prefs.edit().remove("last_active_user_id").apply()
    }

    private fun UserInfo.toDomain(): AuthUser {
        val fullName = userMetadata?.get("full_name")?.jsonPrimitive?.content
            ?: userMetadata?.get("display_name")?.jsonPrimitive?.content
            ?: email?.substringBefore("@")
            ?: ""
        val username = userMetadata?.get("username")?.jsonPrimitive?.content
            ?: ""
        val aboutText = userMetadata?.get("about")?.jsonPrimitive?.content
            ?: "HI there i'm using nura"
        val avatar = userMetadata?.get("avatar_url")?.jsonPrimitive?.content

        return AuthUser(
            id = id,
            email = email ?: "",
            name = fullName,
            username = username,
            about = aboutText,
            avatarUrl = avatar,
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
