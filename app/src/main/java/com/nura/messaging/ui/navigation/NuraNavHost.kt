package com.nura.messaging.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.component.NuraBrandIcon
import com.nura.messaging.core.common.ui.component.NuraWordmark
import com.nura.messaging.features.auth.presentation.screens.LoginScreen
import com.nura.messaging.features.auth.presentation.screens.NuraConnectScreen
import com.nura.messaging.features.auth.presentation.screens.OtpRequestScreen
import com.nura.messaging.features.auth.presentation.screens.OtpVerifyScreen
import com.nura.messaging.features.auth.presentation.screens.ProfilePictureScreen
import com.nura.messaging.features.auth.presentation.screens.SignUpScreen
import com.nura.messaging.features.auth.presentation.screens.WelcomeScreen
import com.nura.messaging.features.auth.presentation.state.AuthUiEvent
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel
import com.nura.messaging.features.home.presentation.HomeScreen
import com.nura.messaging.features.profile.presentation.screens.AccountScreen
import com.nura.messaging.features.settings.presentation.screens.SettingsScreen
import com.nura.messaging.navigation.NavRoute
import kotlinx.coroutines.flow.collectLatest

@Composable
fun NuraNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()

    // React to one-off navigation events
    LaunchedEffect(authViewModel) {
        authViewModel.uiEvents.collectLatest { event ->
            when (event) {
                is AuthUiEvent.NavigateToHome -> {
                    if (!authViewModel.uiState.value.isNewUserRegistration) {
                        navController.navigate(NavRoute.Home) {
                            popUpTo(NavRoute.Auth.Welcome) { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                }
                is AuthUiEvent.NavigateToLogin -> {
                    navController.navigate(NavRoute.Auth.Login) {
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.NavigateToSignUp -> {
                    navController.navigate(NavRoute.Auth.SignUp) {
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.NavigateToOtpRequest -> {
                    navController.navigate(NavRoute.Auth.OtpRequest) {
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.NavigateToOtpVerify -> {
                    navController.navigate(NavRoute.Auth.OtpVerify) {
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.NavigateToProfilePicture -> {
                    navController.navigate(NavRoute.Auth.ProfilePicture) {
                        popUpTo(NavRoute.Auth.SignUp) { inclusive = true }
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.NavigateToNuraConnect -> {
                    navController.navigate(NavRoute.Auth.NuraConnect) {
                        launchSingleTop = true
                    }
                }
                is AuthUiEvent.ShowSnackbar -> {
                    // Handled if scaffold snackbar host is present
                }
            }
        }
    }

    // Cold start gate: hold until session is checked without showing redundant splash
    if (uiState.isInitialSessionChecking) {
        return
    }

    val startDestination: Any = if (uiState.currentUser != null) {
        if (uiState.isNewUserRegistration) {
            NavRoute.Auth.ProfilePicture
        } else {
            NavRoute.Home
        }
    } else {
        NavRoute.Auth.Welcome
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable<NavRoute.Auth.Welcome>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.End,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Start,
                    animationSpec = tween(300)
                )
            }
        ) {
            WelcomeScreen(
                viewModel = authViewModel,
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Auth.Login)
                },
                onNavigateToSignUp = {
                    navController.navigate(NavRoute.Auth.SignUp)
                }
            )
        }

        composable<NavRoute.Auth.Login>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            LoginScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToSignUp = {
                    navController.navigate(NavRoute.Auth.SignUp) {
                        popUpTo(NavRoute.Auth.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Auth.OtpRequest>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            OtpRequestScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                },
                onOtpSent = {
                    navController.navigate(NavRoute.Auth.OtpVerify)
                }
            )
        }

        composable<NavRoute.Auth.OtpVerify>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            OtpVerifyScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Auth.Login) {
                        popUpTo(NavRoute.Auth.Login) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Auth.SignUp>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SignUpScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToLogin = {
                    navController.navigate(NavRoute.Auth.Login) {
                        popUpTo(NavRoute.Auth.SignUp) { inclusive = true }
                    }
                }
            )
        }

        composable<NavRoute.Auth.ProfilePicture>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            ProfilePictureScreen(
                viewModel = authViewModel,
                onNavigateBack = {
                    navController.popBackStack()
                },
                onContinue = {
                    if (uiState.currentUser != null && !uiState.isNewUserRegistration) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoute.Auth.NuraConnect)
                    }
                },
                onSkip = {
                    if (uiState.currentUser != null && !uiState.isNewUserRegistration) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(NavRoute.Auth.NuraConnect)
                    }
                }
            )
        }

        composable<NavRoute.Auth.NuraConnect>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            popEnterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            },
            popExitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            NuraConnectScreen(
                viewModel = authViewModel,
                onContinue = {
                    navController.navigate(NavRoute.Home) {
                        popUpTo(NavRoute.Auth.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onSkip = {
                    navController.navigate(NavRoute.Home) {
                        popUpTo(NavRoute.Auth.Welcome) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable<NavRoute.Home> {
            val connectViewModel: com.nura.messaging.features.contacts.presentation.viewmodel.ConnectViewModel = hiltViewModel()
            val homeViewModel: com.nura.messaging.features.home.presentation.viewmodel.HomeViewModel = hiltViewModel()
            val conversations by homeViewModel.conversations.collectAsStateWithLifecycle()

            HomeScreen(
                user = uiState.currentUser,
                conversations = conversations,
                profilePictureUri = uiState.profilePictureUri,
                selectedPresetIndex = uiState.selectedPresetIndex,
                selectedPresetColor = uiState.selectedPresetColor,
                onNavigateToAccount = {
                    navController.navigate(NavRoute.Account)
                },
                onNavigateToSettings = {
                    navController.navigate(NavRoute.Settings)
                },
                onConversationClick = { item ->
                    val isReq = !item.isProfileShared && !item.isOutgoing
                    navController.navigate(
                        NavRoute.Chat(
                            conversationId = item.id,
                            participantId = item.participantId,
                            participantName = item.name,
                            participantUsername = item.participantUsername,
                            participantAvatarUrl = item.avatarUrl,
                            participantAbout = null,
                            isRequest = isReq
                        )
                    )
                },
                onSignOut = {
                    authViewModel.signOut()
                    navController.navigate(NavRoute.Auth.Welcome) {
                        popUpTo(NavRoute.Home) { inclusive = true }
                    }
                },
                connectContent = { onDismiss ->
                    com.nura.messaging.features.contacts.presentation.screens.ConnectScreen(
                        viewModel = connectViewModel,
                        userId = uiState.currentUser?.id.orEmpty(),
                        username = uiState.currentUser?.username.orEmpty(),
                        name = uiState.currentUser?.name.orEmpty(),
                        onBack = onDismiss,
                        onOpenChat = { user ->
                            onDismiss()
                            val myId = uiState.currentUser?.id.orEmpty()
                            val convId = if (myId < user.id) "${myId}_${user.id}" else "${user.id}_${myId}"
                            navController.navigate(
                                NavRoute.Chat(
                                    conversationId = convId,
                                    participantId = user.id,
                                    participantName = user.displayName.ifEmpty { user.username },
                                    participantUsername = user.username,
                                    participantAvatarUrl = user.avatarUri,
                                    participantAbout = null,
                                    isRequest = false
                                )
                            )
                        }
                    )
                }
            )
        }

        composable<NavRoute.Chat>(
            enterTransition = {
                androidx.compose.animation.scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.EaseOutCubic)
                ) + androidx.compose.animation.fadeIn(animationSpec = tween(300))
            },
            exitTransition = {
                androidx.compose.animation.scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseInCubic)
                ) + androidx.compose.animation.fadeOut(animationSpec = tween(250))
            },
            popEnterTransition = {
                androidx.compose.animation.scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(300, easing = androidx.compose.animation.core.EaseOutCubic)
                ) + androidx.compose.animation.fadeIn(animationSpec = tween(300))
            },
            popExitTransition = {
                androidx.compose.animation.scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(250, easing = androidx.compose.animation.core.EaseInCubic)
                ) + androidx.compose.animation.fadeOut(animationSpec = tween(250))
            }
        ) { backStackEntry ->
            val chatRoute = backStackEntry.toRoute<NavRoute.Chat>()
            com.nura.messaging.features.chat.presentation.screens.ChatScreen(
                conversationId = chatRoute.conversationId,
                participantId = chatRoute.participantId,
                participantName = chatRoute.participantName,
                participantUsername = chatRoute.participantUsername,
                participantAvatarUrl = chatRoute.participantAvatarUrl,
                participantAbout = chatRoute.participantAbout,
                isRequest = chatRoute.isRequest,
                onBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<NavRoute.Account>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            AccountScreen(
                currentUser = uiState.currentUser,
                profilePictureUri = uiState.profilePictureUri,
                selectedPresetIndex = uiState.selectedPresetIndex,
                selectedPresetColor = uiState.selectedPresetColor,
                onUpdateProfilePicture = { uri ->
                    authViewModel.onProfilePictureSelected(uri)
                },
                onSaveProfile = { name, about ->
                    authViewModel.saveUserProfile(name, about)
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        composable<NavRoute.Settings>(
            enterTransition = {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                )
            },
            exitTransition = {
                slideOutOfContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                )
            }
        ) {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
