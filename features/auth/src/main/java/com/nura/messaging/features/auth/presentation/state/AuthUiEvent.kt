package com.nura.messaging.features.auth.presentation.state

sealed interface AuthUiEvent {
    data object NavigateToHome : AuthUiEvent
    data object NavigateToLogin : AuthUiEvent
    data object NavigateToSignUp : AuthUiEvent
    data object NavigateToOtpRequest : AuthUiEvent
    data object NavigateToOtpVerify : AuthUiEvent
    data object NavigateToProfilePicture : AuthUiEvent
    data object NavigateToNuraConnect : AuthUiEvent
    data class ShowSnackbar(val message: String) : AuthUiEvent
}
