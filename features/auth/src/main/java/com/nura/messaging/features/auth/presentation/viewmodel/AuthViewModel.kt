package com.nura.messaging.features.auth.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nura.messaging.core.common.util.DispatcherProvider
import com.nura.messaging.domain.entities.auth.UsernameAvailability
import com.nura.messaging.domain.usecases.auth.CheckUsernameAvailabilityUseCase
import com.nura.messaging.domain.usecases.auth.GetCurrentUserUseCase
import com.nura.messaging.domain.usecases.auth.ObserveAuthStateUseCase
import com.nura.messaging.domain.usecases.auth.ResendEmailVerificationUseCase
import com.nura.messaging.domain.usecases.auth.SendEmailOtpUseCase
import com.nura.messaging.domain.usecases.auth.VerifyEmailOtpUseCase
import com.nura.messaging.domain.usecases.auth.SendPasswordResetUseCase
import com.nura.messaging.domain.usecases.auth.SignInWithEmailUseCase
import com.nura.messaging.domain.usecases.auth.SignInWithGoogleUseCase
import com.nura.messaging.domain.usecases.auth.SignOutUseCase
import com.nura.messaging.domain.usecases.auth.SignUpWithEmailUseCase
import com.nura.messaging.features.auth.presentation.state.AuthUiEvent
import com.nura.messaging.features.auth.presentation.state.AuthUiState
import com.nura.messaging.features.auth.presentation.state.PasswordStrengthLevel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import com.nura.messaging.features.auth.data.OnboardingPreferences
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

