package com.nura.messaging.features.auth.presentation.screens

import android.Manifest
import android.app.Activity
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nura.messaging.core.common.ui.component.AvatarArchetypeCanvas
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.features.auth.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilePictureScreen(
    viewModel: AuthViewModel,
    onNavigateBack: () -> Unit,
    onContinue: () -> Unit,
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    var imageToCropUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraUriString by rememberSaveable { mutableStateOf<String?>(null) }
    var tempCameraFilePath by rememberSaveable { mutableStateOf<String?>(null) }
    var showDefaultsDrawer by remember { mutableStateOf(false) }

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
            val photoFile = File(cameraDir, "camera_avatar_${System.currentTimeMillis()}.jpg")
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
            Toast.makeText(context, "Camera access is needed to take a profile photo", Toast.LENGTH_SHORT).show()
        }
    }

    val colorPresets = remember(colors, colorScheme) {
        listOf(
            colors.terracottaAccent,
            colorScheme.secondary,
            colorScheme.outline,
            colorScheme.secondaryContainer
        )
    }
    var currentColorIndex by remember { mutableIntStateOf(0) }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
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
                // Top Header: Back Button & Step Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go back",
                            tint = colors.brandLogoText,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Centered Step Badge: YOUR PROFILE • 1 of 2
                    Row(
                        modifier = Modifier
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "YOUR PROFILE",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )
                        Box(
                            modifier = Modifier
                                .size(4.dp)
                                .background(colors.terracottaAccent, CircleShape)
                        )
                        Text(
                            text = "1 of 2",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = colors.brandLogoText
                        )
                    }

                    // Spacer for optical symmetry
                    Spacer(modifier = Modifier.size(40.dp))
                }

                // Central Avatar Elevation Zone
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(136.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // Ambient glow backdrop
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            colors.terracottaAccent.copy(alpha = 0.2f),
                                            Color.Transparent
                                        )
                                    ),
                                    shape = RoundedCornerShape(36.dp)
                                )
                        )

                        // Main Squircle Avatar Canvas
                        Box(
                            modifier = Modifier
                                .size(128.dp)
                                .clip(RoundedCornerShape(32.dp))
                                .background(colors.badgeBackground)
                                .border(1.dp, colors.badgeBorder, RoundedCornerShape(32.dp))
                                .clickable {
                                    photoPickerLauncher.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            when {
                                uiState.profilePictureUri != null -> {
                                    val imageModel = remember(uiState.profilePictureUri) {
                                        val uri = uiState.profilePictureUri!!
                                        if (uri.startsWith("data:")) {
                                            val base64 = uri.substringAfter(",")
                                            try {
                                                android.util.Base64.decode(base64, android.util.Base64.DEFAULT)
                                            } catch (_: Exception) {
                                                uri
                                            }
                                        } else {
                                            uri
                                        }
                                    }
                                    AsyncImage(
                                        model = imageModel,
                                        contentDescription = "Profile picture",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                                uiState.selectedPresetIndex != null -> {
                                    AvatarArchetypeCanvas(
                                        presetIndex = uiState.selectedPresetIndex ?: 0,
                                        modifier = Modifier.size(76.dp)
                                    )
                                }
                                uiState.selectedPresetColor != null -> {
                                    val selectedColor = colorPresets.getOrElse((uiState.selectedPresetColor ?: 0L).toInt()) { colors.terracottaAccent }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .background(selectedColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = uiState.name.firstOrNull()?.uppercase() ?: "@",
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 44.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                else -> {
                                    // Default: Terracotta Concentric Ring
                                    AvatarArchetypeCanvas(
                                        presetIndex = 1,
                                        modifier = Modifier.size(76.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Text(
                        text = "Add a profile picture",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        lineHeight = 32.sp,
                        letterSpacing = (-0.02).sp,
                        color = colors.brandLogoText,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Give people a way to recognize you across conversations.",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.subtitleText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                // Mid-level Tactile Source Triggers
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Trigger 1: Choose from gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(16.dp))
                            .clickable {
                                photoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(colors.cardDarkBubble, RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = null,
                                tint = colors.terracottaAccent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Text(
                            text = "Choose from gallery",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = colors.brandLogoText
                        )
                    }

                    // Trigger 2: Take a photo
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(16.dp))
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
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(colors.cardDarkBubble, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CameraAlt,
                                    contentDescription = null,
                                    tint = colors.subtitleText,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "Take a photo",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = colors.brandLogoText
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.AddAPhoto,
                            contentDescription = null,
                            tint = colors.subtitleText.copy(alpha = 0.6f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Architectural Spacer with Micro Divider
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 48.dp, height = 2.dp)
                            .background(colors.badgeBorder, CircleShape)
                    )
                }

                // Ambient Preset Defaults Mosaic
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 2.dp, end = 2.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DEFAULTS",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )
                        Text(
                            text = "All",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = colors.terracottaAccent,
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable { showDefaultsDrawer = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Preset 0: Minimalist Slate Architecture
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.badgeBackground)
                                .border(
                                    width = if (uiState.selectedPresetIndex == 0) 2.dp else 1.dp,
                                    color = if (uiState.selectedPresetIndex == 0) colors.terracottaAccent else colors.badgeBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.onPresetAvatarSelected(0) }
                        ) {
                            PresetAvatarArt(index = 0, modifier = Modifier.fillMaxSize())
                            if (uiState.selectedPresetIndex == 0) {
                                SelectedCheckOverlay()
                            }
                        }

                        // Preset 1: Abstract Terracotta Ceramic
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.badgeBackground)
                                .border(
                                    width = if (uiState.selectedPresetIndex == 1) 2.dp else 1.dp,
                                    color = if (uiState.selectedPresetIndex == 1) colors.terracottaAccent else colors.badgeBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.onPresetAvatarSelected(1) }
                        ) {
                            PresetAvatarArt(index = 1, modifier = Modifier.fillMaxSize())
                            if (uiState.selectedPresetIndex == 1) {
                                SelectedCheckOverlay()
                            }
                        }

                        // Preset 2: Botanical Charred Cedar
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.badgeBackground)
                                .border(
                                    width = if (uiState.selectedPresetIndex == 2) 2.dp else 1.dp,
                                    color = if (uiState.selectedPresetIndex == 2) colors.terracottaAccent else colors.badgeBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable { viewModel.onPresetAvatarSelected(2) }
                        ) {
                            PresetAvatarArt(index = 2, modifier = Modifier.fillMaxSize())
                            if (uiState.selectedPresetIndex == 2) {
                                SelectedCheckOverlay()
                            }
                        }

                        // Preset 3: Color Archetype
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.badgeBackground)
                                .border(
                                    width = if (uiState.selectedPresetColor != null) 2.dp else 1.dp,
                                    color = if (uiState.selectedPresetColor != null) colors.terracottaAccent else colors.badgeBorder,
                                    shape = RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    currentColorIndex = (currentColorIndex + 1) % colorPresets.size
                                    viewModel.onPresetColorSelected(currentColorIndex.toLong())
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Palette,
                                    contentDescription = "Color archetype",
                                    tint = if (uiState.selectedPresetColor != null) colors.terracottaAccent else colors.subtitleText,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Color",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = if (uiState.selectedPresetColor != null) colors.brandLogoText else colors.subtitleText
                                )
                            }
                            if (uiState.selectedPresetColor != null) {
                                SelectedCheckOverlay()
                            }
                        }
                    }
                }
            }

            // Bottom Actions: Continue & Skip for now
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 28.dp, bottom = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onContinue,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
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
                        Text(
                            text = "Continue",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            letterSpacing = 0.01.sp
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Skip for now",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.subtitleText,
                    modifier = Modifier
                        .clickable { onSkip() }
                        .padding(vertical = 8.dp, horizontal = 16.dp)
                )
            }
        }

        // 36 Defaults Drawer
        if (showDefaultsDrawer) {
            AvatarPresetsDrawer(
                selectedIndex = uiState.selectedPresetIndex,
                onSelectPreset = { index ->
                    viewModel.onPresetAvatarSelected(index)
                },
                onDismiss = { showDefaultsDrawer = false }
            )
        }
    }

    // Image Cropping Screen Overlay
    val targetCropUri = imageToCropUriString?.let { Uri.parse(it) }
    if (targetCropUri != null) {
        CropImageDialog(
            sourceUri = targetCropUri,
            onDismiss = { imageToCropUriString = null },
            onCropConfirmed = { croppedUri ->
                imageToCropUriString = null
                viewModel.onProfilePictureSelected(croppedUri.toString())
            }
        )
    }
}
}

