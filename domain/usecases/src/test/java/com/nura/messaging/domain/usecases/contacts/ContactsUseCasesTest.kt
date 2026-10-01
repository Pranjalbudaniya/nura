package com.nura.messaging.domain.usecases.contacts

import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.domain.repositories.contacts.ContactsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ContactsUseCasesTest {

    private lateinit var fakeRepository: FakeContactsRepository
    private lateinit var getRecentConnectionsUseCase: GetRecentConnectionsUseCase
    private lateinit var connectUserUseCase: ConnectUserUseCase
    private lateinit var saveUserProfilePictureUseCase: SaveUserProfilePictureUseCase
    private lateinit var getLocalProfilePictureUseCase: GetLocalProfilePictureUseCase

    @Before
    fun setUp() {
        fakeRepository = FakeContactsRepository()
        getRecentConnectionsUseCase = GetRecentConnectionsUseCase(fakeRepository)
        connectUserUseCase = ConnectUserUseCase(fakeRepository)
        saveUserProfilePictureUseCase = SaveUserProfilePictureUseCase(fakeRepository)
        getLocalProfilePictureUseCase = GetLocalProfilePictureUseCase(fakeRepository)
    }

    @Test
    fun getRecentConnections_returnsEmpty_whenUserIdIsBlank() = runTest {
        val result = getRecentConnectionsUseCase("")
        assertTrue(result.isEmpty())
    }

    @Test
    fun getRecentConnections_returnsList_whenUserHasConnections() = runTest {
        fakeRepository.connections["user1"] = listOf(
            ConnectionUser(id = "c1", username = "maya", displayName = "Maya Lin")
        )
        val result = getRecentConnectionsUseCase("user1")
        assertEquals(1, result.size)
        assertEquals("maya", result.first().username)
    }

    @Test
    fun connectUser_fails_whenUsernameIsBlank() = runTest {
        val result = connectUserUseCase("user1", "   ")
        assertTrue(result.isFailure)
    }

    @Test
    fun connectUser_stripsAtPrefixAndSucceeds() = runTest {
        val result = connectUserUseCase("user1", "@soren")
        assertTrue(result.isSuccess)
        assertEquals("soren", result.getOrNull()?.username)
    }

    @Test
    fun saveUserProfilePicture_delegatesToRepository() = runTest {
        val result = saveUserProfilePictureUseCase("user1", "content://media/photo.jpg")
        assertEquals("file:///app/user_avatars/user1.jpg", result)
    }

    @Test
    fun saveUserPresetAvatar_delegatesToRepository() = runTest {
        val useCase = SaveUserPresetAvatarUseCase(fakeRepository)
        val getUseCase = GetUserPresetIndexUseCase(fakeRepository)
        useCase("user1", 3)
        assertEquals(3, getUseCase("user1"))
    }

    @Test
    fun saveUserPresetColor_delegatesToRepository() = runTest {
        val useCase = SaveUserPresetColorUseCase(fakeRepository)
        val getUseCase = GetUserPresetColorUseCase(fakeRepository)
        useCase("user1", 0xFFE07A5FL)
        assertEquals(0xFFE07A5FL, getUseCase("user1"))
    }

    private class FakeContactsRepository : ContactsRepository {
        val connections = mutableMapOf<String, List<ConnectionUser>>()
        var savedPicturePath: String? = null
        val presetIndices = mutableMapOf<String, Int>()
        val presetColors = mutableMapOf<String, Long>()

        override suspend fun getRecentConnections(currentUserId: String): List<ConnectionUser> {
            return connections[currentUserId] ?: emptyList()
        }

        override suspend fun connectUser(
            currentUserId: String,
            targetUsername: String,
            preferredUserId: String?
        ): Result<ConnectionUser> {
            val user = ConnectionUser(
                id = preferredUserId ?: "id_$targetUsername",
                username = targetUsername,
                displayName = targetUsername.replaceFirstChar { it.uppercase() }
            )
            val current = connections[currentUserId]?.toMutableList() ?: mutableListOf()
            current.add(0, user)
            connections[currentUserId] = current
            return Result.success(user)
        }

        override suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String? {
            savedPicturePath = "file:///app/user_avatars/$userId.jpg"
            return savedPicturePath
        }

        override suspend fun getLocalProfilePicture(userId: String): String? {
            return savedPicturePath
        }

        override suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int) {
            presetIndices[userId] = presetIndex
        }

        override suspend fun saveUserPresetColor(userId: String, colorArgb: Long) {
            presetColors[userId] = colorArgb
        }

        override suspend fun getUserPresetIndex(userId: String): Int? {
            return presetIndices[userId]
        }

        override suspend fun getUserPresetColor(userId: String): Long? {
            return presetColors[userId]
        }
    }
}
