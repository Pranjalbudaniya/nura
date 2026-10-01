package com.nura.messaging.features.chat.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nura.messaging.core.common.ui.component.AvatarArchetypeCanvas
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.domain.entities.chat.ChatMessage
import com.nura.messaging.domain.entities.chat.MessageStatus
import com.nura.messaging.features.chat.presentation.viewmodel.ChatViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ChatScreen(
    conversationId: String,
    participantId: String,
    participantName: String,
    participantUsername: String,
    participantAvatarUrl: String? = null,
    participantAbout: String? = null,
    isRequest: Boolean = false,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    LaunchedEffect(conversationId, participantId) {
        viewModel.initChat(
            conversationId = conversationId,
            participantId = participantId,
            participantName = participantName,
            participantUsername = participantUsername,
            participantAvatarUrl = participantAvatarUrl,
            participantAbout = participantAbout,
            isRequest = isRequest
        )
    }

    ChatScreenContent(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    ChatScreenContent(
        viewModel = viewModel,
        onBack = onBack,
        modifier = modifier
    )
}

@Composable
private fun ChatScreenContent(
    viewModel: ChatViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()

    // Auto-scroll to bottom on new message
    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.size)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = colors.brandLogoText
                )
            }

            // Top Bar Avatar
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.badgeBackground)
                    .border(1.dp, colors.badgeBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isProfileShared && !uiState.participantAvatarUrl.isNullOrBlank()) {
                    val rawAvatar = uiState.participantAvatarUrl!!
                    if (rawAvatar.startsWith("preset:")) {
                        val idx = rawAvatar.removePrefix("preset:").toIntOrNull()
                        if (idx != null) {
                            AvatarArchetypeCanvas(
                                presetIndex = idx,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(4.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = rawAvatar,
                            contentDescription = uiState.participantName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = uiState.participantName.take(1).uppercase(Locale.getDefault()).ifEmpty { "?" },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = colors.terracottaAccent
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = uiState.participantName.ifEmpty { "Chat" },
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = colors.brandLogoText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = if (uiState.participantUsername.isNotBlank()) "@${uiState.participantUsername}" else "Encrypted relay",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                    color = colors.subtitleText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Security Badge
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.badgeBackground)
                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = colors.terracottaAccent,
                    modifier = Modifier.size(12.dp)
                )
                Text(
                    text = "E2EE",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    color = colors.brandLogoText
                )
            }
        }

        // Messages List & Square Profile Header Card
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = if (uiState.messages.isEmpty()) 32.dp else 16.dp
                ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Square profile card: prominent when empty and moves up as users chat
                item(key = "header_square_profile_card") {
                    SquareProfileHeaderCard(
                        name = uiState.participantName,
                        username = uiState.participantUsername,
                        about = uiState.participantAbout,
                        avatarUrl = uiState.participantAvatarUrl,
                        isProfileShared = uiState.isProfileShared,
                        modifier = Modifier.padding(bottom = if (uiState.messages.isEmpty()) 0.dp else 16.dp)
                    )
                }

                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        onRetry = { viewModel.retryMessage(message.id) }
                    )
                }
            }

            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = colors.terracottaAccent
                )
            }
        }

        // Message Request Accept/Reject Banner
        if (uiState.isRequest && !uiState.isAccepted) {
            MessageRequestBanner(
                participantName = uiState.participantName,
                onAccept = viewModel::acceptRequest,
                onReject = { viewModel.rejectRequest(onRejected = onBack) }
            )
        }

        // Bottom Input Bar (with stabilized height to prevent shrinking on typing first letter)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(colors.inputBackground)
                    .border(1.dp, colors.inputBorder, RoundedCornerShape(24.dp))
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputTextChanged,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 15.sp,
                        color = colors.brandLogoText,
                        lineHeight = 20.sp
                    ),
                    cursorBrush = SolidColor(colors.terracottaAccent),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (uiState.inputText.isEmpty()) {
                                Text(
                                    text = "Message...",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 15.sp,
                                    color = colors.subtitleText,
                                    lineHeight = 20.sp
                                )
                            }
                            innerTextField()
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.inputText.isNotBlank()) colors.terracottaAccent else colors.badgeBackground
                    )
                    .clickable(
                        enabled = uiState.inputText.isNotBlank() && !uiState.isSending,
                        onClick = viewModel::sendMessage
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = "Send",
                    tint = if (uiState.inputText.isNotBlank()) Color.White else colors.subtitleText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun SquareProfileHeaderCard(
    name: String,
    username: String,
    about: String,
    avatarUrl: String?,
    isProfileShared: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth(0.85f)
            .aspectRatio(1f)
            .clip(RoundedCornerShape(24.dp))
            .background(colors.cardDarkBubble)
            .border(1.dp, colors.badgeBorder, RoundedCornerShape(24.dp))
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Profile Picture
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(colors.badgeBackground)
                    .border(2.dp, colors.badgeBorder, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (isProfileShared && !avatarUrl.isNullOrBlank()) {
                    if (avatarUrl.startsWith("preset:")) {
                        val idx = avatarUrl.removePrefix("preset:").toIntOrNull()
                        if (idx != null) {
                            AvatarArchetypeCanvas(
                                presetIndex = idx,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(8.dp)
                            )
                        }
                    } else {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Text(
                        text = name.take(1).uppercase(Locale.getDefault()).ifEmpty { "?" },
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        color = colors.terracottaAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Display Name
            Text(
                text = name.ifEmpty { "Nura User" },
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.brandLogoText,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Username
            if (username.isNotBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "@$username",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = colors.subtitleText
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // About / Bio
            Text(
                text = about.ifBlank { "HI there i'm using nura" },
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                color = colors.subtitleText,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun MessageRequestBanner(
    participantName: String,
    onAccept: () -> Unit,
    onReject: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(colors.badgeBackground)
            .border(1.dp, colors.badgeBorder, RoundedCornerShape(20.dp))
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Message Request",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = colors.brandLogoText
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = "${participantName.ifEmpty { "This sender" }} wants to message you. Accept to share profile and reply, or reject to remove this chat.",
            fontFamily = PlusJakartaSansFamily,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = colors.subtitleText,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Reject Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                    .clickable(onClick = onReject),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Reject",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Accept Button
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.terracottaAccent)
                    .clickable(onClick = onAccept),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Accept",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    onRetry: () -> Unit
) {
    val colors = NuraTheme.colors
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isOutgoing) Arrangement.End else Arrangement.Start
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 280.dp)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                    )
                )
                .background(
                    if (message.isOutgoing) colors.cardDarkBubble else colors.badgeBackground
                )
                .border(
                    width = 1.dp,
                    color = colors.badgeBorder,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = message.content,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp,
                color = colors.brandLogoText,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.align(Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = formattedTime,
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 10.sp,
                    color = colors.subtitleText
                )

                if (message.isOutgoing) {
                    when (message.status) {
                        MessageStatus.PENDING -> {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = "Sending",
                                tint = colors.subtitleText,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        MessageStatus.SENT -> {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Sent",
                                tint = colors.subtitleText,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                        MessageStatus.DELIVERED -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Delivered",
                                tint = colors.terracottaAccent,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.READ -> {
                            Icon(
                                imageVector = Icons.Default.DoneAll,
                                contentDescription = "Read",
                                tint = colors.brandLogoText,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                        MessageStatus.FAILED -> {
                            Icon(
                                imageVector = Icons.Default.ErrorOutline,
                                contentDescription = "Failed - tap to retry",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { onRetry() }
                            )
                        }
                    }
                }
            }
        }
    }
}
