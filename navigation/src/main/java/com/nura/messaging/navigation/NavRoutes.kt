package com.nura.messaging.navigation

import kotlinx.serialization.Serializable

sealed interface NavRoute {

    @Serializable
    sealed interface Auth : NavRoute {
        @Serializable
        data object Welcome : Auth

        @Serializable
        data object Login : Auth

        @Serializable
        data object SignUp : Auth

        @Serializable
        data object OtpRequest : Auth

        @Serializable
        data object OtpVerify : Auth

        @Serializable
        data object ProfilePicture : Auth

        @Serializable
        data object NuraConnect : Auth
    }

    @Serializable
    data object Home : NavRoute

    @Serializable
    data class Chat(
        val conversationId: String,
        val participantId: String,
        val participantName: String,
        val participantUsername: String,
        val participantAvatarUrl: String? = null
    ) : NavRoute
}
