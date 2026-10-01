package com.nura.messaging.features.auth.presentation.viewmodel

import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.entities.auth.UsernameAvailability
import com.nura.messaging.domain.repositories.auth.AuthRepository
import com.nura.messaging.domain.usecases.auth.CheckUsernameAvailabilityUseCase
import com.nura.messaging.domain.usecases.auth.GetCurrentUserUseCase
import com.nura.messaging.domain.usecases.auth.ObserveAuthStateUseCase
import com.nura.messaging.domain.usecases.auth.ResendEmailVerificationUseCase
import com.nura.messaging.domain.usecases.auth.SendEmailOtpUseCase
import com.nura.messaging.domain.usecases.auth.SendPasswordResetUseCase
import com.nura.messaging.domain.usecases.auth.SignInWithEmailUseCase
import com.nura.messaging.domain.usecases.auth.SignInWithGoogleUseCase
import com.nura.messaging.domain.usecases.auth.SignOutUseCase
import com.nura.messaging.domain.usecases.auth.SignUpWithEmailUseCase
import com.nura.messaging.domain.usecases.auth.VerifyEmailOtpUseCase
import com.nura.messaging.features.auth.data.OnboardingPreferences
import com.nura.messaging.features.auth.presentation.state.AuthUiEvent
import com.nura.messaging.features.auth.presentation.state.PasswordStrengthLevel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AuthViewModelTest {

    private lateinit var testDispatcher: TestDispatcher
    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var onboardingPreferences: OnboardingPreferences
    private lateinit var dispatchers: DispatcherProvider
    private lateinit var viewModel: AuthViewModel
    private lateinit var fakeContactsRepository: FakeContactsRepository

    @Before
    fun setup() {
        testDispatcher = StandardTestDispatcher()
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeAuthRepository()
        onboardingPreferences = OnboardingPreferences(null)
        dispatchers = object : DispatcherProvider {
            override val main: CoroutineDispatcher = testDispatcher
            override val io: CoroutineDispatcher = testDispatcher
            override val default: CoroutineDispatcher = testDispatcher
            override val unconfined: CoroutineDispatcher = testDispatcher
        }

        fakeContactsRepository = FakeContactsRepository()
        viewModel = createViewModel()
    }

    private fun createViewModel(): AuthViewModel {
        return AuthViewModel(
            signInWithEmailUseCase = SignInWithEmailUseCase(fakeRepository),
            signUpWithEmailUseCase = SignUpWithEmailUseCase(fakeRepository),
            checkUsernameAvailabilityUseCase = CheckUsernameAvailabilityUseCase(fakeRepository),
            signInWithGoogleUseCase = SignInWithGoogleUseCase(fakeRepository),
            signOutUseCase = SignOutUseCase(fakeRepository),
            sendPasswordResetUseCase = SendPasswordResetUseCase(fakeRepository),
            resendEmailVerificationUseCase = ResendEmailVerificationUseCase(fakeRepository),
            sendEmailOtpUseCase = SendEmailOtpUseCase(fakeRepository),
            verifyEmailOtpUseCase = VerifyEmailOtpUseCase(fakeRepository),
            observeAuthStateUseCase = ObserveAuthStateUseCase(fakeRepository),
            getCurrentUserUseCase = GetCurrentUserUseCase(fakeRepository),
            onboardingPreferences = onboardingPreferences,
            dispatchers = dispatchers,
            saveUserProfilePictureUseCase = com.nura.messaging.domain.usecases.contacts.SaveUserProfilePictureUseCase(fakeContactsRepository),
            getLocalProfilePictureUseCase = com.nura.messaging.domain.usecases.contacts.GetLocalProfilePictureUseCase(fakeContactsRepository),
            saveUserPresetAvatarUseCase = com.nura.messaging.domain.usecases.contacts.SaveUserPresetAvatarUseCase(fakeContactsRepository),
            saveUserPresetColorUseCase = com.nura.messaging.domain.usecases.contacts.SaveUserPresetColorUseCase(fakeContactsRepository),
            getUserPresetIndexUseCase = com.nura.messaging.domain.usecases.contacts.GetUserPresetIndexUseCase(fakeContactsRepository),
            getUserPresetColorUseCase = com.nura.messaging.domain.usecases.contacts.GetUserPresetColorUseCase(fakeContactsRepository),
            updateProfileUseCase = com.nura.messaging.domain.usecases.auth.UpdateProfileUseCase(fakeRepository)
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state starts with empty values and checking session`() = runTest {
        val state = viewModel.uiState.value
        assertEquals("", state.name)
        assertEquals("", state.email)
        assertEquals("", state.password)
        assertFalse(state.isPasswordVisible)
        assertFalse(state.isLoading)
        assertFalse(state.isGoogleLoading)
    }

    @Test
    fun `name input validation checks minimum length and formats`() {
        viewModel.onNameChanged("A")
        // trigger validation via error check
        viewModel.signUpWithEmail()
        assertNotNull(viewModel.uiState.value.nameError)

        viewModel.onNameChanged("Valid Name")
        assertNull(viewModel.uiState.value.nameError)
    }

    @Test
    fun `email validation rejects invalid email and spaces`() {
        viewModel.onEmailChanged("invalid-email")
        viewModel.signInWithEmail()
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.onEmailChanged("user with spaces@domain.com")
        viewModel.signInWithEmail()
        assertNotNull(viewModel.uiState.value.emailError)

        viewModel.onEmailChanged("valid.user@nura.chat")
        assertNull(viewModel.uiState.value.emailError)
    }

    @Test
    fun `password strength calculates empty, weak, good, and strong accurately`() {
        viewModel.onPasswordChanged("")
        assertEquals(PasswordStrengthLevel.EMPTY, viewModel.uiState.value.passwordStrength)

        viewModel.onPasswordChanged("short")
        assertEquals(PasswordStrengthLevel.WEAK, viewModel.uiState.value.passwordStrength)

        viewModel.onPasswordChanged("password123")
        assertEquals(PasswordStrengthLevel.GOOD, viewModel.uiState.value.passwordStrength)

        viewModel.onPasswordChanged("ExtremelyStrong#2026!Pass")
        assertEquals(PasswordStrengthLevel.STRONG, viewModel.uiState.value.passwordStrength)
    }

    @Test
    fun `togglePasswordVisibility flips password visibility state`() {
        assertFalse(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertTrue(viewModel.uiState.value.isPasswordVisible)
        viewModel.togglePasswordVisibility()
        assertFalse(viewModel.uiState.value.isPasswordVisible)
    }

    @Test
    fun `signInWithEmail with valid credentials signs in successfully`() = runTest {
        viewModel.onEmailChanged("verified@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        viewModel.signInWithEmail()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.currentUser)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `signInWithEmail with unverified account triggers email verification notice`() = runTest {
        viewModel.onEmailChanged("unverified@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        viewModel.signInWithEmail()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.showVerificationNotice)
        assertEquals("unverified@nura.chat", viewModel.uiState.value.verificationEmail)
    }

    @Test
    fun `signInWithEmail failure displays user friendly error`() = runTest {
        fakeRepository.shouldSignInSucceed = false
        viewModel.onEmailChanged("user@nura.chat")
        viewModel.onPasswordChanged("WrongPassword123!")

        viewModel.signInWithEmail()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertNotNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `signUpWithEmail with valid input creates account and triggers profile picture navigation`() = runTest(testDispatcher) {
        viewModel.onNameChanged("Alice")
        viewModel.onUsernameChanged("alice_wonderland")
        viewModel.onEmailChanged("alice@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        var emittedEvent: AuthUiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { emittedEvent = it }
        }

        viewModel.signUpWithEmail()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isNewUserRegistration)
        assertEquals("alice@nura.chat", viewModel.uiState.value.verificationEmail)
        assertEquals(AuthUiEvent.NavigateToProfilePicture, emittedEvent)
    }

    @Test
    fun `signInWithEmail navigates directly to Home and bypasses post signup onboarding`() = runTest(testDispatcher) {
        viewModel.onEmailChanged("alice@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        var emittedEvent: AuthUiEvent? = null
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.uiEvents.collect { emittedEvent = it }
        }

        viewModel.signInWithEmail()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("No error expected, got: ${state.generalError}", null, state.generalError)
        assertNotNull("Expected currentUser to be set", state.currentUser)
        assertTrue("Expected isEmailVerified to be true, got: ${state.currentUser?.isEmailVerified}", state.currentUser?.isEmailVerified == true)
        assertFalse(state.isLoading)
        assertFalse(state.isNewUserRegistration)
        assertEquals(AuthUiEvent.NavigateToHome, emittedEvent)
    }

    @Test
    fun `completePostSignUpOnboarding marks onboarding completed in preferences`() = runTest {
        viewModel.onNameChanged("Alice")
        viewModel.onUsernameChanged("alice_wonderland")
        viewModel.onEmailChanged("alice@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        viewModel.signUpWithEmail()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isNewUserRegistration)
        val userId = viewModel.uiState.value.currentUser?.id ?: ""
        assertTrue(userId.isNotBlank())
        assertFalse(onboardingPreferences.isOnboardingCompleted(userId))

        viewModel.completePostSignUpOnboarding()
        assertFalse(viewModel.uiState.value.isNewUserRegistration)
        assertTrue(onboardingPreferences.isOnboardingCompleted(userId))
    }

    @Test
    fun `signInWithGoogle cancellation does not show alarming error banner`() = runTest {
        fakeRepository.shouldGoogleSucceed = false
        viewModel.signInWithGoogle()
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isGoogleLoading)
        assertNull(viewModel.uiState.value.generalError)
    }

    @Test
    fun `forgot password dialog opens, validates email and sends reset link`() = runTest {
        viewModel.showForgotPassword(true)
        assertTrue(viewModel.uiState.value.showForgotPasswordDialog)

        viewModel.onForgotPasswordEmailChanged("reset@nura.chat")
        viewModel.sendPasswordReset()
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(viewModel.uiState.value.forgotPasswordSuccess)
        assertFalse(viewModel.uiState.value.isForgotPasswordLoading)
    }

    @Test
    fun `resendEmailVerification triggers cooldown timer`() = runTest {
        viewModel.onEmailChanged("verify@nura.chat")
        viewModel.resendEmailVerification()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.verificationResentSuccess)
        assertTrue(viewModel.uiState.value.resendCooldownSeconds > 0)
    }

    @Test
    fun `sendEmailOtp with valid email sends OTP and starts cooldown`() = runTest {
        viewModel.onEmailChanged("otp@nura.chat")
        viewModel.sendEmailOtp()
        testDispatcher.scheduler.runCurrent()

        assertTrue(viewModel.uiState.value.otpSentSuccess)
        assertTrue(viewModel.uiState.value.otpCooldownSeconds > 0)
    }

    @Test
    fun `verifyEmailOtp with 6 digits signs in and navigates to home`() = runTest {
        viewModel.onEmailChanged("otp@nura.chat")
        viewModel.onOtpCodeChanged("123456")
        viewModel.verifyEmailOtp()
        testDispatcher.scheduler.runCurrent()

        assertEquals("user_otp_123", viewModel.uiState.value.currentUser?.id)
    }

    @Test
    fun `onUsernameChanged with short handle sets INVALID state`() = runTest {
        viewModel.onUsernameChanged("ab")
        assertEquals("ab", viewModel.uiState.value.username)
        assertEquals(UsernameAvailability.INVALID, viewModel.uiState.value.usernameAvailability)
        assertNotNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `onUsernameChanged with valid handle becomes AVAILABLE after debounce`() = runTest {
        viewModel.onUsernameChanged("awesome_user")
        assertEquals(UsernameAvailability.CHECKING, viewModel.uiState.value.usernameAvailability)

        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UsernameAvailability.AVAILABLE, viewModel.uiState.value.usernameAvailability)
        assertNull(viewModel.uiState.value.usernameError)
    }

    @Test
    fun `onUsernameChanged with taken handle sets TAKEN state and error`() = runTest {
        fakeRepository.isUsernameAvailable = false
        viewModel.onUsernameChanged("existing_user")

        testDispatcher.scheduler.advanceTimeBy(600L)
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(UsernameAvailability.TAKEN, viewModel.uiState.value.usernameAvailability)
        assertEquals("@existing_user is already taken", viewModel.uiState.value.usernameError)
    }

    @Test
    fun `signUpWithEmail with missing username displays usernameError`() = runTest {
        viewModel.onNameChanged("Alice")
        viewModel.onEmailChanged("alice@nura.chat")
        viewModel.onPasswordChanged("StrongPassword123!")

        viewModel.signUpWithEmail()

        assertNotNull(viewModel.uiState.value.usernameError)
        assertFalse(viewModel.uiState.value.isLoading)
    }

    private class FakeAuthRepository : AuthRepository {
        var shouldSignInSucceed: Boolean = true
        var shouldGoogleSucceed: Boolean = true
        var isUsernameAvailable: Boolean = true

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
            return if (shouldSignInSucceed) {
                val isVerified = !email.contains("unverified")
                Result.success(AuthUser("user_123", email, "Test User", isEmailVerified = isVerified))
            } else {
                Result.failure(Exception("Invalid email or password. Please check your credentials."))
            }
        }

        override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<AuthUser> {
            return Result.success(AuthUser("user_456", email, name, isEmailVerified = false))
        }

        override suspend fun signUpWithEmail(
            name: String,
            username: String,
            email: String,
            password: String
        ): Result<AuthUser> {
            return Result.success(AuthUser("user_456", email, name, username = username, isEmailVerified = false))
        }

        override suspend fun checkUsernameAvailability(username: String): Result<Boolean> {
            return Result.success(isUsernameAvailable)
        }

        override suspend fun signInWithGoogle(): Result<AuthUser> {
            return if (shouldGoogleSucceed) {
                Result.success(AuthUser("google_1", "google@nura.chat", "Google User", isEmailVerified = true))
            } else {
                Result.failure(Exception("Google sign-in cancelled by user"))
            }
        }

        override suspend fun signOut(): Result<Unit> = Result.success(Unit)

        override suspend fun sendPasswordReset(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun resendEmailVerification(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun sendEmailOtp(email: String): Result<Unit> = Result.success(Unit)

        override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> {
            return Result.success(AuthUser("user_otp_123", email, "OTP User", isEmailVerified = true))
        }

        override suspend fun getCurrentUser(): AuthUser? = null

        override fun observeAuthState(): Flow<AuthUser?> = flowOf(null)

        override suspend fun handleDeepLink(uriString: String): Boolean = true

        override suspend fun updateProfile(name: String, about: String): Result<AuthUser> =
            Result.success(AuthUser("user_123", "user@nura.chat", name, about = about, isEmailVerified = true))
    }

    private class FakeContactsRepository : com.nura.messaging.domain.repositories.contacts.ContactsRepository {
        var savedPic: String? = null
        override suspend fun getRecentConnections(currentUserId: String): List<com.nura.messaging.domain.entities.contacts.ConnectionUser> = emptyList()
        override suspend fun connectUser(
            currentUserId: String,
            targetUsername: String,
            preferredUserId: String?
        ): Result<com.nura.messaging.domain.entities.contacts.ConnectionUser> =
            Result.failure(Exception("Stub"))
        override suspend fun saveUserProfilePicture(userId: String, sourceUriString: String): String? {
            savedPic = sourceUriString
            return sourceUriString
        }
        override suspend fun getLocalProfilePicture(userId: String): String? = savedPic
        override suspend fun saveUserPresetAvatar(userId: String, presetIndex: Int) {}
        override suspend fun saveUserPresetColor(userId: String, colorArgb: Long) {}
        override suspend fun getUserPresetIndex(userId: String): Int? = null
        override suspend fun getUserPresetColor(userId: String): Long? = null
    }
}
