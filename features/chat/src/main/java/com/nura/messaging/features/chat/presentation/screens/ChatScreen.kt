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
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Keyboard
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.SentimentSatisfiedAlt
import androidx.compose.material.icons.outlined.Videocam
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.core.content.ContextCompat
import com.nura.messaging.features.chat.presentation.components.ChatImageCropDialog
import com.nura.messaging.features.chat.presentation.components.EmojiPickerSection
import com.nura.messaging.features.chat.presentation.components.InAppMediaViewer
import com.nura.messaging.features.chat.presentation.util.AudioPlayerHelper
import com.nura.messaging.features.chat.presentation.util.AudioRecorderHelper
import com.nura.messaging.features.chat.presentation.util.PlaybackState
import java.io.File

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

private data class ViewingMediaItem(
    val mediaUrl: String,
    val mediaType: String,
    val senderName: String,
    val timestamp: Long
)

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

@OptIn(ExperimentalMaterial3Api::class)
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
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val audioRecorder = remember { AudioRecorderHelper(context) }
    val audioPlayer = remember { AudioPlayerHelper() }
    val playbackState by audioPlayer.playbackState.collectAsStateWithLifecycle()

    var showAttachmentSheet by remember { mutableStateOf(false) }
    var pendingCropImageUri by remember { mutableStateOf<Uri?>(null) }
    var viewingMediaItem by remember { mutableStateOf<ViewingMediaItem?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            audioRecorder.release()
            audioPlayer.release()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            pendingCropImageUri = uri
        }
    }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { viewModel.sendVideoUri(context, it) }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val audioFile = audioRecorder.startRecording()
            if (audioFile != null) {
                viewModel.startRecordingAudio()
            }
        }
    }

    val startRecordingAction = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val audioFile = audioRecorder.startRecording()
            if (audioFile != null) {
                viewModel.startRecordingAudio()
            }
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val cancelRecordingAction = {
        audioRecorder.cancelRecording()
        viewModel.cancelRecordingAudio()
    }

    val sendVoiceNoteAction = {
        val duration = uiState.recordingDurationSeconds
        val audioFile = audioRecorder.stopRecording()
        if (audioFile != null && duration > 0) {
            viewModel.sendVoiceNote(audioFile, duration)
        } else {
            viewModel.cancelRecordingAudio()
        }
    }

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
                if (uiState.isAccepted && !uiState.participantAvatarUrl.isNullOrBlank()) {
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
                    } else if (rawAvatar.startsWith("color:")) {
                        val colorLong = rawAvatar.removePrefix("color:").toLongOrNull()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (colorLong != null) Color(colorLong.toULong()) else colors.badgeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = uiState.participantName.take(1).uppercase(Locale.getDefault()).ifEmpty { "?" },
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        val imageModel = remember(rawAvatar) {
                            if (rawAvatar.startsWith("data:")) {
                                val base64 = rawAvatar.substringAfter(",")
                                try {
                                    android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                } catch (_: Exception) {
                                    rawAvatar
                                }
                            } else {
                                rawAvatar
                            }
                        }
                        AsyncImage(
                            model = imageModel,
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
                        isAccepted = uiState.isAccepted,
                        modifier = Modifier.padding(bottom = if (uiState.messages.isEmpty()) 0.dp else 16.dp)
                    )
                }

                items(uiState.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        playbackState = playbackState,
                        participantName = uiState.participantName,
                        onPlayAudio = { id, url -> audioPlayer.togglePlayPause(id, url) },
                        onMediaClick = { url, type, sender, ts ->
                            viewingMediaItem = ViewingMediaItem(
                                mediaUrl = url,
                                mediaType = type,
                                senderName = sender,
                                timestamp = ts
                            )
                        },
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

        // Media Uploading Banner
        AnimatedVisibility(visible = uiState.isUploadingMedia) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.badgeBackground)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    color = colors.terracottaAccent,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Uploading media...",
                    fontFamily = PlusJakartaSansFamily,
                    fontSize = 12.sp,
                    color = colors.subtitleText
                )
            }
        }

        // Message Request Accept/Reject Banner or Bottom Input Bar
        if (uiState.isRequest && !uiState.isAccepted) {
            MessageRequestBanner(
                participantName = uiState.participantName,
                onAccept = viewModel::acceptRequest,
                onReject = { viewModel.rejectRequest(onRejected = onBack) }
            )
        } else {
            if (uiState.isRecordingAudio) {
                // Audio recording bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = cancelRecordingAction,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.badgeBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Cancel Recording",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.inputBackground)
                            .border(1.dp, colors.inputBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(CircleShape)
                                .background(colors.terracottaAccent)
                        )
                        val mins = uiState.recordingDurationSeconds / 60
                        val secs = uiState.recordingDurationSeconds % 60
                        Text(
                            text = String.format(Locale.getDefault(), "%02d:%02d", mins, secs),
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = colors.brandLogoText
                        )
                        Text(
                            text = "Recording...",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            color = colors.subtitleText
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.terracottaAccent)
                            .clickable(onClick = sendVoiceNoteAction),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "Send Voice Note",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            } else {
                // Bottom Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(colors.inputBackground)
                            .border(1.dp, colors.inputBorder, RoundedCornerShape(24.dp))
                            .padding(horizontal = 6.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Emoji Picker toggle button
                        IconButton(
                            onClick = {
                                if (!uiState.isEmojiPickerVisible) {
                                    keyboardController?.hide()
                                }
                                viewModel.toggleEmojiPicker()
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (uiState.isEmojiPickerVisible) Icons.Outlined.Keyboard else Icons.Outlined.SentimentSatisfiedAlt,
                                contentDescription = "Emoji Picker",
                                tint = if (uiState.isEmojiPickerVisible) colors.terracottaAccent else colors.subtitleText,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        BasicTextField(
                            value = uiState.inputText,
                            onValueChange = {
                                if (uiState.isEmojiPickerVisible) {
                                    viewModel.setEmojiPickerVisible(false)
                                }
                                viewModel.onInputTextChanged(it)
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 15.sp,
                                color = colors.brandLogoText,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(colors.terracottaAccent),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 4.dp),
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

                        // Attachment button
                        IconButton(
                            onClick = {
                                if (uiState.isEmojiPickerVisible) {
                                    viewModel.setEmojiPickerVisible(false)
                                }
                                showAttachmentSheet = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = "Attach Media",
                                tint = colors.subtitleText,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    // Dynamic Send / Mic Button
                    if (uiState.inputText.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.terracottaAccent)
                                .clickable(
                                    enabled = !uiState.isSending,
                                    onClick = {
                                        if (uiState.isEmojiPickerVisible) {
                                            viewModel.setEmojiPickerVisible(false)
                                        }
                                        viewModel.sendMessage()
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(colors.terracottaAccent)
                                .clickable(onClick = startRecordingAction),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Mic,
                                contentDescription = "Record Voice Note",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }

        // WhatsApp-style Google Emoji Picker
        AnimatedVisibility(visible = uiState.isEmojiPickerVisible) {
            EmojiPickerSection(
                onEmojiSelected = viewModel::onEmojiSelected,
                onBackspace = viewModel::onEmojiBackspace,
                modifier = Modifier.height(260.dp)
            )
        }
    }

    if (showAttachmentSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAttachmentSheet = false },
            containerColor = colors.cardDarkBubble,
            contentColor = colors.brandLogoText
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Share media",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.brandLogoText
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.badgeBackground)
                        .clickable {
                            showAttachmentSheet = false
                            photoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.terracottaAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = "Photo",
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Photo",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.brandLogoText
                        )
                        Text(
                            text = "Send an image in full quality",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            color = colors.subtitleText
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.badgeBackground)
                        .clickable {
                            showAttachmentSheet = false
                            videoPickerLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
                            )
                        }
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.terracottaAccent.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Videocam,
                            contentDescription = "Video",
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Video (Max 25 MB)",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.brandLogoText
                        )
                        Text(
                            text = "Send a video up to 25 MB",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 12.sp,
                            color = colors.subtitleText
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (pendingCropImageUri != null) {
        ChatImageCropDialog(
            sourceUri = pendingCropImageUri!!,
            onDismiss = { pendingCropImageUri = null },
            onCropConfirmed = { croppedBytes ->
                viewModel.sendPhoto(croppedBytes)
                pendingCropImageUri = null
            }
        )
    }

    if (viewingMediaItem != null) {
        val media = viewingMediaItem!!
        InAppMediaViewer(
            mediaUrl = media.mediaUrl,
            mediaType = media.mediaType,
            senderName = media.senderName,
            timestamp = media.timestamp,
            onDismiss = { viewingMediaItem = null }
        )
    }
}

@Composable
private fun SquareProfileHeaderCard(
    name: String,
    username: String,
    about: String,
    avatarUrl: String?,
    isProfileShared: Boolean,
    isAccepted: Boolean,
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
                if (isAccepted && !avatarUrl.isNullOrBlank()) {


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
                    } else if (avatarUrl.startsWith("color:")) {
                        val colorLong = avatarUrl.removePrefix("color:").toLongOrNull()
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(if (colorLong != null) Color(colorLong.toULong()) else colors.badgeBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = name.take(1).uppercase(Locale.getDefault()).ifEmpty { "?" },
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp,
                                color = Color.White
                            )
                        }
                    } else {
                        val imageModel = remember(avatarUrl) {
                            if (avatarUrl.startsWith("data:")) {
                                val base64 = avatarUrl.substringAfter(",")
                                try {
                                    android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                } catch (_: Exception) {
                                    avatarUrl
                                }
                            } else {
                                avatarUrl
                            }
                        }
                        AsyncImage(
                            model = imageModel,
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
    playbackState: PlaybackState,
    participantName: String,
    onPlayAudio: (messageId: String, audioUrl: String) -> Unit,
    onMediaClick: (mediaUrl: String, mediaType: String, senderName: String, timestamp: Long) -> Unit,
    onRetry: () -> Unit
) {
    val colors = NuraTheme.colors
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))
    val senderName = if (message.isOutgoing) "You" else participantName.ifBlank { "User" }

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
                    if (message.isOutgoing) colors.cardDarkBubble else colors.terracottaAccent
                )
                .border(
                    width = 1.dp,
                    color = if (message.isOutgoing) colors.badgeBorder else colors.terracottaAccent,
                    shape = RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (message.isOutgoing) 16.dp else 4.dp,
                        bottomEnd = if (message.isOutgoing) 4.dp else 16.dp
                    )
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            when (message.messageType) {
                "image" -> {
                    AsyncImage(
                        model = message.content,
                        contentDescription = "Image message",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                onMediaClick(message.content, "image", senderName, message.timestamp)
                            }
                    )
                }
                "video" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable {
                                onMediaClick(message.content, "video", senderName, message.timestamp)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PlayArrow,
                                contentDescription = "Play video",
                                tint = Color.White,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            text = "▶ Video",
                            fontFamily = PlusJakartaSansFamily,
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.8f),
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(8.dp)
                        )
                    }
                }
                "audio" -> {
                    val isCurrentAudio = playbackState.playingMessageId == message.id
                    val isPlaying = isCurrentAudio && playbackState.isPlaying
                    val progress = if (isCurrentAudio && playbackState.durationMs > 0) {
                        (playbackState.currentPositionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
                    } else 0f

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (message.isOutgoing) colors.terracottaAccent else Color.White)
                                .clickable { onPlayAudio(message.id, message.content) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = if (message.isOutgoing) Color.White else colors.terracottaAccent,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = if (message.isOutgoing) colors.terracottaAccent else Color.White,
                                trackColor = if (message.isOutgoing) colors.badgeBorder else Color.White.copy(alpha = 0.3f)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            val timeText = if (isCurrentAudio && playbackState.durationMs > 0) {
                                val curSecs = playbackState.currentPositionMs / 1000
                                val totSecs = playbackState.durationMs / 1000
                                String.format(Locale.getDefault(), "%02d:%02d / %02d:%02d", curSecs / 60, curSecs % 60, totSecs / 60, totSecs % 60)
                            } else {
                                "Voice Note"
                            }
                            Text(
                                text = timeText,
                                fontFamily = PlusJakartaSansFamily,
                                fontSize = 10.sp,
                                color = if (message.isOutgoing) colors.subtitleText else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
                else -> {
                    Text(
                        text = message.content,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        color = if (message.isOutgoing) colors.brandLogoText else Color.White,
                        lineHeight = 20.sp
                    )
                }
            }

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
                    color = if (message.isOutgoing) colors.subtitleText else Color.White.copy(alpha = 0.8f)
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
