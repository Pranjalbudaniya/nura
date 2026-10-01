package com.nura.messaging.domain.usecases.contacts

import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.domain.repositories.contacts.ContactsRepository
import javax.inject.Inject

class GetRecentConnectionsUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(currentUserId: String): List<ConnectionUser> {
        if (currentUserId.isBlank()) return emptyList()
        return repository.getRecentConnections(currentUserId)
    }
}

class ConnectUserUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(
        currentUserId: String,
        targetUsername: String,
        preferredUserId: String? = null
    ): Result<ConnectionUser> {
        val clean = targetUsername.trim().removePrefix("@")
        if (clean.isBlank()) {
            return Result.failure(IllegalArgumentException("Username cannot be empty"))
        }
        return repository.connectUser(currentUserId, clean, preferredUserId)
    }
}

class SaveUserProfilePictureUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String, sourceUriString: String): String? {
        if (userId.isBlank() || sourceUriString.isBlank()) return null
        return repository.saveUserProfilePicture(userId, sourceUriString)
    }
}

class GetLocalProfilePictureUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String): String? {
        if (userId.isBlank()) return null
        return repository.getLocalProfilePicture(userId)
    }
}

class SaveUserPresetAvatarUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String, presetIndex: Int) {
        if (userId.isNotBlank() && presetIndex >= 0) {
            repository.saveUserPresetAvatar(userId, presetIndex)
        }
    }
}

class SaveUserPresetColorUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String, colorArgb: Long) {
        if (userId.isNotBlank()) {
            repository.saveUserPresetColor(userId, colorArgb)
        }
    }
}

class GetUserPresetIndexUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String): Int? {
        if (userId.isBlank()) return null
        return repository.getUserPresetIndex(userId)
    }
}

class GetUserPresetColorUseCase @Inject constructor(
    private val repository: ContactsRepository
) {
    suspend operator fun invoke(userId: String): Long? {
        if (userId.isBlank()) return null
        return repository.getUserPresetColor(userId)
    }
}
