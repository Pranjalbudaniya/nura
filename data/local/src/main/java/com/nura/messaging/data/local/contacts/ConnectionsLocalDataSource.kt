package com.nura.messaging.data.local.contacts

import android.content.Context
import android.net.Uri
import com.nura.messaging.domain.entities.contacts.ConnectionUser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class ConnectionsLocalDataSource @Inject constructor(
    @ApplicationContext private val context: Context?
) {
    private val inMemoryConnections = mutableMapOf<String, MutableList<ConnectionUser>>()
    private val inMemoryAvatars = mutableMapOf<String, String>()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private fun getPrefs(userId: String) =
        context?.getSharedPreferences(PREFS_PREFIX + userId, Context.MODE_PRIVATE)

    open suspend fun getRecentConnections(userId: String): List<ConnectionUser> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext emptyList()
        val prefs = getPrefs(userId)
        val jsonStr = prefs?.getString(KEY_CONNECTIONS, null)
        if (jsonStr != null) {
            try {
                json.decodeFromString<List<ConnectionUser>>(jsonStr)
            } catch (_: Exception) {
                inMemoryConnections[userId] ?: emptyList()
            }
        } else {
            inMemoryConnections[userId] ?: emptyList()
        }
    }

    open suspend fun saveConnection(userId: String, connection: ConnectionUser): ConnectionUser = withContext(Dispatchers.IO) {
        val currentList = getRecentConnections(userId).toMutableList()
        // Remove existing by id or username to prevent duplicates
        currentList.removeAll { it.id == connection.id || it.username.equals(connection.username, ignoreCase = true) }
        currentList.add(0, connection)

        val prefs = getPrefs(userId)
        if (prefs != null) {
            val encoded = json.encodeToString(currentList)
            prefs.edit().putString(KEY_CONNECTIONS, encoded).apply()
        }
        inMemoryConnections[userId] = currentList
        connection
    }

    open suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank() || sourceUriString.isBlank() || context == null) {
            inMemoryAvatars[userId] = sourceUriString
            return@withContext sourceUriString
        }

        try {
            val avatarsDir = File(context.filesDir, "user_avatars").apply { mkdirs() }
            val destFile = File(avatarsDir, "${userId}_avatar.jpg")

            val uri = Uri.parse(sourceUriString)
            val inputStream = when {
                uri.scheme == "file" -> {
                    val path = uri.path ?: sourceUriString.removePrefix("file://")
                    File(path).inputStream()
                }
                uri.scheme == "content" -> {
                    context.contentResolver.openInputStream(uri)
                }
                else -> {
                    val f = File(sourceUriString)
                    if (f.exists()) f.inputStream() else context.contentResolver.openInputStream(uri)
                }
            }

            inputStream?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            }

            val savedPath = "file://${destFile.absolutePath}"
            getPrefs(userId)?.edit()
                ?.putString(KEY_USER_AVATAR, savedPath)
                ?.remove(KEY_USER_PRESET_INDEX)
                ?.remove(KEY_USER_PRESET_COLOR)
                ?.apply()
            inMemoryAvatars[userId] = savedPath
            savedPath
        } catch (_: Exception) {
            inMemoryAvatars[userId] = sourceUriString
            sourceUriString
        }
    }

    open suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        getPrefs(userId)?.edit()
            ?.putInt(KEY_USER_PRESET_INDEX, presetIndex)
            ?.remove(KEY_USER_AVATAR)
            ?.remove(KEY_USER_PRESET_COLOR)
            ?.apply()
        inMemoryAvatars.remove(userId)
    }

    open suspend fun saveUserPresetColor(userId: String, colorArgb: Long) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext
        getPrefs(userId)?.edit()
            ?.putLong(KEY_USER_PRESET_COLOR, colorArgb)
            ?.remove(KEY_USER_AVATAR)
            ?.remove(KEY_USER_PRESET_INDEX)
            ?.apply()
        inMemoryAvatars.remove(userId)
    }

    open suspend fun getUserPresetIndex(userId: String): Int? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        val prefs = getPrefs(userId) ?: return@withContext null
        if (prefs.contains(KEY_USER_PRESET_INDEX)) {
            prefs.getInt(KEY_USER_PRESET_INDEX, -1).takeIf { it >= 0 }
        } else null
    }

    open suspend fun getUserPresetColor(userId: String): Long? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        val prefs = getPrefs(userId) ?: return@withContext null
        if (prefs.contains(KEY_USER_PRESET_COLOR)) {
            prefs.getLong(KEY_USER_PRESET_COLOR, -1L).takeIf { it != -1L }
        } else null
    }

    open suspend fun getLocalProfilePicture(userId: String): String? = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext null
        val stored = getPrefs(userId)?.getString(KEY_USER_AVATAR, null) ?: inMemoryAvatars[userId]
        if (stored != null) {
            if (stored.startsWith("file://")) {
                val filePath = stored.removePrefix("file://")
                if (File(filePath).exists()) return@withContext stored
            } else if (stored.startsWith("/") && File(stored).exists()) {
                return@withContext "file://$stored"
            } else {
                return@withContext stored
            }
        }
        null
    }

    open suspend fun saveConnectedUserAvatar(
        userId: String,
        connectedUserId: String,
        imageBytes: ByteArray
    ): String? = withContext(Dispatchers.IO) {
        if (context == null || userId.isBlank() || connectedUserId.isBlank()) return@withContext null
        try {
            val connDir = File(File(context.filesDir, "connections"), userId).apply { mkdirs() }
            val destFile = File(connDir, "${connectedUserId}_avatar.jpg")
            FileOutputStream(destFile).use { output ->
                output.write(imageBytes)
            }
            "file://${destFile.absolutePath}"
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private const val PREFS_PREFIX = "nura_connections_store_"
        private const val KEY_CONNECTIONS = "key_connections_list"
        private const val KEY_USER_AVATAR = "key_local_user_avatar"
        private const val KEY_USER_PRESET_INDEX = "key_local_user_preset_index"
        private const val KEY_USER_PRESET_COLOR = "key_local_user_preset_color"
    }
}
