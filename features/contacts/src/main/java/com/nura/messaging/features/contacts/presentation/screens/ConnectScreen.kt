package com.nura.messaging.features.contacts.presentation.screens

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.domain.entities.contacts.ConnectionUser
import com.nura.messaging.features.auth.presentation.screens.QrScannerScreen
import com.nura.messaging.features.auth.presentation.util.QrCodeGenerator
import com.nura.messaging.features.contacts.presentation.viewmodel.ConnectViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConnectScreen(
    viewModel: ConnectViewModel,
    userId: String,
    username: String = "",
    name: String = "",
    onBack: () -> Unit,
    onOpenChat: (ConnectionUser) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val focusManager = LocalFocusManager.current

    LaunchedEffect(userId, username, name) {
        viewModel.initialize(userId, username, name)
    }

    // Auto-dismiss toast
    LaunchedEffect(uiState.showToast) {
        if (uiState.showToast) {
            delay(2400)
            viewModel.dismissToast()
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is com.nura.messaging.features.contacts.presentation.viewmodel.ConnectUiEvent.NavigateToChat -> {
                    onOpenChat(event.user)
                }
            }
        }
    }

    BackHandler {
        if (uiState.showScanner) {
            viewModel.setShowScanner(false)
        } else if (uiState.showYourQrSheet) {
            viewModel.setShowYourQr(false)
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                focusManager.clearFocus()
            },
        containerColor = colorScheme.surface
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // TOP APP BAR
                item(key = "top_bar") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Circular back button in terracotta primary container
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(colors.terracottaAccent)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Go back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Centered uppercase label: CONNECT
                        Text(
                            text = "CONNECT",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            letterSpacing = 2.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )

                        // Balanced right spacer
                        Spacer(modifier = Modifier.size(40.dp))
                    }
                }

                // INTRODUCTION
                item(key = "intro") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp, bottom = 18.dp)
                    ) {
                        Text(
                            text = "Connect with someone.",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 26.sp,
                            lineHeight = 34.sp,
                            letterSpacing = (-0.015).sp,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Use their username or scan their architectural QR signature to open a private line.",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }

                // PRIMARY CONNECTION FORM: USER KEY
                item(key = "connection_form") {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "NURA USERNAME",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(colors.terracottaAccent, CircleShape)
                                )
                                Text(
                                    text = "End-to-End",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = colors.terracottaAccent.copy(alpha = 0.85f)
                                )
                            }
                        }

                        // Architectural Input Field Box
                        var isFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isFocused) colorScheme.surfaceContainer else colorScheme.surfaceContainerLow)
                                .border(
                                    width = 1.dp,
                                    color = if (isFocused) colors.terracottaAccent.copy(alpha = 0.6f) else colorScheme.surfaceContainerHigh,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AlternateEmail,
                                        contentDescription = null,
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Box(modifier = Modifier.weight(1f)) {
                                    if (uiState.usernameInput.isEmpty()) {
                                        Text(
                                            text = "@username",
                                            fontFamily = PlusJakartaSansFamily,
                                            fontSize = 15.sp,
                                            color = colors.inputPlaceholder
                                        )
                                    }
                                    BasicTextField(
                                        value = uiState.usernameInput,
                                        onValueChange = viewModel::onUsernameChanged,
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Text,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onDone = {
                                                focusManager.clearFocus()
                                                viewModel.onConnectClicked()
                                            }
                                        ),
                                        textStyle = TextStyle(
                                            fontFamily = PlusJakartaSansFamily,
                                            fontSize = 15.sp,
                                            color = colorScheme.onSurface
                                        ),
                                        cursorBrush = SolidColor(colors.terracottaAccent),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isFocused = it.isFocused }
                                    )
                                }
                                if (uiState.usernameInput.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.onUsernameChanged("") },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Close,
                                            contentDescription = "Clear",
                                            tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Primary Action Button
                        Button(
                            onClick = {
                                focusManager.clearFocus()
                                viewModel.onConnectClicked()
                            },
                            enabled = !uiState.isConnecting,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.terracottaAccent,
                                contentColor = Color.White
                            ),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (uiState.isConnecting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "Connect",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 16.sp
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
                }

                // OR DIVIDER
                item(key = "or_divider") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 20.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(colorScheme.surfaceContainerHigh)
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(colorScheme.surfaceContainer)
                                .border(1.dp, colorScheme.surfaceContainerHigh, CircleShape)
                                .padding(horizontal = 14.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "or",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                // SECONDARY ACTIONS: QR & SHARE
                item(key = "secondary_actions") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Action: Scan QR code
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceContainerLow)
                                .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setShowScanner(true) }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCodeScanner,
                                        contentDescription = null,
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Scan QR code",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Action: Your QR
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colorScheme.surfaceContainerLow)
                                .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
                                .clickable { viewModel.setShowYourQr(true) }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorScheme.surfaceContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.QrCode,
                                        contentDescription = null,
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Your QR",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurface
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                                contentDescription = null,
                                tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // RECENT CONNECTIONS HEADER
                item(key = "recent_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 24.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENT CONNECTIONS",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                        if (uiState.recentConnections.isNotEmpty()) {
                            Text(
                                text = "${uiState.recentConnections.size} Encrypted",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 11.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                            )
                        }
                    }
                }

                // RECENT CONNECTIONS LIST OR EMPTY STATE
                if (uiState.recentConnections.isEmpty()) {
                    item(key = "empty_state") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(colorScheme.surfaceContainerLow)
                                .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(18.dp))
                                .padding(vertical = 32.dp, horizontal = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(CircleShape)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    colors.terracottaAccent.copy(alpha = 0.18f),
                                                    Color.Transparent
                                                )
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(colorScheme.surfaceContainer)
                                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(14.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.PersonAdd,
                                            contentDescription = null,
                                            tint = colors.terracottaAccent,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "No recent connections",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                    color = colorScheme.onSurface,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(6.dp))

                                Text(
                                    text = "Enter a username or scan a QR code above to connect and start a secure conversation.",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 13.sp,
                                    lineHeight = 19.sp,
                                    color = colors.subtitleText,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 12.dp)
                                )
                            }
                        }
                    }
                } else {
                    items(uiState.recentConnections, key = { it.id }) { connection ->
                        ConnectionRowItem(
                            connection = connection,
                            onClickChat = { onOpenChat(connection) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(36.dp))
                }
            }

            // FLOATING TOAST NOTIFICATION CONTAINER
            AnimatedVisibility(
                visible = uiState.showToast,
                enter = slideInVertically(initialOffsetY = { it / 2 }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it / 2 }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .zIndex(10f)
            ) {
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerHighest)
                        .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.3f), CircleShape)
                        .shadow(elevation = 12.dp, shape = CircleShape)
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Verified,
                            contentDescription = null,
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = uiState.toastMessage.orEmpty(),
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colorScheme.onSurface
                        )
                    }
                }
            }

            // YOUR QR CODE MODAL BOTTOM SHEET
            if (uiState.showYourQrSheet) {
                val sheetState = rememberModalBottomSheetState()
                ModalBottomSheet(
                    onDismissRequest = { viewModel.setShowYourQr(false) },
                    sheetState = sheetState,
                    containerColor = colorScheme.surfaceContainerLow,
                    scrimColor = Color.Black.copy(alpha = 0.65f)
                ) {
                    YourQrSheetContent(
                        handle = uiState.currentUserHandle,
                        userId = uiState.currentUserId,
                        userKey = uiState.userKey,
                        onDismiss = { viewModel.setShowYourQr(false) }
                    )
                }
            }

            // QR SCANNER SCREEN OVERLAY
            if (uiState.showScanner) {
                Box(modifier = Modifier.fillMaxSize().zIndex(20f)) {
                    QrScannerScreen(
                        onNavigateBack = { viewModel.setShowScanner(false) },
                        onQrDetected = { detected ->
                            viewModel.onQrScanned(detected)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionRowItem(
    connection: ConnectionUser,
    onClickChat: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val relativeTime = remember(connection.connectedAt) {
        val now = System.currentTimeMillis()
        val diff = (now - connection.connectedAt).coerceAtLeast(0L)
        if (diff < 60_000L) {
            "Just now"
        } else {
            DateUtils.getRelativeTimeSpanString(
                connection.connectedAt,
                now,
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
            ).toString()
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceContainerLow)
            .border(1.dp, colorScheme.surfaceContainerHigh, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            // Squircle Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceContainer)
                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!connection.avatarUri.isNullOrBlank()) {
                    AsyncImage(
                        model = connection.avatarUri,
                        contentDescription = connection.displayName,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    val initial = connection.displayName.firstOrNull()?.uppercaseChar()?.toString()
                        ?: connection.username.firstOrNull()?.uppercaseChar()?.toString()
                        ?: "@"
                    Text(
                        text = initial,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.terracottaAccent
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = connection.displayName.ifBlank { "@${connection.username}" },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (connection.isPro) {
                        Text(
                            text = "PRO",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            color = colors.terracottaAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(colorScheme.surfaceContainer)
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
                Text(
                    text = "@${connection.username} • $relativeTime",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(
            onClick = onClickChat,
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colorScheme.surfaceContainer)
        ) {
            Icon(
                imageVector = Icons.Outlined.ChatBubbleOutline,
                contentDescription = "Message",
                tint = colors.terracottaAccent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun YourQrSheetContent(
    handle: String,
    userId: String,
    userKey: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val cleanHandle = handle.removePrefix("@")
    val qrPayload = if (userId.isNotBlank()) {
        "nura://user/$cleanHandle?id=$userId&key=$userKey"
    } else {
        "nura://user/$cleanHandle?key=$userKey"
    }
    val qrBitmap = remember(qrPayload) {
        QrCodeGenerator.generateQrBitmap(
            content = qrPayload,
            sizePx = 512,
            foregroundColorArgb = android.graphics.Color.BLACK,
            backgroundColorArgb = android.graphics.Color.WHITE
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Your Architectural Signature",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "Let someone scan this code to connect directly.",
            fontFamily = PlusJakartaSansFamily,
            fontSize = 13.sp,
            color = colors.subtitleText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(20.dp))

        // QR Code Box (Solid White for QR standards compliance)
        Box(
            modifier = Modifier
                .size(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            val bmp = qrBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "User QR Code",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = handle,
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 18.sp,
            color = colorScheme.onSurface
        )

        Text(
            text = "Key: $userKey",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 12.sp,
            color = colors.subtitleText
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colorScheme.surfaceContainerHigh,
                contentColor = colorScheme.onSurface
            )
        ) {
            Text(
                text = "Close",
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
