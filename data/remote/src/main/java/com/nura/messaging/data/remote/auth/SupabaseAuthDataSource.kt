package com.nura.messaging.data.remote.auth

import com.nura.messaging.core.network.SupabaseConfig
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SupabaseAuthDataSource @Inject constructor(
    private val supabaseClient: SupabaseClient,
    private val okHttpClient: OkHttpClient
) {
    private val auth: Auth = supabaseClient.auth

    suspend fun checkUsernameAvailability(username: String): Boolean {
        val clean = username.trim().lowercase().removePrefix("@")
        val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/rest/v1/profiles?username=eq.$clean&select=username"
        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
            .header("Authorization", "Bearer ${SupabaseConfig.DEFAULT_ANON_KEY}")
            .get()
            .build()

        return withContext(Dispatchers.IO) {
            try {
                okHttpClient.newCall(request).execute().use { response ->
                    if (response.code == 404) {
                        return@use true
                    }
                    if (!response.isSuccessful) {
                        throw IOException("Unexpected HTTP response: ${response.code}")
                    }
                    val body = response.body?.string().orEmpty()
                    body.trim() == "[]"
                }
            } catch (e: Exception) {
                if (e is IOException && e.message?.contains("404") == true) {
                    true
                } else {
                    throw e
                }
            }
        }
    }

    suspend fun signUpWithEmail(name: String, username: String, email: String, password: String): UserInfo? {
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val userMetadata = buildJsonObject {
            put("full_name", name)
            put("display_name", name)
            put("username", cleanUsername)
        }
        val signedUpUser = auth.signUpWith(Email) {
            this.email = email
            this.password = password
            this.data = userMetadata
        }
        if (auth.currentUserOrNull() == null) {
            try {
                auth.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
            } catch (_: Exception) {
                // Keep signedUpUser if sign-in fails
            }
        }
        val user = auth.currentUserOrNull() ?: signedUpUser
        user?.let {
            try {
                upsertProfile(it.id, cleanUsername, name)
            } catch (_: Exception) {
                // Ignore if profiles table is not yet created
            }
        }
        return user
    }

    suspend fun signUpWithEmail(name: String, email: String, password: String): UserInfo? {
        return signUpWithEmail(name, name.lowercase().replace(" ", "_"), email, password)
    }

    private suspend fun upsertProfile(userId: String, username: String, fullName: String) {
        val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/rest/v1/profiles"
        val json = """{"id":"$userId","username":"$username","full_name":"$fullName"}"""
        val request = Request.Builder()
            .url(url)
            .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
            .header("Authorization", "Bearer ${auth.currentAccessTokenOrNull() ?: SupabaseConfig.DEFAULT_ANON_KEY}")
            .header("Prefer", "resolution=merge-duplicates")
            .header("Content-Type", "application/json")
            .post(json.toRequestBody("application/json".toMediaType()))
            .build()

        withContext(Dispatchers.IO) {
            okHttpClient.newCall(request).execute().close()
        }
    }

    suspend fun signInWithEmail(email: String, password: String): UserInfo {
        auth.signInWith(Email) {
            this.email = email
            this.password = password
        }
        return auth.currentUserOrNull() ?: throw IllegalStateException("User session not found after sign in")
    }

    suspend fun signInWithGoogle(): UserInfo? {
        auth.signInWith(Google)
        return auth.currentUserOrNull()
    }

    suspend fun signOut() {
        auth.signOut()
    }

    suspend fun sendPasswordReset(email: String) {
        auth.resetPasswordForEmail(email)
    }

    suspend fun resendEmailVerification(email: String) {
        auth.resendEmail(OtpType.Email.SIGNUP, email)
    }

    suspend fun sendEmailOtp(email: String) {
        auth.signInWith(OTP) {
            this.email = email
        }
    }

    suspend fun verifyEmailOtp(email: String, token: String): UserInfo {
        auth.verifyEmailOtp(
            type = OtpType.Email.EMAIL,
            email = email,
            token = token
        )
        return auth.currentUserOrNull() ?: throw IllegalStateException("User session not found after OTP verification")
    }

    fun getCurrentUser(): UserInfo? {
        return auth.currentUserOrNull()
    }

    fun observeSessionStatus(): Flow<SessionStatus> {
        return auth.sessionStatus
    }

    suspend fun handleDeepLink(uri: android.net.Uri): Boolean {
        return try {
            supabaseClient.handleDeeplinks(android.content.Intent(android.content.Intent.ACTION_VIEW, uri))
            true
        } catch (_: Exception) {
            false
        }
    }

    suspend fun uploadAvatar(userId: String, imageBytes: ByteArray): String? = withContext(Dispatchers.IO) {
        try {
            val token = auth.currentAccessTokenOrNull()
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/storage/v1/object/avatars/${userId}.jpg"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", "Bearer ${token ?: SupabaseConfig.DEFAULT_ANON_KEY}")
                .header("Content-Type", "image/jpeg")
                .header("x-upsert", "true")
                .post(imageBytes.toRequestBody("image/jpeg".toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) {
                "${SupabaseConfig.DEFAULT_SUPABASE_URL}/storage/v1/object/public/avatars/${userId}.jpg?t=${System.currentTimeMillis()}"
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadMedia(fileName: String, fileBytes: ByteArray, mimeType: String): String? = withContext(Dispatchers.IO) {
        try {
            val token = auth.currentAccessTokenOrNull()
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/storage/v1/object/avatars/${fileName}"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", "Bearer ${token ?: SupabaseConfig.DEFAULT_ANON_KEY}")
                .header("Content-Type", mimeType)
                .header("x-upsert", "true")
                .post(fileBytes.toRequestBody(mimeType.toMediaType()))
                .build()

            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            response.close()

            if (success) {
                "${SupabaseConfig.DEFAULT_SUPABASE_URL}/storage/v1/object/public/avatars/${fileName}"
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }


    suspend fun deleteAvatarFromStorage(userId: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val token = auth.currentAccessTokenOrNull()
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/storage/v1/object/avatars/${userId}.jpg"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", "Bearer ${token ?: SupabaseConfig.DEFAULT_ANON_KEY}")
                .delete()
                .build()
            val response = okHttpClient.newCall(request).execute()
            val success = response.isSuccessful
            response.close()
            success
        } catch (_: Exception) {
            false
        }
    }

    suspend fun updateProfile(
        userId: String,
        name: String,
        about: String,
        avatarUrl: String? = null
    ): UserInfo? {
        val token = auth.currentAccessTokenOrNull()

        // If user explicitly changed to a preset or removed custom avatar, delete old image from storage
        if (avatarUrl == "" || avatarUrl?.startsWith("preset:") == true || avatarUrl?.startsWith("color:") == true) {
            deleteAvatarFromStorage(userId)
        }

        // 1. Update Supabase Auth user metadata
        if (token != null) {
            try {
                val authUrl = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/auth/v1/user"
                val authJson = buildJsonObject {
                    put("data", buildJsonObject {
                        put("full_name", name)
                        put("display_name", name)
                        put("about", about)
                        if (!avatarUrl.isNullOrBlank()) {
                            put("avatar_url", avatarUrl)
                        } else if (avatarUrl == "") {
                            put("avatar_url", "")
                        }
                    })
                }.toString()

                val authRequest = Request.Builder()
                    .url(authUrl)
                    .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                    .header("Authorization", "Bearer $token")
                    .header("Content-Type", "application/json")
                    .put(authJson.toRequestBody("application/json".toMediaType()))
                    .build()

                withContext(Dispatchers.IO) {
                    okHttpClient.newCall(authRequest).execute().close()
                }
            } catch (_: Exception) {}
        }

        // 2. Idempotent Upsert into Supabase PostgREST public.profiles table
        try {
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/rest/v1/profiles"
            val json = buildJsonObject {
                put("id", userId)
                put("display_name", name)
                put("about", about)
                if (!avatarUrl.isNullOrBlank()) {
                    put("avatar_url", avatarUrl)
                } else if (avatarUrl == "") {
                    put("avatar_url", "")
                }
            }.toString()

            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", "Bearer ${token ?: SupabaseConfig.DEFAULT_ANON_KEY}")
                .header("Prefer", "resolution=merge-duplicates")
                .header("Content-Type", "application/json")
                .post(json.toRequestBody("application/json".toMediaType()))
                .build()

            withContext(Dispatchers.IO) {
                okHttpClient.newCall(request).execute().close()
            }
        } catch (_: Exception) {}

        return auth.currentUserOrNull()
    }

    suspend fun fetchRemoteProfile(userId: String): com.nura.messaging.domain.entities.auth.RemoteUserProfile? = withContext(Dispatchers.IO) {
        val token = auth.currentAccessTokenOrNull()
        try {
            val url = "${SupabaseConfig.DEFAULT_SUPABASE_URL}/rest/v1/profiles?id=eq.$userId&select=id,username,display_name,about,avatar_url"
            val request = Request.Builder()
                .url(url)
                .header("apikey", SupabaseConfig.DEFAULT_ANON_KEY)
                .header("Authorization", "Bearer ${token ?: SupabaseConfig.DEFAULT_ANON_KEY}")
                .get()
                .build()

            okHttpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    if (body.isNotBlank() && body.trim() != "[]") {
                        val array = kotlinx.serialization.json.Json.parseToJsonElement(body) as? kotlinx.serialization.json.JsonArray
                        if (array != null && array.isNotEmpty()) {
                            val obj = array[0] as? kotlinx.serialization.json.JsonObject
                            if (obj != null) {
                                val id = (obj["id"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: userId
                                val username = (obj["username"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
                                val displayName = (obj["display_name"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: username
                                val about = (obj["about"] as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "HI there i'm using nura"
                                val avatarUrl = (obj["avatar_url"] as? kotlinx.serialization.json.JsonPrimitive)?.content
                                return@withContext com.nura.messaging.domain.entities.auth.RemoteUserProfile(
                                    id = id,
                                    username = username,
                                    displayName = displayName,
                                    about = about,
                                    avatarUrl = avatarUrl
                                )
                            }
                        }
                    }
                }
            }
        } catch (_: Exception) {}
        null
    }
}
