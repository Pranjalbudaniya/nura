package com.nura.messaging.domain.repositories.contacts

import com.nura.messaging.domain.entities.contacts.ConnectionUser

interface ContactsRepository {
    suspend fun getRecentConnections(currentUserId: String): List<ConnectionUser>
    suspend fun connectUser(
        currentUserId: String,
        targetUsername: String,
        preferredUserId: String? = null
    ): Result<ConnectionUser>
    suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String?
    suspend fun getLocalProfilePicture(userId: String): String?
    suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int)
    suspend fun saveUserPresetColor(userId: String, colorArgb: Long)
    suspend fun getUserPresetIndex(userId: String): Int?
    suspend fun getUserPresetColor(userId: String): Long?
}