import com.nura.messaging.domain.usecases.contacts.GetLocalProfilePictureUseCase
import com.nura.messaging.domain.usecases.contacts.SaveUserProfilePictureUseCase
import com.nura.messaging.domain.usecases.contacts.SaveUserPresetAvatarUseCase
import com.nura.messaging.domain.usecases.contacts.SaveUserPresetColorUseCase
import com.nura.messaging.domain.usecases.contacts.GetUserPresetIndexUseCase
import com.nura.messaging.domain.usecases.contacts.GetUserPresetColorUseCase
import com.nura.messaging.domain.usecases.auth.UpdateProfileUseCase

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val signInWithEmailUseCase: SignInWithEmailUseCase,
    private val signUpWithEmailUseCase: SignUpWithEmailUseCase,
    private val checkUsernameAvailabilityUseCase: CheckUsernameAvailabilityUseCase,
    private val signInWithGoogleUseCase: SignInWithGoogleUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val sendPasswordResetUseCase: SendPasswordResetUseCase,
    private val resendEmailVerificationUseCase: ResendEmailVerificationUseCase,
    private val sendEmailOtpUseCase: SendEmailOtpUseCase,
    private val verifyEmailOtpUseCase: VerifyEmailOtpUseCase,
    private val observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val onboardingPreferences: OnboardingPreferences,
    private val dispatchers: DispatcherProvider,
    private val saveUserProfilePictureUseCase: SaveUserProfilePictureUseCase,
    private val getLocalProfilePictureUseCase: GetLocalProfilePictureUseCase,
    private val saveUserPresetAvatarUseCase: SaveUserPresetAvatarUseCase,
    private val saveUserPresetColorUseCase: SaveUserPresetColorUseCase,
    private val getUserPresetIndexUseCase: GetUserPresetIndexUseCase,
    private val getUserPresetColorUseCase: GetUserPresetColorUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val _uiEvents = MutableSharedFlow<AuthUiEvent>(extraBufferCapacity = 64)
    val uiEvents: SharedFlow<AuthUiEvent> = _uiEvents.asSharedFlow()

    private var cooldownJob: Job? = null
    private var otpCooldownJob: Job? = null
    private var usernameCheckJob: Job? = null

    init {
        checkInitialSession()
        observeAuthChanges()
    }

    private fun checkInitialSession() {
        viewModelScope.launch(dispatchers.io) {
            val user = getCurrentUserUseCase()
            val isOnboarded = if (user != null && user.id.isNotBlank()) {
                onboardingPreferences.isOnboardingCompleted(user.id)
            } else true

            val localPic = if (user != null && user.id.isNotBlank()) {
                getLocalProfilePictureUseCase(user.id)
            } else null
            val presetIndex = if (user != null && user.id.isNotBlank()) {
                getUserPresetIndexUseCase(user.id)
            } else null
            val presetColor = if (user != null && user.id.isNotBlank()) {
                getUserPresetColorUseCase(user.id)
            } else null

            _uiState.update {
                it.copy(
                    currentUser = user,
                    profilePictureUri = localPic ?: it.profilePictureUri,
                    selectedPresetIndex = presetIndex ?: it.selectedPresetIndex,
                    selectedPresetColor = presetColor ?: it.selectedPresetColor,
                    isInitialSessionChecking = false,
                    isNewUserRegistration = user != null && !isOnboarded
                )
            }
            if (user != null) {
                if (isOnboarded) {
                    _uiEvents.emit(AuthUiEvent.NavigateToHome)
                } else {
                    _uiEvents.emit(AuthUiEvent.NavigateToProfilePicture)
                }
            }
        }
    }

    private fun observeAuthChanges() {
        viewModelScope.launch(dispatchers.io) {
            observeAuthStateUseCase().collect { user ->
                val localPic = if (user != null && user.id.isNotBlank()) {
                    getLocalProfilePictureUseCase(user.id)
                } else null
                val presetIndex = if (user != null && user.id.isNotBlank()) {
                    getUserPresetIndexUseCase(user.id)
                } else null
                val presetColor = if (user != null && user.id.isNotBlank()) {
                    getUserPresetColorUseCase(user.id)
                } else null
                _uiState.update {
                    it.copy(
                        currentUser = user,
                        profilePictureUri = localPic ?: it.profilePictureUri,
                        selectedPresetIndex = presetIndex ?: it.selectedPresetIndex,
                        selectedPresetColor = presetColor ?: it.selectedPresetColor
                    )
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        _uiState.update {
            it.copy(
                name = name,
                nameError = if (it.nameError != null) validateName(name) else null,
                generalError = null
            )
        }
    }

    fun onUsernameChanged(rawUsername: String) {
        val clean = rawUsername.trim().removePrefix("@").filter { it.isLetterOrDigit() || it == '_' }
        val usernameError = if (clean.isNotEmpty()) validateUsername(clean) else null

        usernameCheckJob?.cancel()

        if (clean.isEmpty()) {
            _uiState.update {
                it.copy(
                    username = clean,
                    usernameError = null,
                    usernameAvailability = UsernameAvailability.IDLE,
                    generalError = null
                )
            }
            return
        }

        if (usernameError != null) {
            _uiState.update {
                it.copy(
                    username = clean,
                    usernameError = usernameError,
                    usernameAvailability = UsernameAvailability.INVALID,
                    generalError = null
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                username = clean,
                usernameError = null,
                usernameAvailability = UsernameAvailability.CHECKING,
                generalError = null
            )
        }

        usernameCheckJob = viewModelScope.launch(dispatchers.io) {
            delay(500L)
            val result = checkUsernameAvailabilityUseCase(clean)
            result.fold(
                onSuccess = { isAvailable ->
                    _uiState.update { current ->
                        if (current.username == clean) {
                            if (isAvailable) {
                                current.copy(
                                    usernameAvailability = UsernameAvailability.AVAILABLE,
                                    usernameError = null
                                )
                            } else {
                                current.copy(
                                    usernameAvailability = UsernameAvailability.TAKEN,
                                    usernameError = "@$clean is already taken"
                                )
                            }
                        } else current
                    }
                },
                onFailure = {
                    _uiState.update { current ->
                        if (current.username == clean) {
                            current.copy(
                                usernameAvailability = UsernameAvailability.ERROR,
                                usernameError = "Unable to check username availability"
                            )
                        } else current
                    }
                }
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = if (it.emailError != null) validateEmail(email) else null,
                generalError = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        val strength = calculatePasswordStrength(password)
        _uiState.update {
            it.copy(
                password = password,
                passwordStrength = strength,
                passwordError = if (it.passwordError != null) validatePassword(password) else null,
                generalError = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun signInWithEmail() {
        val state = _uiState.value
        if (state.isLoading || state.isGoogleLoading) return

        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.isEmpty()) "Password cannot be empty" else null

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                generalError = null,
                emailError = null,
                passwordError = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = signInWithEmailUseCase(state.email, state.password)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user
                        )
                    }
                    if (user.isEmailVerified) {
                        _uiEvents.emit(AuthUiEvent.NavigateToHome)
                    } else {
                        _uiState.update {
                            it.copy(
                                showVerificationNotice = true,
                                verificationEmail = state.email.trim()
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = error.message ?: "Failed to sign in. Please verify your credentials."
                        )
                    }
                }
            )
        }
    }

    fun signUpWithEmail() {
        val state = _uiState.value
        if (state.isLoading || state.isGoogleLoading) return

        val nameError = validateName(state.name)
        val usernameError = validateUsername(state.username)
        val emailError = validateEmail(state.email)
        val passwordError = validatePassword(state.password)

        if (nameError != null || usernameError != null || emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(
                    nameError = nameError,
                    usernameError = usernameError,
                    emailError = emailError,
                    passwordError = passwordError
                )
            }
            return
        }

        if (state.usernameAvailability == UsernameAvailability.TAKEN) {
            _uiState.update {
                it.copy(usernameError = "@${state.username} is already taken")
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                generalError = null,
                nameError = null,
                usernameError = null,
                emailError = null,
                passwordError = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = signUpWithEmailUseCase(
                name = state.name,
                username = state.username.trim().removePrefix("@"),
                email = state.email,
                password = state.password
            )
            result.fold(
                onSuccess = { user ->
                    onboardingPreferences.setOnboardingCompleted(user.id, false)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            currentUser = user,
                            showVerificationNotice = !user.isEmailVerified,
                            verificationEmail = state.email.trim(),
                            isNewUserRegistration = true
                        )
                    }
                    _uiEvents.emit(AuthUiEvent.NavigateToProfilePicture)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = error.message ?: "Failed to create account. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun signInWithGoogle() {
        val state = _uiState.value
        if (state.isLoading || state.isGoogleLoading) return

        _uiState.update {
            it.copy(
                isGoogleLoading = true,
                generalError = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = signInWithGoogleUseCase()
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            currentUser = user
                        )
                    }
                    _uiEvents.emit(AuthUiEvent.NavigateToHome)
                },
                onFailure = { error ->
                    // If user cancelled, don't show an alarming error banner
                    val isCancellation = error.message?.contains("cancelled", ignoreCase = true) == true
                    _uiState.update {
                        it.copy(
                            isGoogleLoading = false,
                            generalError = if (isCancellation) null else error.message ?: "Google sign-in was interrupted"
                        )
                    }
                }
            )
        }
    }

    fun showForgotPassword(show: Boolean) {
        _uiState.update {
            it.copy(
                showForgotPasswordDialog = show,
                forgotPasswordEmail = if (show) it.email else "",
                forgotPasswordEmailError = null,
                forgotPasswordSuccess = false
            )
        }
    }

    fun onForgotPasswordEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                forgotPasswordEmail = email,
                forgotPasswordEmailError = if (it.forgotPasswordEmailError != null) validateEmail(email) else null
            )
        }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        if (state.isForgotPasswordLoading) return

        val error = validateEmail(state.forgotPasswordEmail)
        if (error != null) {
            _uiState.update { it.copy(forgotPasswordEmailError = error) }
            return
        }

        _uiState.update {
            it.copy(
                isForgotPasswordLoading = true,
                forgotPasswordEmailError = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = sendPasswordResetUseCase(state.forgotPasswordEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isForgotPasswordLoading = false,
                            forgotPasswordSuccess = true
                        )
                    }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isForgotPasswordLoading = false,
                            forgotPasswordEmailError = err.message ?: "Could not send reset link. Please check the address."
                        )
                    }
                }
            )
        }
    }

    fun resendEmailVerification() {
        val state = _uiState.value
        if (state.isResendingVerification || state.resendCooldownSeconds > 0) return

        val targetEmail = state.verificationEmail.ifEmpty { state.email.trim() }
        if (targetEmail.isEmpty()) return

        _uiState.update {
            it.copy(
                isResendingVerification = true,
                verificationResentSuccess = false
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = resendEmailVerificationUseCase(targetEmail)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isResendingVerification = false,
                            verificationResentSuccess = true
                        )
                    }
                    startResendCooldown()
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isResendingVerification = false,
                            generalError = err.message ?: "Failed to resend verification. Please wait a moment."
                        )
                    }
                }
            )
        }
    }

    private fun startResendCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch(dispatchers.default) {
            _uiState.update { it.copy(resendCooldownSeconds = 60) }
            for (sec in 59 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(resendCooldownSeconds = sec) }
            }
        }
    }

    fun dismissVerificationNotice() {
        _uiState.update { it.copy(showVerificationNotice = false) }
    }

    fun signOut() {
        viewModelScope.launch(dispatchers.io) {
            signOutUseCase()
            _uiState.update {
                AuthUiState(
                    isInitialSessionChecking = false
                )
            }
            _uiEvents.emit(AuthUiEvent.NavigateToLogin)
        }
    }

    fun clearGeneralError() {
        _uiState.update { it.copy(generalError = null) }
    }

    // Validation rules
    private fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> "Name cannot be empty"
            trimmed.length < 2 -> "Name must be at least 2 characters"
            trimmed.length > 50 -> "Name cannot exceed 50 characters"
            else -> null
        }
    }

    private fun validateUsername(username: String): String? {
        val clean = username.trim().removePrefix("@")
        return when {
            clean.isEmpty() -> "Username cannot be empty"
            clean.length < 3 -> "Username must be at least 3 characters"
            clean.length > 20 -> "Username cannot exceed 20 characters"
            !clean.matches("^[a-zA-Z0-9_]+$".toRegex()) -> "Only letters, numbers, and underscores allowed"
            else -> null
        }
    }

    private fun validateEmail(email: String): String? {
        val trimmed = email.trim()
        val emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$".toRegex()
        return when {
            trimmed.isEmpty() -> "Email cannot be empty"
            trimmed.contains(" ") -> "Email cannot contain spaces"
            !emailRegex.matches(trimmed) -> "Please enter a valid email address"
            else -> null
        }
    }

    private fun validatePassword(password: String): String? {
        return when {
            password.isEmpty() -> "Password cannot be empty"
            password.length < 8 -> "Password must be at least 8 characters"
            else -> null
        }
    }

    private fun calculatePasswordStrength(password: String): PasswordStrengthLevel {
        if (password.isEmpty()) return PasswordStrengthLevel.EMPTY
        if (password.length < 8) return PasswordStrengthLevel.WEAK

        var score = 0
        if (password.length >= 8) score++
        if (password.length >= 12) score++
        if (password.any { it.isDigit() }) score++
        if (password.any { !it.isLetterOrDigit() }) score++
        if (password.any { it.isUpperCase() } && password.any { it.isLowerCase() }) score++

        return when {
            score <= 1 -> PasswordStrengthLevel.WEAK
            score in 2..3 -> PasswordStrengthLevel.GOOD
            else -> PasswordStrengthLevel.STRONG
        }
    }

    // Post-signup profile picture & identity actions
    fun onProfilePictureSelected(uriString: String?) {
        val user = _uiState.value.currentUser
        if (user != null && user.id.isNotBlank() && !uriString.isNullOrBlank()) {
            viewModelScope.launch(dispatchers.io) {
                val savedPath = saveUserProfilePictureUseCase(user.id, uriString)
                val targetUri = savedPath ?: uriString
                _uiState.update {
                    it.copy(
                        profilePictureUri = targetUri,
                        selectedPresetIndex = null,
                        selectedPresetColor = null
                    )
                }
                val base64 = compressImageToBase64(targetUri)
                if (!base64.isNullOrBlank()) {
                    updateProfileUseCase(
                        name = user.name.ifBlank { user.username },
                        about = user.about.ifBlank { "HI there i'm using nura" },
                        avatarUrl = base64
                    )
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    profilePictureUri = uriString,
                    selectedPresetIndex = null,
                    selectedPresetColor = null
                )
            }
        }
    }

    fun onPresetAvatarSelected(index: Int) {
        val user = _uiState.value.currentUser
        if (user != null && user.id.isNotBlank()) {
            viewModelScope.launch(dispatchers.io) {
                saveUserPresetAvatarUseCase(user.id, index)
                updateProfileUseCase(
                    name = user.name.ifBlank { user.username },
                    about = user.about.ifBlank { "HI there i'm using nura" },
                    avatarUrl = "preset:$index"
                )
            }
        }
        _uiState.update {
            it.copy(
                selectedPresetIndex = index,
                profilePictureUri = null,
                selectedPresetColor = null
            )
        }
    }

    fun onPresetColorSelected(colorArgb: Long) {
        val user = _uiState.value.currentUser
        if (user != null && user.id.isNotBlank()) {
            viewModelScope.launch(dispatchers.io) {
                saveUserPresetColorUseCase(user.id, colorArgb)
                updateProfileUseCase(
                    name = user.name.ifBlank { user.username },
                    about = user.about.ifBlank { "HI there i'm using nura" },
                    avatarUrl = "color:$colorArgb"
                )
            }
        }
        _uiState.update {
            it.copy(
                selectedPresetColor = colorArgb,
                profilePictureUri = null,
                selectedPresetIndex = null
            )
        }
    }

    fun completePostSignUpOnboarding() {
        val user = _uiState.value.currentUser
        if (user != null && user.id.isNotBlank()) {
            onboardingPreferences.setOnboardingCompleted(user.id, true)
            val pic = _uiState.value.profilePictureUri
            val presetIdx = _uiState.value.selectedPresetIndex
            val presetColor = _uiState.value.selectedPresetColor
            viewModelScope.launch(dispatchers.io) {
                val avatarUrl = when {
                    !pic.isNullOrBlank() -> {
                        saveUserProfilePictureUseCase(user.id, pic)
                        compressImageToBase64(pic)
                    }
                    presetIdx != null -> {
                        saveUserPresetAvatarUseCase(user.id, presetIdx)
                        "preset:$presetIdx"
                    }
                    presetColor != null -> {
                        saveUserPresetColorUseCase(user.id, presetColor)
                        "color:$presetColor"
                    }
                    else -> null
                }
                updateProfileUseCase(
                    name = user.name.ifBlank { user.username },
                    about = user.about.ifBlank { "HI there i'm using nura" },
                    avatarUrl = avatarUrl
                )
            }
        }
        _uiState.update { it.copy(isNewUserRegistration = false) }
    }

    fun saveUserProfile(name: String, about: String) {
        val current = _uiState.value.currentUser ?: return
        val updated = current.copy(name = name, about = about)
        _uiState.update { it.copy(currentUser = updated) }
        viewModelScope.launch(dispatchers.io) {
            val pic = _uiState.value.profilePictureUri
            val presetIdx = _uiState.value.selectedPresetIndex
            val presetColor = _uiState.value.selectedPresetColor
            val avatarUrl = when {
                !pic.isNullOrBlank() -> compressImageToBase64(pic)
                presetIdx != null -> "preset:$presetIdx"
                presetColor != null -> "color:$presetColor"
                else -> null
            }
            updateProfileUseCase(name, about, avatarUrl)
        }
    }

    private fun compressImageToBase64(uriString: String): String? {
        return try {
            if (uriString.startsWith("data:") || uriString.startsWith("http")) return uriString
            val path = if (uriString.startsWith("file://")) uriString.removePrefix("file://") else uriString
            val file = java.io.File(path)
            val bitmap = if (file.exists()) {
                android.graphics.BitmapFactory.decodeFile(file.absolutePath)
            } else {
                null
            }
            if (bitmap != null) {
                // Downscale to 160x160 to keep payload compact (<8KB) for Supabase
                val scaled = android.graphics.Bitmap.createScaledBitmap(bitmap, 160, 160, true)
                val outputStream = java.io.ByteArrayOutputStream()
                scaled.compress(android.graphics.Bitmap.CompressFormat.JPEG, 75, outputStream)
                val bytes = outputStream.toByteArray()
                "data:image/jpeg;base64," + android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun updateUserName(name: String) {
        saveUserProfile(name, _uiState.value.currentUser?.about ?: "HI there i'm using nura")
    }

    fun setOtpMode(enabled: Boolean) {
        _uiState.update {
            it.copy(
                isOtpMode = enabled,
                generalError = null,
                otpCode = "",
                otpCodeError = null
            )
        }
    }

    fun onOtpCodeChanged(code: String) {
        val filtered = code.filter { it.isDigit() }.take(6)
        _uiState.update {
            it.copy(
                otpCode = filtered,
                otpCodeError = if (it.otpCodeError != null && filtered.length == 6) null else it.otpCodeError,
                generalError = null
            )
        }
    }

    fun sendEmailOtp() {
        val state = _uiState.value
        if (state.isOtpSending || state.otpCooldownSeconds > 0) return

        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        _uiState.update {
            it.copy(
                isOtpSending = true,
                generalError = null,
                emailError = null,
                otpSentSuccess = false
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = sendEmailOtpUseCase(state.email)
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isOtpSending = false,
                            otpSentSuccess = true
                        )
                    }
                    startOtpCooldown()
                    _uiEvents.emit(AuthUiEvent.NavigateToOtpVerify)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isOtpSending = false,
                            generalError = error.message ?: "Failed to send OTP. Please try again."
                        )
                    }
                }
            )
        }
    }

    fun verifyEmailOtp() {
        val state = _uiState.value
        if (state.isOtpVerifying) return

        val emailError = validateEmail(state.email)
        val otpError = if (state.otpCode.length < 6) "Please enter complete 6-digit code" else null

        if (emailError != null || otpError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    otpCodeError = otpError
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isOtpVerifying = true,
                generalError = null,
                otpCodeError = null
            )
        }

        viewModelScope.launch(dispatchers.io) {
            val result = verifyEmailOtpUseCase(state.email, state.otpCode)
            result.fold(
                onSuccess = { user ->
                    _uiState.update {
                        it.copy(
                            isOtpVerifying = false,
                            currentUser = user
                        )
                    }
                    _uiEvents.emit(AuthUiEvent.NavigateToHome)
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isOtpVerifying = false,
                            generalError = error.message ?: "Invalid OTP code. Please check and try again."
                        )
                    }
                }
            )
        }
    }

    private fun startOtpCooldown() {
        otpCooldownJob?.cancel()
        otpCooldownJob = viewModelScope.launch(dispatchers.default) {
            _uiState.update { it.copy(otpCooldownSeconds = 60) }
            for (sec in 59 downTo 0) {
                delay(1000L)
                _uiState.update { it.copy(otpCooldownSeconds = sec) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cooldownJob?.cancel()
        otpCooldownJob?.cancel()
        usernameCheckJob?.cancel()
    }
}
