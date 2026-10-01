package com.nura.messaging.features.auth.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.components.NuraGoogleButton
import com.nura.messaging.features.auth.presentation.components.StitchInputField
import com.nura.messaging.features.auth.presentation.dialogs.EmailVerificationDialog
import com.nura.messaging.features.auth.presentation.dialogs.ForgotPasswordDialog
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val focusManager = LocalFocusManager.current
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Navigation: Back Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = colors.brandLogoText,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 16.dp)
                ) {
                    Text(
                        text = "Welcome back.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.025).sp,
                        color = colors.brandLogoText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "Your conversations are waiting.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        lineHeight = 28.sp,
                        letterSpacing = (-0.005).sp,
                        color = colors.subtitleText
                    )
                }

                // General Error Banner (if any)
                AnimatedVisibility(
                    visible = uiState.generalError != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    uiState.generalError?.let { error ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                                .background(colorScheme.errorContainer, RoundedCornerShape(12.dp))
                                .border(1.dp, colorScheme.error.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Text(
                                text = error,
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 13.sp,
                                color = colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Field 1: Email or phone
                StitchInputField(
                    label = "Email or phone",
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChanged(it) },
                    placeholder = "Email ID",
                    errorMessage = uiState.emailError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email,
                        imeAction = ImeAction.Next
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { focusManager.moveFocus(FocusDirection.Down) }
                    ),
                    enabled = !uiState.isLoading && !uiState.isGoogleLoading
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Field 2: Password
                StitchInputField(
                    label = "Password",
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChanged(it) },
                    placeholder = "password",
                    errorMessage = uiState.passwordError,
                    visualTransformation = if (uiState.isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.signInWithEmail()
                        }
                    ),
                    trailingContent = {
                        IconButton(
                            onClick = { viewModel.togglePasswordVisibility() },
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isPasswordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                contentDescription = if (uiState.isPasswordVisible) "Hide password" else "Show password",
                                tint = colors.subtitleText,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    enabled = !uiState.isLoading && !uiState.isGoogleLoading
                )

                // Below Password: Forgot Password on Right
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    contentAlignment = Alignment.CenterEnd
                ) {
                    Text(
                        text = "Forgot password?",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 13.sp,
                        color = colors.subtitleText,
                        modifier = Modifier
                            .clickable(enabled = !uiState.isLoading) {
                                viewModel.showForgotPassword(true)
                            }
                            .padding(vertical = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Primary Button: Log in
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signInWithEmail()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.terracottaAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                    enabled = !uiState.isLoading && !uiState.isGoogleLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Log in",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                lineHeight = 22.sp
                            )
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                // Divider: or continue with
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(colors.dividerColor)
                    )
                    Text(
                        text = "OR CONTINUE WITH",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        letterSpacing = 0.06.sp,
                        color = Color(0xFF7D7D85)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(1.dp)
                            .background(colors.dividerColor)
                    )
                }

                // Continue with Google Button
                NuraGoogleButton(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.signInWithGoogle()
                    },
                    text = "Continue with Google",
                    isLoading = uiState.isGoogleLoading,
                    enabled = !uiState.isLoading
                )
            }

            // Footer: New to Nura? Create account
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val signUpAnnotated = buildAnnotatedString {
                    append("New to Nura? ")
                    withStyle(
                        SpanStyle(
                            color = colors.terracottaAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Create account")
                    }
                }

                Text(
                    text = signUpAnnotated,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = colors.subtitleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(enabled = !uiState.isLoading) {
                            onNavigateToSignUp()
                        }
                        .padding(8.dp)
                )
            }
        }
    }

    // Forgot Password Dialog
    if (uiState.showForgotPasswordDialog) {
        ForgotPasswordDialog(
            email = uiState.forgotPasswordEmail,
            emailError = uiState.forgotPasswordEmailError,
            isLoading = uiState.isForgotPasswordLoading,
            isSuccess = uiState.forgotPasswordSuccess,
            onEmailChange = { viewModel.onForgotPasswordEmailChanged(it) },
            onSubmit = { viewModel.sendPasswordReset() },
            onDismiss = { viewModel.showForgotPassword(false) }
        )
    }

    // Email Verification Dialog
    if (uiState.showVerificationNotice) {
        EmailVerificationDialog(
            email = uiState.verificationEmail,
            resendCooldownSeconds = uiState.resendCooldownSeconds,
            isResending = uiState.isResendingVerification,
            isResentSuccess = uiState.verificationResentSuccess,
            onResendClick = { viewModel.resendEmailVerification() },
            onDismiss = { viewModel.dismissVerificationNotice() }
        )
    }
}
