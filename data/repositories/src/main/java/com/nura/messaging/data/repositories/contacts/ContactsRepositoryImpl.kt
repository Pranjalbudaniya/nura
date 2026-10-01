package com.nura.messaging.data.repositories.contacts

import com.nura.messaging.core.network.SupabaseConfig
import com.nura.messaging.data.local.contacts.ConnectionsLocalDataSource
import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.domain.repositories.contacts.ContactsRepository
import io.github.jan.supabase.auth.Auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val localDataSource: ConnectionsLocalDataSource,
    private val okHttpClient: OkHttpClient,
    private val auth: Auth
) : ContactsRepository {

    private val json = Json { ignoreUnknownKeys = true }

    override suspend fun getRecentConnections(currentUserId: String): List<ConnectionUser> {
        return localDataSource.getRecentConnections(currentUserId)
    }

    override suspend fun connectUser(
        currentUserId: String,
        targetUsername: String,
        preferredUserId: String?
    ): Result<ConnectionUser> = withContext(Dispatchers.IO) {
        val clean = targetUsername.trim().lowercase().removePrefix("@")
        if (clean.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("Username cannot be empty"))
        }

        var remoteId: String? = preferredUserId
        var remoteName: String? = null
        var remoteAvatarUrl: String? = null

        val token = auth.currentAccessTokenOrNull()
        val authHeader = if (!token.isNullOrBlank()) "Bearer $token" else "Bearer ${SupabaseConfig.DEFAULT_ANON_KEY}"

        // Query user profile from Supabase PostgREST
        try {
            val queryParam = if (!preferredUserId.isNullOrBlank()) {
                "id=eq.$preferredUserId"
            } else {
                "username=ilike.$clean"
            }
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/rest/v1/profiles?$queryParam&select=id,username,display_name,avatar_url"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", authHeader)
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank() && body.trim() != "[]") {
                        val array = json.parseToJsonElement(body).jsonArray
                        if (array.isNotEmpty()) {
                            val obj = array[0].jsonObject
                            remoteId = obj["id"]?.jsonPrimitive?.content ?: remoteId
                            remoteName = obj["display_name"]?.jsonPrimitive?.content
                            remoteAvatarUrl = obj["avatar_url"]?.jsonPrimitive?.content
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Network fallback
        }

        // ONLY ADD USER IF THEY ACTUALLY EXIST
        if (remoteId.isNullOrBlank()) {
            return@withContext Result.failure(
                NoSuchElementException("User @$clean was not found or is unavailable.")
            )
        }

        val myId = auth.currentUserOrNull()?.id ?: currentUserId
        if (remoteId == myId) {
            return@withContext Result.failure(
                IllegalArgumentException("You cannot connect with your own account.")
            )
        }

        val displayName = when {
            !remoteName.isNullOrBlank() -> remoteName!!
            else -> clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

        // If remote user has avatar, download and save locally to app data
        var localAvatarUri: String? = null
        if (!remoteAvatarUrl.isNullOrBlank()) {
            try {
                val avatarReq = Request.Builder().url(remoteAvatarUrl!!).get().build()
                okHttpClient.newCall(avatarReq).execute().use { avResp ->
                    if (avResp.isSuccessful) {
                        val bytes = avResp.body?.bytes()
                        if (bytes != null && bytes.isNotEmpty()) {
                            localAvatarUri = localDataSource.saveConnectedUserAvatar(
                                userId = currentUserId,
                                connectedUserId = remoteId!!,
                                imageBytes = bytes
                            )
                        }
                    }
                }
            } catch (_: Exception) {
                // Fall back to null if download fails
            }
        }

        val connection = ConnectionUser(
            id = remoteId!!,
            username = clean,
            displayName = displayName,
            avatarUri = localAvatarUri,
            connectedAt = System.currentTimeMillis()
        )

        // Persist to local app data for current user
        localDataSource.saveConnection(currentUserId, connection)

        Result.success(connection)
    }

    override suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String? {
        return localDataSource.saveUserProfilePicture(userId, sourceUriString)
    }

    override suspend fun getLocalProfilePicture(userId: String): String? {
        return localDataSource.getLocalProfilePicture(userId)
    }

    override suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int) {
        localDataSource.saveUserPresetAvatar(userId, presetIndex)
    }

    override suspend fun saveUserPresetColor(userId: String, colorArgb: Long) {
        localDataSource.saveUserPresetColor(userId, colorArgb)
    }

    override suspend fun getUserPresetIndex(userId: String): Int? {
        return localDataSource.getUserPresetIndex(userId)
    }

    override suspend fun getUserPresetColor(userId: String): Long? {
        return localDataSource.getUserPresetColor(userId)
    }
}
