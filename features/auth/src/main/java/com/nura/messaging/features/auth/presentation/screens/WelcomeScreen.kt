package com.nura.messaging.features.auth.presentation.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.component.NuraWordmark
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.dialogs.LegalBottomSheet
import com.nura.messaging.features.auth.presentation.dialogs.LegalDocumentType
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel

@Composable
fun WelcomeScreen(
    viewModel: AuthViewModel,
    onNavigateToLogin: () -> Unit,
    onNavigateToSignUp: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    var selectedLegalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = colorScheme.surface
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Brand Space Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NuraWordmark()

                    // Calm space badge with pulsing indicator
                    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
                    val pulseAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.4f,
                        targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(durationMillis = 1000),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "pulse_alpha"
                    )

                    Row(
                        modifier = Modifier
                            .background(
                                color = colors.badgeBackground,
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .border(
                                width = 1.dp,
                                color = colors.badgeBorder,
                                shape = RoundedCornerShape(9999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .alpha(pulseAlpha)
                                .background(colors.terracottaAccent, CircleShape)
                        )
                        Text(
                            text = "calm space",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            letterSpacing = 0.04.sp,
                            color = colors.badgeText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Abstract Conversation Architecture (Ma / Negative Space)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(172.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(172.dp)
                    ) {
                        // Top-right dot and line (opacity 50%)
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-32).dp, y = 0.dp)
                                .alpha(0.5f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(4.dp)
                                    .background(colors.terracottaAccent, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(48.dp)
                                    .height(1.dp)
                                    .background(colors.cardDarkBorder)
                            )
                        }

                        // Top-left dark capsule
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 8.dp, y = 16.dp)
                                .width(192.dp)
                                .height(40.dp)
                                .background(
                                    color = colors.cardDarkBubble,
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomEnd = 20.dp,
                                        bottomStart = 4.dp
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    color = colors.cardDarkBorder,
                                    shape = RoundedCornerShape(
                                        topStart = 20.dp,
                                        topEnd = 20.dp,
                                        bottomEnd = 20.dp,
                                        bottomStart = 4.dp
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(colors.terracottaAccent.copy(alpha = 0.8f), CircleShape)
                                )
                                Box(
                                    modifier = Modifier
                                        .width(64.dp)
                                        .height(4.dp)
                                        .background(colors.cardDarkBorder, RoundedCornerShape(9999.dp))
                                )
                            }
                        }

                        // Center floating light audio message card
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 0.dp, y = 64.dp)
                                .width(256.dp)
                                .height(48.dp)
                                .shadow(
                                    elevation = 8.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 22.dp,
                                        bottomEnd = 22.dp,
                                        bottomStart = 22.dp
                                    ),
                                    ambientColor = Color.Black.copy(alpha = 0.4f),
                                    spotColor = Color.Black.copy(alpha = 0.4f)
                                )
                                .background(
                                    color = colors.cardLightBubble,
                                    shape = RoundedCornerShape(
                                        topStart = 4.dp,
                                        topEnd = 22.dp,
                                        bottomEnd = 22.dp,
                                        bottomStart = 22.dp
                                    )
                                )
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    // Black dot
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(colors.cardLightOnBubble, CircleShape)
                                    )

                                    // 8 waveform bars
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(12.dp)
                                                .background(colors.waveformLight, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(20.dp)
                                                .background(colors.waveformDark, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(8.dp)
                                                .background(colors.waveformLight, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(24.dp)
                                                .background(colors.terracottaAccent, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(16.dp)
                                                .background(colors.waveformDark, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(10.dp)
                                                .background(colors.waveformLight, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(18.dp)
                                                .background(colors.waveformDark, RoundedCornerShape(9999.dp))
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(4.dp)
                                                .height(8.dp)
                                                .background(colors.waveformLight, RoundedCornerShape(9999.dp))
                                        )
                                    }
                                }

                                Text(
                                    text = "0:14",
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    letterSpacing = 0.05.sp,
                                    color = colors.cardLightDuration
                                )
                            }
                        }

                        // Bottom-left terracotta microphone bubble
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .offset(x = 32.dp, y = 120.dp)
                                .width(48.dp)
                                .height(40.dp)
                                .shadow(
                                    elevation = 6.dp,
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd = 4.dp
                                    ),
                                    ambientColor = colors.terracottaAccent.copy(alpha = 0.2f),
                                    spotColor = colors.terracottaAccent.copy(alpha = 0.2f)
                                )
                                .background(
                                    color = colors.terracottaAccent,
                                    shape = RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 18.dp,
                                        bottomStart = 18.dp,
                                        bottomEnd = 4.dp
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = "Voice Message",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        // Bottom-right connecting lines and dot
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = (-40).dp, y = 138.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(56.dp)
                                    .height(1.dp)
                                    .background(colors.cardDarkBorder)
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .background(colors.terracottaAccent, CircleShape)
                            )
                            Box(
                                modifier = Modifier
                                    .width(24.dp)
                                    .height(1.dp)
                                    .background(colors.cardDarkBorder)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // Editorial Headline & Copy
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "PRESENCE • INTENTION",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.04.sp,
                        color = colors.terracottaAccent
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "A quieter way\nto connect.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        lineHeight = 38.sp,
                        letterSpacing = (-0.025).sp,
                        color = colors.brandLogoText
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Your conversations, without the noise.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 16.sp,
                        lineHeight = 24.sp,
                        letterSpacing = (-0.005).sp,
                        color = colors.subtitleText
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Action Area
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Log in Primary Button
                Button(
                    onClick = onNavigateToLogin,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.terracottaAccent,
                        contentColor = Color.White
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                ) {
                    Text(
                        text = "Log in",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.01).sp
                    )
                }

                // Create account Secondary Button
                Button(
                    onClick = onNavigateToSignUp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.secondaryButtonBackground,
                        contentColor = colors.brandLogoText
                    ),
                    border = BorderStroke(1.dp, colors.secondaryButtonBorder),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Text(
                        text = "Create account",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        letterSpacing = (-0.01).sp,
                        color = colors.brandLogoText
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Bottom Legal Text
                @OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "By continuing, you agree to Nura’s ",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.legalText
                    )
                    Text(
                        text = "Terms",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.badgeText,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                selectedLegalDocument = LegalDocumentType.TERMS
                            }
                    )
                    Text(
                        text = " and ",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.legalText
                    )
                    Text(
                        text = "Privacy Policy",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.badgeText,
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier
                            .clickable {
                                selectedLegalDocument = LegalDocumentType.PRIVACY
                            }
                    )
                    Text(
                        text = ".",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        color = colors.legalText
                    )
                }
            }
        }
    }

    selectedLegalDocument?.let { docType ->
        LegalBottomSheet(
            initialDocument = docType,
            onDismiss = { selectedLegalDocument = null }
        )
    }
}