@Composable
private fun SelectedCheckOverlay() {
    val colors = NuraTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.brandLogoText.copy(alpha = 0.25f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .background(colors.terracottaAccent, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@Composable
private fun PresetAvatarArt(
    index: Int,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        when (index) {
            0 -> {
                // Preset 0: Minimalist Architectural Facade
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(colorScheme.surfaceContainerLow, colorScheme.surfaceContainerHighest),
                        start = Offset.Zero,
                        end = Offset(w, h)
                    )
                )
                // Angled golden aperture beam
                drawRect(
                    color = colors.terracottaAccent.copy(alpha = 0.85f),
                    topLeft = Offset(w * 0.45f, h * 0.25f),
                    size = androidx.compose.ui.geometry.Size(w * 0.18f, h * 0.5f)
                )
                drawRect(
                    color = colorScheme.primaryContainer,
                    topLeft = Offset(w * 0.52f, h * 0.35f),
                    size = androidx.compose.ui.geometry.Size(w * 0.11f, h * 0.4f)
                )
            }
            1 -> {
                // Preset 1: Abstract Terracotta Ceramic Form
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(colorScheme.surfaceVariant, colorScheme.surfaceContainerLow),
                        center = Offset(w * 0.5f, h * 0.5f),
                        radius = w * 0.7f
                    )
                )
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(colors.terracottaAccent, colorScheme.primary),
                        center = Offset(w * 0.5f, h * 0.48f),
                        radius = w * 0.3f
                    ),
                    radius = w * 0.28f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
                drawCircle(
                    color = colorScheme.surfaceContainerLowest,
                    radius = w * 0.12f,
                    center = Offset(w * 0.5f, h * 0.5f)
                )
            }
            else -> {
                // Preset 2: Botanical Charred Cedar Motif
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(colorScheme.surfaceContainerHigh, colorScheme.surface),
                        start = Offset(0f, h),
                        end = Offset(w, 0f)
                    )
                )
                // Minimalist botanical branch silhouette
                drawLine(
                    color = colorScheme.outline,
                    start = Offset(w * 0.2f, h * 0.8f),
                    end = Offset(w * 0.8f, h * 0.2f),
                    strokeWidth = 4f
                )
                drawLine(
                    color = colors.terracottaAccent,
                    start = Offset(w * 0.4f, h * 0.6f),
                    end = Offset(w * 0.6f, h * 0.45f),
                    strokeWidth = 3f
                )
                drawLine(
                    color = colorScheme.outlineVariant,
                    start = Offset(w * 0.55f, h * 0.45f),
                    end = Offset(w * 0.75f, h * 0.35f),
                    strokeWidth = 2.5f
                )
            }
        }
    }
}
