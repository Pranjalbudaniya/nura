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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.components.StitchInputField
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun OtpVerifyScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToLogin: () -> Unit,
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
                        .padding(top = 24.dp, bottom = 20.dp)
                ) {
                    Text(
                        text = "Verify code.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.025).sp,
                        color = colors.brandLogoText
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (uiState.email.isNotEmpty()) {
                            "Enter the 6-digit code sent to ${uiState.email}."
                        } else {
                            "Enter the 6-digit code sent to your email."
                        },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 17.sp,
                        lineHeight = 26.sp,
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

                // OTP Code Input Field
                StitchInputField(
                    label = "6-Digit OTP Code",
                    value = uiState.otpCode,
                    onValueChange = { viewModel.onOtpCodeChanged(it) },
                    placeholder = "Enter 6-digit code",
                    errorMessage = uiState.otpCodeError,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.NumberPassword,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            focusManager.clearFocus()
                            viewModel.verifyEmailOtp()
                        }
                    ),
                    enabled = !uiState.isOtpVerifying
                )

                // Resend timer and status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Didn't receive it?",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 13.sp,
                        color = colors.subtitleText
                    )

                    if (uiState.otpCooldownSeconds > 0) {
                        Text(
                            text = "Resend in ${uiState.otpCooldownSeconds}s",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 13.sp,
                            color = colors.subtitleText
                        )
                    } else {
                        Text(
                            text = "Resend code",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = colors.terracottaAccent,
                            modifier = Modifier
                                .clickable(enabled = !uiState.isOtpSending) {
                                    viewModel.sendEmailOtp()
                                }
                                .padding(vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Primary Button: Verify & Log in
                Button(
                    onClick = {
                        focusManager.clearFocus()
                        viewModel.verifyEmailOtp()
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
                    enabled = !uiState.isOtpVerifying && uiState.otpCode.length == 6
                ) {
                    if (uiState.isOtpVerifying) {
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
                                text = "Verify & Log in",
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
            }

            // Footer Link: Change email address or log in with password
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val changeAnnotated = buildAnnotatedString {
                    append("Need to change email? ")
                    withStyle(
                        SpanStyle(
                            color = colors.terracottaAccent,
                            fontWeight = FontWeight.SemiBold
                        )
                    ) {
                        append("Change email")
                    }
                }

                Text(
                    text = changeAnnotated,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 15.sp,
                    lineHeight = 24.sp,
                    color = colors.subtitleText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clickable(enabled = !uiState.isOtpVerifying) {
                            onNavigateBack()
                        }
                        .padding(8.dp)
                )
            }
        }
    }
}
