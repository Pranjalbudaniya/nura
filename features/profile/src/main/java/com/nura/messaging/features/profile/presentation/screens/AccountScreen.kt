package com.nura.messaging.features.profile.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.nura.messaging.core.common.ui.component.AvatarArchetypeCanvas
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.domain.entities.auth.AuthUser
import com.nura.messaging.features.auth.presentation.screens.CropImageDialog
import java.io.File
import java.io.FileOutputStream

@Composable
fun AccountScreen(
    currentUser: AuthUser?,
    profilePictureUri: String?,
    selectedPresetIndex: Int?,
    selectedPresetColor: Long?,
    onUpdateProfilePicture: (String?) -> Unit,
    onSaveProfile: (name: String, about: String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    var nameState by rememberSaveable(currentUser) {
        mutableStateOf(currentUser?.name.orEmpty().ifBlank { "User" })
    }
    var aboutState by rememberSaveable(currentUser) {
        mutableStateOf(currentUser?.about?.ifBlank { "HI there i'm using nura" } ?: "HI there i'm using nura")
    }
    var isSavedConfirmation by remember { mutableStateOf(false) }

    // Instant local avatar state
    var localAvatarUri by rememberSaveable(profilePictureUri) {
        mutableStateOf(profilePictureUri)
    }

    // Cropping & Camera States
    var imageToCropUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let { imageToCropUriString = it.toString() }
    }

    // Clean, robust camera launcher
    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = tempCameraFilePath
        val file = if (!path.isNullOrBlank()) File(path) else null
        val fileHasData = file != null && file.exists() && file.length() > 0L

        if (success || fileHasData) {
            if (file != null && file.exists() && file.length() > 0L) {
                imageToCropUriString = Uri.fromFile(file).toString()
            } else if (!tempCameraUriString.isNullOrBlank()) {
                imageToCropUriString = tempCameraUriString
            }
        } else {
            Toast.makeText(context, "No photo captured. Please try again.", Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraDirectly = {
        try {
            val cameraDir = File(context.cacheDir, "camera_photos").apply { mkdirs() }
            val photoFile = File(cameraDir, "avatar_acc_${System.currentTimeMillis()}.jpg")
            if (photoFile.exists()) {
                photoFile.delete()
            }
            photoFile.createNewFile()
            tempCameraFilePath = photoFile.absolutePath
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                photoFile
            )
            tempCameraUriString = uri.toString()
            takePictureLauncher.launch(uri)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open camera: ${e.localizedMessage ?: "Unknown error"}", Toast.LENGTH_SHORT).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            launchCameraDirectly()
        } else {
            Toast.makeText(context, "Camera permission is required to take a profile photo", Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            // Header Bar: ZERO top gap above header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Go back",
                        tint = colors.brandLogoText,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "Edit Profile",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colorScheme.onSurface,
                    letterSpacing = (-0.01).sp
                )
            }

            // Spatial Status Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(colors.terracottaAccent, CircleShape)
                    )
                    Text(
                        text = "ARCHITECTURAL VAULT",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = colors.subtitleText
                    )
                }

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(colors.badgeBackground)
                        .border(1.dp, colors.badgeBorder, CircleShape)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CloudDone,
                        contentDescription = null,
                        tint = colors.terracottaAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = "Synced",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 11.sp,
                        color = colors.subtitleText
                    )
                }
            }

            // Profile Picture Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // 120dp Squircle Avatar Container
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(colors.badgeBackground)
                        .border(1.dp, colors.badgeBorder, RoundedCornerShape(28.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !localAvatarUri.isNullOrBlank() -> {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(localAvatarUri)
                                    .memoryCachePolicy(CachePolicy.DISABLED)
                                    .diskCachePolicy(CachePolicy.DISABLED)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile picture",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(28.dp))
                            )
                        }
                        selectedPresetIndex != null -> {
                            AvatarArchetypeCanvas(
                                presetIndex = selectedPresetIndex,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        else -> {
                            val initial = currentUser?.name?.take(1)?.uppercase()
                                ?: currentUser?.username?.take(1)?.uppercase()
                                ?: "U"
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(colors.cardDarkBubble),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = initial,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 42.sp,
                                    color = colors.terracottaAccent
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = nameState.ifBlank { "User" },
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 20.sp,
                    color = colorScheme.onSurface
                )

                Text(
                    text = if (!currentUser?.username.isNullOrBlank()) "@${currentUser?.username}" else "Nura Resident",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 14.sp,
                    color = colors.subtitleText,
                    modifier = Modifier.padding(top = 2.dp)
                )

                // Compact Action Pills: Change / Take / Remove
                Row(
                    modifier = Modifier
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Change Pill (Gallery)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Change",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                    }

                    // Take Pill (Camera)
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                            .clickable {
                                val hasPermission = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.CAMERA
                                ) == PackageManager.PERMISSION_GRANTED
                                if (hasPermission) {
                                    launchCameraDirectly()
                                } else {
                                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                }
                            }
                            .padding(horizontal = 14.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = "Take",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colorScheme.onSurface
                        )
                    }

                    // Remove Pill
                    if (!localAvatarUri.isNullOrBlank()) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .clickable {
                                    localAvatarUri = null
                                    onUpdateProfilePicture(null)
                                }
                                .padding(horizontal = 10.dp, vertical = 7.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Remove",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = colorScheme.error
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Form Sections
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Section 1: Personal Information
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colors.terracottaAccent, CircleShape)
                        )
                        Text(
                            text = "Personal information",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colorScheme.onSurface,
                            letterSpacing = (-0.01).sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(18.dp))
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // Name Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "NAME",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colors.subtitleText
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.cardDarkBubble)
                                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 12.dp)
                            ) {
                                BasicTextField(
                                    value = nameState,
                                    onValueChange = { nameState = it },
                                    singleLine = true,
                                    textStyle = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 15.sp,
                                        color = colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(colors.terracottaAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }

                        // About Field
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "ABOUT",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    letterSpacing = 1.sp,
                                    color = colors.subtitleText
                                )
                                Text(
                                    text = "${aboutState.length} / 120",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (aboutState.length > 110) colorScheme.error else colors.subtitleText
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(76.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colors.cardDarkBubble)
                                    .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                            ) {
                                BasicTextField(
                                    value = aboutState,
                                    onValueChange = {
                                        if (it.length <= 120) {
                                            aboutState = it
                                        }
                                    },
                                    textStyle = TextStyle(
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        lineHeight = 20.sp,
                                        color = colorScheme.onSurface
                                    ),
                                    cursorBrush = SolidColor(colors.terracottaAccent),
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }

                // Section 2: Account Details
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .background(colors.subtitleText, CircleShape)
                        )
                        Text(
                            text = "Account details",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colorScheme.onSurface,
                            letterSpacing = (-0.01).sp
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(18.dp))
                    ) {
                        // Email Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Email address",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = currentUser?.email.orEmpty().ifBlank { "Unverified account" },
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = colorScheme.onSurface
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .background(colors.terracottaAccent, CircleShape)
                                    )
                                }
                            }
                            Text(
                                text = "Verified",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = colors.terracottaAccent
                            )
                        }

                        HorizontalDivider(
                            color = colors.badgeBorder,
                            thickness = 1.dp
                        )

                        // Username / Nura Key Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Nura Username",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = if (!currentUser?.username.isNullOrBlank()) "@${currentUser?.username}" else "@user",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                            }
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Nura Username", "@${currentUser?.username.orEmpty()}")
                                    clipboard.setPrimaryClip(clip)
                                    Toast.makeText(context, "Username copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "Copy username",
                                    tint = colors.subtitleText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = colors.badgeBorder,
                            thickness = 1.dp
                        )

                        // Registration Timestamp Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Vault Status",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "End-to-End Encrypted Identity",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 14.sp,
                                    color = colors.subtitleText
                                )
                            }
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = colors.subtitleText,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Save Action Button
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Button(
                        onClick = {
                            onSaveProfile(nameState, aboutState)
                            isSavedConfirmation = true
                            Toast.makeText(context, "Profile changes saved securely.", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.terracottaAccent,
                            contentColor = Color.White
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = if (isSavedConfirmation) Icons.Outlined.Check else Icons.Outlined.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isSavedConfirmation) "Changes Saved" else "Save changes",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sync,
                            contentDescription = null,
                            tint = colors.subtitleText,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "Edits are preserved securely on this device.",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            color = colors.subtitleText
                        )
                    }
                }
            }
        }

        // Crop Dialog Overlay
        val targetCropUri = imageToCropUriString?.let { Uri.parse(it) }
        if (targetCropUri != null) {
            CropImageDialog(
                sourceUri = targetCropUri,
                onDismiss = { imageToCropUriString = null },
                onCropConfirmed = { croppedUri ->
                    imageToCropUriString = null
                    localAvatarUri = croppedUri.toString()
                    onUpdateProfilePicture(croppedUri.toString())
                }
            )
        }
    }
}
