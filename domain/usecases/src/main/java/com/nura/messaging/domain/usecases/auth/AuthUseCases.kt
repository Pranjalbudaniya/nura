package com.nura.messaging.domain.usecases.auth

import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.repositories.auth.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<AuthUser> {
        val trimmedEmail = email.trim()
        return repository.signInWithEmail(trimmedEmail, password)
    }
}

class CheckUsernameAvailabilityUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(username: String): Result<Boolean> {
        val clean = username.trim().lowercase().removePrefix("@")
        return repository.checkUsernameAvailability(clean)
    }
}

class SignUpWithEmailUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, username: String, email: String, password: String): Result<AuthUser> {
        val trimmedName = name.trim()
        val cleanUsername = username.trim().lowercase().removePrefix("@")
        val trimmedEmail = email.trim()
        return repository.signUpWithEmail(trimmedName, cleanUsername, trimmedEmail, password)
    }

    suspend operator fun invoke(name: String, email: String, password: String): Result<AuthUser> {
        return invoke(name, name.lowercase().replace(" ", "_"), email, password)
    }
}

class SignInWithGoogleUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<AuthUser> {
        return repository.signInWithGoogle()
    }
}

class SignOutUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.signOut()
    }
}

class SendPasswordResetUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return repository.sendPasswordReset(email.trim())
    }
}

class ResendEmailVerificationUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return repository.resendEmailVerification(email.trim())
    }
}

class SendEmailOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String): Result<Unit> {
        return repository.sendEmailOtp(email.trim())
    }
}

class VerifyEmailOtpUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(email: String, token: String): Result<AuthUser> {
        return repository.verifyEmailOtp(email.trim(), token.trim())
    }
}

class ObserveAuthStateUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    operator fun invoke(): Flow<AuthUser?> {
        return repository.observeAuthState()
    }
}

class HandleDeepLinkUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(uriString: String): Boolean {
        return repository.handleDeepLink(uriString)
    }
}

class GetCurrentUserUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(): AuthUser? {
        return repository.getCurrentUser()
    }
}

class UpdateProfileUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(name: String, about: String, avatarUrl: String? = null): Result<AuthUser> {
        val trimmedName = name.trim()
        val trimmedAbout = about.trim().ifEmpty { "HI there i'm using nura" }
        return repository.updateProfile(trimmedName, trimmedAbout, avatarUrl)
    }
}

class GetRemoteUserProfileUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String): Result<com.nura.messaging.domain.entities.auth.RemoteUserProfile?> {
        return repository.getRemoteUserProfile(userId)
    }
}

class UploadAvatarUseCase @Inject constructor(
    private val repository: AuthRepository
) {
    suspend operator fun invoke(userId: String, imageBytes: ByteArray): Result<String> {
        return repository.uploadAvatar(userId, imageBytes)
    }
}

