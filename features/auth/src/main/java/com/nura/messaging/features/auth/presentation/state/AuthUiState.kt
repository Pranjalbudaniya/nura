package com.nura.messaging.features.auth.presentation.state

import androidx.compose.runtime.Immutable
import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.domain.entities.auth.UsernameAvailability

enum class PasswordStrengthLevel {
    EMPTY,
    WEAK,
    GOOD,
    STRONG
}

@Immutable
data class AuthUiState(
    // Sign In / Sign Up form fields
    val name: String = "",
    val username: String = "",
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,

    // Inline validation errors
    val nameError: String? = null,
    val usernameError: String? = null,
    val usernameAvailability: UsernameAvailability = UsernameAvailability.IDLE,
    val emailError: String? = null,
    val passwordError: String? = null,
    val passwordStrength: PasswordStrengthLevel = PasswordStrengthLevel.EMPTY,

    // Action loading states
    val isLoading: Boolean = false,
    val isGoogleLoading: Boolean = false,
    val generalError: String? = null,

    // Forgot Password state
    val showForgotPasswordDialog: Boolean = false,
    val forgotPasswordEmail: String = "",
    val forgotPasswordEmailError: String? = null,
    val isForgotPasswordLoading: Boolean = false,
    val forgotPasswordSuccess: Boolean = false,

    // Email verification state
    val showVerificationNotice: Boolean = false,
    val verificationEmail: String = "",
    val resendCooldownSeconds: Int = 0,
    val isResendingVerification: Boolean = false,
    val verificationResentSuccess: Boolean = false,

    // OTP Authentication state
    val isOtpMode: Boolean = false,
    val otpCode: String = "",
    val otpCodeError: String? = null,
    val isOtpSending: Boolean = false,
    val isOtpVerifying: Boolean = false,
    val otpSentSuccess: Boolean = false,
    val otpCooldownSeconds: Int = 0,

    // Post-signup onboarding state
    val profilePictureUri: String? = null,
    val selectedPresetIndex: Int? = null,
    val selectedPresetColor: Long? = null,
    val isUploadingProfilePicture: Boolean = false,
    val isNewUserRegistration: Boolean = false,

    // Authenticated user & session
    val currentUser: AuthUser? = null,
    val isInitialSessionChecking: Boolean = true
)
