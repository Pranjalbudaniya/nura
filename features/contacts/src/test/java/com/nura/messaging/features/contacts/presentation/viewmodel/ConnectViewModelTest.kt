package com.nura.messaging.features.contacts.presentation.viewmodel

import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.domain.repositories.contacts.ContactsRepository
import com.nura.messaging.domain.usecases.contacts.ConnectUserUseCase
import com.nura.messaging.domain.usecases.contacts.GetRecentConnectionsUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConnectViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeRepository: FakeContactsRepository
    private lateinit var viewModel: ConnectViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeContactsRepository()
        val getRecentConnectionsUseCase = GetRecentConnectionsUseCase(fakeRepository)
        val connectUserUseCase = ConnectUserUseCase(fakeRepository)
        viewModel = ConnectViewModel(getRecentConnectionsUseCase, connectUserUseCase)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialize_loadsExistingConnections() = runTest {
        fakeRepository.connections["user1"] = mutableListOf(
            ConnectionUser(id = "c1", username = "maya", displayName = "Maya Lin")
        )

        viewModel.initialize("user1", "alex", "Alex Rivera")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("user1", state.currentUserId)
        assertEquals("@alex", state.currentUserHandle)
        assertEquals(1, state.recentConnections.size)
        assertEquals("maya", state.recentConnections.first().username)
    }

    @Test
    fun onConnectClicked_showsToast_whenUsernameIsBlank() = runTest {
        viewModel.initialize("user1", "alex", "Alex Rivera")
        viewModel.onUsernameChanged("")
        viewModel.onConnectClicked()

        val state = viewModel.uiState.value
        assertTrue(state.showToast)
        assertEquals("Please enter a valid Nura username", state.toastMessage)
    }

    @Test
    fun onConnectClicked_connectsAndUpdatesRecentConnections() = runTest {
        viewModel.initialize("user1", "alex", "Alex Rivera")
        viewModel.onUsernameChanged("@soren")
        viewModel.onConnectClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isConnecting)
        assertEquals("", state.usernameInput)
        assertTrue(state.showToast)
        assertTrue(state.toastMessage?.contains("soren") == true)
        assertEquals(1, state.recentConnections.size)
        assertEquals("soren", state.recentConnections.first().username)
    }

    @Test
    fun onConnectClicked_fails_whenUserNotFound() = runTest {
        viewModel.initialize("user1", "alex", "Alex Rivera")
        fakeRepository.failNext = true
        viewModel.onUsernameChanged("@ghost")
        viewModel.onConnectClicked()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isConnecting)
        assertTrue(state.showToast)
        assertEquals("User @ghost was not found or is unavailable.", state.toastMessage)
        assertEquals(0, state.recentConnections.size)
    }

    @Test
    fun onQrScanned_extractsUsernameAndUserId_andConnects() = runTest {
        viewModel.initialize("user1", "alex", "Alex Rivera")
        viewModel.onQrScanned("nura://user/sarah?id=sarah-uuid-123&key=NU-1234-5678")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showScanner)
        assertEquals(1, state.recentConnections.size)
        assertEquals("sarah", state.recentConnections.first().username)
        assertEquals("sarah-uuid-123", state.recentConnections.first().id)
    }

    private class FakeContactsRepository : ContactsRepository {
        val connections = mutableMapOf<String, MutableList<ConnectionUser>>()
        var failNext = false

        override suspend fun getRecentConnections(currentUserId: String): List<ConnectionUser> {
            return connections[currentUserId] ?: emptyList()
        }

        override suspend fun connectUser(
            currentUserId: String,
            targetUsername: String,
            preferredUserId: String?
        ): Result<ConnectionUser> {
            if (failNext) {
                return Result.failure(NoSuchElementException("User @$targetUsername was not found or is unavailable."))
            }
            val user = ConnectionUser(
                id = preferredUserId ?: "id_$targetUsername",
                username = targetUsername,
                displayName = targetUsername.replaceFirstChar { it.uppercase() }
            )
            val list = connections.getOrPut(currentUserId) { mutableListOf() }
            list.add(0, user)
            return Result.success(user)
        }

        override suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String? = null
        override suspend fun getLocalProfilePicture(userId: String): String? = null
        override suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int) {}
        override suspend fun saveUserPresetColor(userId: String, colorArgb: Long) {}
        override suspend fun getUserPresetIndex(userId: String): Int? = null
        override suspend fun getUserPresetColor(userId: String): Long? = null
    }
}
