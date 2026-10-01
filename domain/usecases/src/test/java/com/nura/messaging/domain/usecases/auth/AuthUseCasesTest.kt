package com.nura.messaging.domain.usecases.auth

import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.repositories.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AuthUseCasesTest {

    private lateinit var fakeRepository: FakeAuthRepository
    private lateinit var signInWithEmailUseCase: SignInWithEmailUseCase
    private lateinit var signUpWithEmailUseCase: SignUpWithEmailUseCase
    private lateinit var signInWithGoogleUseCase: SignInWithGoogleUseCase
    private lateinit var sendPasswordResetUseCase: SendPasswordResetUseCase
    private lateinit var resendEmailVerificationUseCase: ResendEmailVerificationUseCase
    private lateinit var handleDeepLinkUseCase: HandleDeepLinkUseCase

    @Before
    fun setup() {
        fakeRepository = FakeAuthRepository()
        signInWithEmailUseCase = SignInWithEmailUseCase(fakeRepository)
        signUpWithEmailUseCase = SignUpWithEmailUseCase(fakeRepository)
        signInWithGoogleUseCase = SignInWithGoogleUseCase(fakeRepository)
        sendPasswordResetUseCase = SendPasswordResetUseCase(fakeRepository)
        resendEmailVerificationUseCase = ResendEmailVerificationUseCase(fakeRepository)
        handleDeepLinkUseCase = HandleDeepLinkUseCase(fakeRepository)
    }

    @Test
    fun `signInWithEmail trims accidental email whitespace`() = runTest {
        val result = signInWithEmailUseCase("  user@nura.chat  ", "SecurePass123!")

        assertTrue(result.isSuccess)
        assertEquals("user@nura.chat", fakeRepository.lastPassedEmail)
        assertEquals("SecurePass123!", fakeRepository.lastPassedPassword)
    }

    @Test
    fun `signUpWithEmail preserves password whitespace and trims name and email`() = runTest {
        val rawPassword = "  P@sswordWithIntentionalSpaces  "
        val result = signUpWithEmailUseCase("  Jane Doe  ", "  jane@nura.chat  ", rawPassword)

        assertTrue(result.isSuccess)
        assertEquals("Jane Doe", fakeRepository.lastPassedName)
        assertEquals("jane@nura.chat", fakeRepository.lastPassedEmail)
        assertEquals(rawPassword, fakeRepository.lastPassedPassword)
    }

    @Test
    fun `signInWithGoogle returns success on valid authentication`() = runTest {
        fakeRepository.shouldGoogleSucceed = true
        val result = signInWithGoogleUseCase()

        assertTrue(result.isSuccess)
        assertEquals("google_user_1", result.getOrNull()?.id)
    }

    @Test
    fun `signInWithGoogle returns failure on cancellation`() = runTest {
        fakeRepository.shouldGoogleSucceed = false
        val result = signInWithGoogleUseCase()

        assertTrue(result.isFailure)
        assertEquals("Google sign-in cancelled or interrupted", result.exceptionOrNull()?.message)
    }

    @Test
    fun `sendPasswordReset trims email address`() = runTest {
        val result = sendPasswordResetUseCase("  reset@nura.chat  ")

        assertTrue(result.isSuccess)
        assertEquals("reset@nura.chat", fakeRepository.lastPassedEmail)
    }

    @Test
    fun `resendEmailVerification trims email address`() = runTest {
        val result = resendEmailVerificationUseCase("  verify@nura.chat  ")

        assertTrue(result.isSuccess)
        assertEquals("verify@nura.chat", fakeRepository.lastPassedEmail)
    }

    @Test
    fun `handleDeepLink returns true for valid auth callback uri`() = runTest {
        fakeRepository.deepLinkResult = true
        val handled = handleDeepLinkUseCase("nura://auth-callback#access_token=xyz")

        assertTrue(handled)
    }

    @Test
    fun `handleDeepLink returns false for invalid callback`() = runTest {
        fakeRepository.deepLinkResult = false
        val handled = handleDeepLinkUseCase("nura://invalid-route")

        assertFalse(handled)
    }

    @Test
    fun `sendEmailOtp trims whitespace and delegates to repository`() = runTest {
        val sendEmailOtpUseCase = SendEmailOtpUseCase(fakeRepository)
        val result = sendEmailOtpUseCase("  user@nura.chat  ")

        assertTrue(result.isSuccess)
        assertEquals("user@nura.chat", fakeRepository.lastPassedEmail)
    }

    @Test
    fun `verifyEmailOtp trims whitespace and delegates to repository`() = runTest {
        val verifyEmailOtpUseCase = VerifyEmailOtpUseCase(fakeRepository)
        val result = verifyEmailOtpUseCase("  user@nura.chat  ", "  123456  ")

        assertTrue(result.isSuccess)
        assertEquals("user@nura.chat", fakeRepository.lastPassedEmail)
        assertEquals("123456", fakeRepository.lastPassedToken)
    }

    private class FakeAuthRepository : AuthRepository {
        var lastPassedName: String? = null
        var lastPassedEmail: String? = null
        var lastPassedPassword: String? = null
        var lastPassedToken: String? = null
        var shouldGoogleSucceed: Boolean = true
        var deepLinkResult: Boolean = true

        override suspend fun signInWithEmail(email: String, password: String): Result<AuthUser> {
            lastPassedEmail = email
            lastPassedPassword = password
            return Result.success(AuthUser("id_1", email, "User"))
        }

        override suspend fun checkUsernameAvailability(username: String): Result<Boolean> = Result.success(true)

        override suspend fun signUpWithEmail(name: String, username: String, email: String, password: String): Result<AuthUser> {
            lastPassedName = name
            lastPassedEmail = email
            lastPassedPassword = password
            return Result.success(AuthUser("id_2", email, name, username, isEmailVerified = false))
        }

        override suspend fun signUpWithEmail(name: String, email: String, password: String): Result<AuthUser> {
            return signUpWithEmail(name, name.lowercase().replace(" ", "_"), email, password)
        }

        override suspend fun signInWithGoogle(): Result<AuthUser> {
            return if (shouldGoogleSucceed) {
                Result.success(AuthUser("google_user_1", "google@nura.chat", "Google User"))
            } else {
                Result.failure(Exception("Google sign-in cancelled or interrupted"))
            }
        }

        override suspend fun signOut(): Result<Unit> = Result.success(Unit)

        override suspend fun sendPasswordReset(email: String): Result<Unit> {
            lastPassedEmail = email
            return Result.success(Unit)
        }

        override suspend fun resendEmailVerification(email: String): Result<Unit> {
            lastPassedEmail = email
            return Result.success(Unit)
        }

        override suspend fun sendEmailOtp(email: String): Result<Unit> {
            lastPassedEmail = email
            return Result.success(Unit)
        }

        override suspend fun verifyEmailOtp(email: String, token: String): Result<AuthUser> {
            lastPassedEmail = email
            lastPassedToken = token
            return Result.success(AuthUser("otp_user_1", email, "OTP User"))
        }

        override suspend fun getCurrentUser(): AuthUser? = null

        override fun observeAuthState(): Flow<AuthUser?> = flowOf(null)

        override suspend fun handleDeepLink(uriString: String): Boolean = deepLinkResult
    }
}
