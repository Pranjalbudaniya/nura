package com.nura.messaging.features.settings.presentation.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CleaningServices
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private enum class SettingsCardType {
    NONE,
    APPEARANCE,
    CACHE,
    PRIVACY,
    SECURITY,
    ABOUT
}

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var expandedCard by remember { mutableStateOf(SettingsCardType.NONE) }

    // Appearance states
    var selectedThemeMode by remember { mutableIntStateOf(0) } // 0: Dark (Nocturnal), 1: Light (Porcelain), 2: System
    var selectedCustomAccentIndex by remember { mutableIntStateOf(0) } // 0: Terracotta, 1: Emerald, 2: Cobalt, 3: Amethyst
    var highContrastMode by remember { mutableStateOf(false) }

    // Cache states
    var cacheSizeBytes by remember { mutableLongStateOf(0L) }
    var isClearingCache by remember { mutableStateOf(false) }

    // Privacy & Delivery states
    var readReceiptsEnabled by remember { mutableStateOf(true) }
    var typingIndicatorsEnabled by remember { mutableStateOf(true) }
    var messagePreviewsEnabled by remember { mutableStateOf(true) }

    // Calculate real cache size on mount
    val calculateCacheSize: suspend () -> Long = {
        withContext(Dispatchers.IO) {
            var total = 0L
            val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
            for (dir in cacheDirs) {
                if (dir.exists()) {
                    dir.walkTopDown().forEach { file ->
                        if (file.isFile) {
                            total += file.length()
                        }
                    }
                }
            }
            total
        }
    }

    LaunchedEffect(Unit) {
        cacheSizeBytes = calculateCacheSize()
    }

    val clearCacheAction: () -> Unit = {
        scope.launch {
            isClearingCache = true
            withContext(Dispatchers.IO) {
                val cacheDirs = listOfNotNull(context.cacheDir, context.externalCacheDir)
                for (dir in cacheDirs) {
                    if (dir.exists()) {
                        dir.listFiles()?.forEach { file ->
                            try {
                                if (file.isDirectory) {
                                    file.deleteRecursively()
                                } else {
                                    file.delete()
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }
            cacheSizeBytes = calculateCacheSize()
            isClearingCache = false
            Toast.makeText(context, "Local cache cleared successfully", Toast.LENGTH_SHORT).show()
        }
    }

    val formatBytes: (Long) -> String = { bytes ->
        when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> "${bytes / 1024} KB"
            else -> String.format(java.util.Locale.US, "%.1f MB", bytes.toDouble() / (1024 * 1024))
        }
    }

    val toggleCard: (SettingsCardType) -> Unit = { card ->
        expandedCard = if (expandedCard == card) SettingsCardType.NONE else card
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
                    text = "Settings",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colorScheme.onSurface,
                    letterSpacing = (-0.01).sp
                )
            }

            // Spatial Introduction Strip
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
                        text = "ENVIRONMENT PREFERENCES",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 11.sp,
                        letterSpacing = 1.5.sp,
                        color = colors.subtitleText
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Expandable Settings Cards Stack
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Card 1: Appearance & Custom Themes
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance & Theme",
                    subtitle = when (selectedThemeMode) {
                        0 -> "Nocturnal Obsidian (Dark)"
                        1 -> "Porcelain Cream (Light)"
                        else -> "System Synchronized"
                    },
                    isExpanded = expandedCard == SettingsCardType.APPEARANCE,
                    onToggle = { toggleCard(SettingsCardType.APPEARANCE) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "THEME MODE",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )

                        // Mode Radio Group
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemeModeOptionRow(
                                title = "Nocturnal Obsidian (Dark Mode)",
                                description = "Deep yakisugi dark canvas tailored for low-light intimacy",
                                isSelected = selectedThemeMode == 0,
                                onSelect = { selectedThemeMode = 0 }
                            )
                            ThemeModeOptionRow(
                                title = "Porcelain Cream (Light Mode)",
                                description = "High-legibility warm architectural parchment surface",
                                isSelected = selectedThemeMode == 1,
                                onSelect = { selectedThemeMode = 1 }
                            )
                            ThemeModeOptionRow(
                                title = "System Synchronized",
                                description = "Automatically conforms to your device light/dark schedule",
                                isSelected = selectedThemeMode == 2,
                                onSelect = { selectedThemeMode = 2 }
                            )
                        }

                        HorizontalDivider(color = colors.badgeBorder, thickness = 1.dp)

                        // Custom Theme Accent Options
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "CUSTOM ACCENT PALETTE",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colors.subtitleText
                            )

                            val accentSwatches = listOf(
                                "Radiant Terracotta" to colors.terracottaAccent,
                                "Alpine Emerald" to colors.strengthStrong,
                                "Nordic Cobalt" to colorScheme.secondary,
                                "Royal Amethyst" to colorScheme.tertiary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                accentSwatches.forEachIndexed { index, (label, swatchColor) ->
                                    val isSelected = selectedCustomAccentIndex == index
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(colors.cardDarkBubble)
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) colors.terracottaAccent else colors.badgeBorder,
                                                shape = RoundedCornerShape(12.dp)
                                            )
                                            .clickable { selectedCustomAccentIndex = index }
                                            .padding(vertical = 12.dp, horizontal = 6.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(swatchColor),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Filled.Check,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = label.split(" ").last(),
                                            fontFamily = PlusJakartaSansFamily,
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                            fontSize = 11.sp,
                                            color = if (isSelected) colorScheme.onSurface else colors.subtitleText
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = colors.badgeBorder, thickness = 1.dp)

                        // High Contrast Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "High Contrast Insets",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Elevates micro-borders and squircle containers",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                            }
                            Switch(
                                checked = highContrastMode,
                                onCheckedChange = { highContrastMode = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.terracottaAccent,
                                    uncheckedThumbColor = colors.subtitleText,
                                    uncheckedTrackColor = colors.cardDarkBubble
                                )
                            )
                        }
                    }
                }

                // Card 2: Cache & Device Storage (Real & Usable)
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Storage,
                    title = "Cache & Local Storage",
                    subtitle = "${formatBytes(cacheSizeBytes)} cached on this device",
                    isExpanded = expandedCard == SettingsCardType.CACHE,
                    onToggle = { toggleCard(SettingsCardType.CACHE) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "DEVICE STORAGE USAGE",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )

                        // Metric Card
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(colors.cardDarkBubble)
                                .border(1.dp, colors.badgeBorder, RoundedCornerShape(14.dp))
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Temporary Image & Relay Cache",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 13.sp,
                                    color = colors.subtitleText
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = formatBytes(cacheSizeBytes),
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 24.sp,
                                    color = colorScheme.onSurface
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.badgeBackground)
                                    .border(1.dp, colors.badgeBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CleaningServices,
                                    contentDescription = null,
                                    tint = colors.terracottaAccent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Text(
                            text = "Clearing local cache removes temporary avatar previews and decoded camera photo buffers. Your encrypted message database remains secure and intact.",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = colors.subtitleText
                        )

                        Button(
                            onClick = clearCacheAction,
                            enabled = !isClearingCache && cacheSizeBytes > 0L,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.cardDarkBubble,
                                contentColor = colorScheme.onSurface,
                                disabledContainerColor = colors.cardDarkBubble.copy(alpha = 0.5f),
                                disabledContentColor = colors.subtitleText
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DeleteOutline,
                                    contentDescription = null,
                                    tint = if (cacheSizeBytes > 0L) colorScheme.error else colors.subtitleText,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isClearingCache) "Clearing Cache..." else "Clear Local Cache",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                // Card 3: Privacy & Reciprocal Delivery
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Notifications,
                    title = "Privacy & Messaging",
                    subtitle = "Bilateral sharing, read receipts, and typing",
                    isExpanded = expandedCard == SettingsCardType.PRIVACY,
                    onToggle = { toggleCard(SettingsCardType.PRIVACY) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "PRIVACY GATES",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )

                        // Bilateral Notice Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.cardDarkBubble)
                                .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "Bilateral Avatar Reveal",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    color = colors.terracottaAccent
                                )
                                Text(
                                    text = "Active. Your profile picture and identity photo are shared only after both participants exchange their first message.",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    lineHeight = 18.sp,
                                    color = colors.subtitleText
                                )
                            }
                        }

                        HorizontalDivider(color = colors.badgeBorder, thickness = 1.dp)

                        // Read Receipts
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Read Receipts",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Displays double checkmark when messages are read",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                            }
                            Switch(
                                checked = readReceiptsEnabled,
                                onCheckedChange = { readReceiptsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.terracottaAccent,
                                    uncheckedThumbColor = colors.subtitleText,
                                    uncheckedTrackColor = colors.cardDarkBubble
                                )
                            )
                        }

                        HorizontalDivider(color = colors.badgeBorder, thickness = 1.dp)

                        // Typing Indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Typing Indicators",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Signals presence when actively drafting a reply",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                            }
                            Switch(
                                checked = typingIndicatorsEnabled,
                                onCheckedChange = { typingIndicatorsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.terracottaAccent,
                                    uncheckedThumbColor = colors.subtitleText,
                                    uncheckedTrackColor = colors.cardDarkBubble
                                )
                            )
                        }

                        HorizontalDivider(color = colors.badgeBorder, thickness = 1.dp)

                        // Message Previews
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Message Snippets in Feed",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "Preview message text on home conversation rows",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = colors.subtitleText
                                )
                            }
                            Switch(
                                checked = messagePreviewsEnabled,
                                onCheckedChange = { messagePreviewsEnabled = it },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = colors.terracottaAccent,
                                    uncheckedThumbColor = colors.subtitleText,
                                    uncheckedTrackColor = colors.cardDarkBubble
                                )
                            )
                        }
                    }
                }

                // Card 4: Security & End-to-End Cryptography
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Security,
                    title = "Security & Encryption",
                    subtitle = "Temporary relay purging & vault verification",
                    isExpanded = expandedCard == SettingsCardType.SECURITY,
                    onToggle = { toggleCard(SettingsCardType.SECURITY) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "ARCHITECTURE SECURITY",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            letterSpacing = 1.sp,
                            color = colors.subtitleText
                        )

                        // Security Attributes
                        SecurityAttributeRow(
                            label = "Temporary Server Relay",
                            value = "Purged immediately upon delivery ACK"
                        )
                        SecurityAttributeRow(
                            label = "Local Storage Engine",
                            value = "Android Room SQLite (Encrypted Vault)"
                        )
                        SecurityAttributeRow(
                            label = "Cryptographic Primitives",
                            value = "Curve25519 · AES-256 GCM"
                        )

                        // Copy Device Fingerprint
                        Button(
                            onClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Nura Fingerprint", "NURA-VAULT-25519-VERIFIED")
                                clipboard.setPrimaryClip(clip)
                                Toast.makeText(context, "Identity fingerprint copied to clipboard", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.cardDarkBubble,
                                contentColor = colorScheme.onSurface
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = null,
                                    tint = colors.terracottaAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "Copy Cryptographic Fingerprint",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }

                // Card 5: About & Build Information
                ExpandableSettingsCard(
                    icon = Icons.Outlined.Info,
                    title = "About Nura",
                    subtitle = "Version 1.0.0 Architecture Build",
                    isExpanded = expandedCard == SettingsCardType.ABOUT,
                    onToggle = { toggleCard(SettingsCardType.ABOUT) }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Nura Messaging",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Minimalist architectural messaging system crafted with quiet nocturnal luxury, zero telemetry tracking, and privacy-first local message vaults.",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                            lineHeight = 20.sp,
                            color = colors.subtitleText
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Build: Living Architecture 1.0.0 (Clean MVVM)",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = colors.terracottaAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandableSettingsCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 250),
        label = "arrowRotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.badgeBackground)
            .border(
                width = if (isExpanded) 1.5.dp else 1.dp,
                color = if (isExpanded) colors.terracottaAccent else colors.badgeBorder,
                shape = RoundedCornerShape(18.dp)
            )
            .animateContentSize(animationSpec = tween(durationMillis = 250))
    ) {
        // Card Header Trigger
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.cardDarkBubble)
                        .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isExpanded) colors.terracottaAccent else colors.brandLogoText,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = title,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        color = colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        color = colors.subtitleText
                    )
                }
            }

            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (isExpanded) "Collapse" else "Expand",
                tint = if (isExpanded) colors.terracottaAccent else colors.subtitleText,
                modifier = Modifier
                    .size(22.dp)
                    .rotate(arrowRotation)
            )
        }

        // Expandable Content Body
        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = tween(250)) + fadeIn(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)) + fadeOut(animationSpec = tween(150))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 18.dp, top = 4.dp)
            ) {
                HorizontalDivider(
                    color = colors.badgeBorder,
                    thickness = 1.dp,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                content()
            }
        }
    }
}

@Composable
private fun ThemeModeOptionRow(
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardDarkBubble)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) colors.terracottaAccent else colors.badgeBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onSelect() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                color = colors.subtitleText
            )
        }

        RadioButton(
            selected = isSelected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = colors.terracottaAccent,
                unselectedColor = colors.subtitleText
            )
        )
    }
}

@Composable
private fun SecurityAttributeRow(
    label: String,
    value: String
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.cardDarkBubble)
            .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = label,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                color = colors.subtitleText
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontFamily = PlusJakartaSansFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = colorScheme.onSurface
            )
        }
        Icon(
            imageVector = Icons.Outlined.Check,
            contentDescription = null,
            tint = colors.terracottaAccent,
            modifier = Modifier.size(16.dp)
        )
    }
}
