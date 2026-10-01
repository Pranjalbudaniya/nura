package com.nura.messaging.features.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import coil.compose.AsyncImage
import com.nura.messaging.core.common.ui.component.AvatarArchetypeCanvas
import com.nura.messaging.core.common.ui.theme.NuraTheme
import com.nura.messaging.core.common.ui.theme.PlusJakartaSansFamily
import com.nura.messaging.domain.entities.auth.AuthUser
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class HomeFilterTab {
    ALL,
    UNREAD
}

data class HomeConversationItem(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val initials: String,
    val lastMessage: String,
    val timestamp: String,
    val unreadCount: Int = 0,
    val isOnline: Boolean = false,
    val isPinned: Boolean = false,
    val isVoiceNote: Boolean = false,
    val voiceNoteDuration: String? = null,
    val isOutgoing: Boolean = false,
    val participantId: String = "",
    val participantUsername: String = "",
    val isProfileShared: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    user: AuthUser?,
    conversations: List<HomeConversationItem> = emptyList(),
    profilePictureUri: String? = null,
    selectedPresetIndex: Int? = null,
    selectedPresetColor: Long? = null,
    onNavigateToAccount: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNewConversation: () -> Unit = {},
    onConversationClick: (HomeConversationItem) -> Unit = {},
    onSignOut: () -> Unit = {},
    connectContent: (@Composable (onDismiss: () -> Unit) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    var isConnectExpanded by remember { mutableStateOf(false) }
    BackHandler(enabled = isConnectExpanded) {
        isConnectExpanded = false
    }

    var selectedFilter by remember { mutableStateOf(HomeFilterTab.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var showAccountSheet by remember { mutableStateOf(false) }
    var showFullAvatarPreview by remember { mutableStateOf(false) }

    // Count of distinct conversations that have unread messages
    val unreadPeopleCount = remember(conversations) {
        conversations.count { it.unreadCount > 0 }
    }

    val filteredConversations = remember(conversations, selectedFilter, searchQuery) {
        conversations.filter { item ->
            val matchesFilter = when (selectedFilter) {
                HomeFilterTab.ALL -> true
                HomeFilterTab.UNREAD -> item.unreadCount > 0
            }
            val matchesSearch = if (searchQuery.isBlank()) {
                true
            } else {
                item.name.contains(searchQuery, ignoreCase = true) ||
                        item.lastMessage.contains(searchQuery, ignoreCase = true)
            }
            matchesFilter && matchesSearch
        }
    }

    // Pull-to-refresh offset and spring animation
    val pullOffset = remember { Animatable(0f) }
    var isRefreshing by remember { mutableStateOf(false) }

    val refreshTriggerDp = 68.dp
    val refreshTriggerPx = with(density) { refreshTriggerDp.toPx() }
    val maxPullPx = with(density) { 140.dp.toPx() }
    val maxIndicatorTravelPx = with(density) { 52.dp.toPx() }

    // Continuous spin during active refreshing
    val infiniteTransition = rememberInfiniteTransition(label = "refreshSpinner")
    val spinningRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 750, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "spinningRotation"
    )

    val nestedScrollConnection = remember(coroutineScope, refreshTriggerPx, maxPullPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                // If dragging upwards while pulled down, consume negative scroll to collapse pullOffset first
                if (available.y < 0f && pullOffset.value > 0f) {
                    val newOffset = (pullOffset.value + available.y).coerceAtLeast(0f)
                    val consumedY = newOffset - pullOffset.value
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, consumedY)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                // Pull down when list or container is at top
                if (source == NestedScrollSource.UserInput && available.y > 0f && !isRefreshing) {
                    val resistance = 0.45f
                    val newOffset = (pullOffset.value + available.y * resistance).coerceAtMost(maxPullPx)
                    coroutineScope.launch {
                        pullOffset.snapTo(newOffset)
                    }
                    return Offset(0f, available.y)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (pullOffset.value > 0f) {
                    if (pullOffset.value >= refreshTriggerPx && !isRefreshing) {
                        isRefreshing = true
                        // Settle at refresh trigger position
                        pullOffset.animateTo(
                            targetValue = refreshTriggerPx,
                            animationSpec = spring(
                                dampingRatio = 0.65f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                        // Perform refresh
                        delay(1200)
                        isRefreshing = false
                        // Smooth physics bounce back
                        pullOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.55f,
                                stiffness = Spring.StiffnessLow
                            )
                        )
                    } else if (!isRefreshing) {
                        // Spring back without refreshing
                        pullOffset.animateTo(
                            targetValue = 0f,
                            animationSpec = spring(
                                dampingRatio = 0.6f,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    }
                    return Velocity.Zero
                }
                return super.onPreFling(available)
            }
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    focusManager.clearFocus()
                },
            containerColor = colorScheme.surface,
            floatingActionButton = {
                if (!isConnectExpanded) {
                    FloatingActionButton(
                        onClick = {
                            if (connectContent != null) {
                                isConnectExpanded = true
                            } else {
                                onNewConversation()
                            }
                        },
                        containerColor = colors.terracottaAccent,
                        contentColor = Color.White,
                        shape = CircleShape,
                        modifier = Modifier.padding(bottom = 8.dp, end = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "New Conversation",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(nestedScrollConnection)
        ) {
            // LAYER 1: Refresh Spinner (Appears from behind the search bar as chats pull down)
            val pullFraction = (pullOffset.value / refreshTriggerPx).coerceIn(0f, 1f)
            val indicatorTravel = (pullOffset.value * 0.45f).coerceAtMost(maxIndicatorTravelPx)
            val indicatorAlpha = (pullOffset.value / (refreshTriggerPx * 0.4f)).coerceIn(0f, 1f)
            val indicatorScale = (0.5f + 0.5f * pullFraction).coerceIn(0.5f, 1f)
            val currentRotation = if (isRefreshing) spinningRotation else pullFraction * 360f

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 110.dp)
                    .graphicsLayer {
                        translationY = indicatorTravel
                        alpha = indicatorAlpha
                        scaleX = indicatorScale
                        scaleY = indicatorScale
                    }
                    .zIndex(1f),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(colorScheme.surfaceContainerLow)
                        .border(1.dp, colors.badgeBorder, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Refreshing",
                        tint = colors.terracottaAccent,
                        modifier = Modifier
                            .size(20.dp)
                            .rotate(currentRotation)
                    )
                }
            }

            // LAYER 2: Floating Chats Feed & Empty State
            // Pulls down with translationY and bounces back with spring animation
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 116.dp)
                    .graphicsLayer {
                        translationY = pullOffset.value
                    }
                    .zIndex(2f)
            ) {
                // Filter Tabs: All, Unread
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // "All" tab
                    val isAllSelected = selectedFilter == HomeFilterTab.ALL
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isAllSelected) colors.terracottaAccent else colorScheme.surfaceContainerLow)
                            .clickable { selectedFilter = HomeFilterTab.ALL }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "All",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (isAllSelected) Color.White else colorScheme.onSurfaceVariant
                        )
                    }

                    // "Unread" tab
                    val isUnreadSelected = selectedFilter == HomeFilterTab.UNREAD
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (isUnreadSelected) colors.terracottaAccent else colorScheme.surfaceContainerLow)
                            .clickable { selectedFilter = HomeFilterTab.UNREAD }
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Unread",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = if (isUnreadSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 13.sp,
                                color = if (isUnreadSelected) Color.White else colorScheme.onSurfaceVariant
                            )
                            if (unreadPeopleCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .height(18.dp)
                                        .widthIn(min = 18.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnreadSelected) Color.White.copy(alpha = 0.25f) else colors.terracottaAccent)
                                        .padding(horizontal = 5.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "$unreadPeopleCount",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        lineHeight = 10.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }

                // Conversations List Feed OR Connect With People Empty State
                if (filteredConversations.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(top = 48.dp, bottom = 120.dp)
                        ) {
                            // Atmospheric Icon Aura
                            Box(
                                modifier = Modifier.size(88.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(88.dp)
                                        .background(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    colors.terracottaAccent.copy(alpha = 0.16f),
                                                    Color.Transparent
                                                )
                                            ),
                                            shape = CircleShape
                                        )
                                )
                                Box(
                                    modifier = Modifier
                                        .size(64.dp)
                                        .clip(RoundedCornerShape(22.dp))
                                        .background(colors.badgeBackground)
                                        .border(1.dp, colors.badgeBorder, RoundedCornerShape(22.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Forum,
                                        contentDescription = null,
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(30.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))

                            Text(
                                text = "Connect with people",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                letterSpacing = (-0.015).sp,
                                color = colorScheme.onSurface,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = "Start a new conversation or invite friends with your Nura key to begin messaging securely.",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.Normal,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                color = colors.subtitleText,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                            Button(
                                onClick = {
                                    if (connectContent != null) {
                                        isConnectExpanded = true
                                    } else {
                                        onNewConversation()
                                    }
                                },
                                modifier = Modifier.height(46.dp),
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colors.terracottaAccent,
                                    contentColor = Color.White
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Start a conversation",
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        items(filteredConversations, key = { it.id }) { item ->
                            ConversationRowItem(
                                conversation = item,
                                onClick = { onConversationClick(item) }
                            )
                        }
                    }
                }
            }

            // LAYER 3: Fixed Header & Quiet Search Bar (Z-Index 5 masks the refresh icon)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surface)
                    .zIndex(5f)
            ) {
                // Top Bar: Brand Wordmark on LEFT, Profile Picture on RIGHT
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Brand Wordmark on Left: "nura" with warm radiant accent dot
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "nura",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            letterSpacing = (-0.02).sp,
                            color = colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(colors.terracottaAccent, CircleShape)
                        )
                    }

                    // User Profile Picture on Right (opens Account & Settings Drawer)
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.badgeBackground)
                            .border(1.dp, colors.badgeBorder, RoundedCornerShape(12.dp))
                            .clickable {
                                showAccountSheet = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !profilePictureUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = "Profile and Settings",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(12.dp))
                                )
                            }
                            selectedPresetIndex != null -> {
                                AvatarArchetypeCanvas(
                                    presetIndex = selectedPresetIndex,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                )
                            }
                            selectedPresetColor != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(selectedPresetColor.toULong()), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: ""
                                    Text(
                                        text = userInitial,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            else -> {
                                val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: ""
                                if (userInitial.isNotEmpty()) {
                                    Text(
                                        text = userInitial,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.terracottaAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "Profile and Settings",
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Quiet Integrated Search Bar (No always-blinking cursor; only appears when explicitly tapped)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.inputBackground)
                        .border(1.dp, colors.inputBorder, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = "Search",
                            tint = colors.subtitleText,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search conversations",
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 14.sp,
                                    color = colors.inputPlaceholder
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = PlusJakartaSansFamily,
                                    fontSize = 14.sp,
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = if (isSearchFocused) SolidColor(colors.terracottaAccent) else SolidColor(Color.Transparent),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .onFocusChanged { focusState ->
                                        isSearchFocused = focusState.isFocused
                                    }
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    focusManager.clearFocus()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear",
                                    tint = colors.subtitleText,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Account Modal Bottom Sheet
    if (showAccountSheet) {
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showAccountSheet = false },
            sheetState = sheetState,
            containerColor = colorScheme.surfaceContainerLow,
            scrimColor = Color.Black.copy(alpha = 0.65f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar and User Name (Tappable to expand full-size preview)
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(colors.badgeBackground)
                        .border(1.dp, colors.badgeBorder, RoundedCornerShape(22.dp))
                        .clickable {
                            showFullAvatarPreview = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    when {
                        !profilePictureUri.isNullOrBlank() -> {
                            AsyncImage(
                                model = profilePictureUri,
                                contentDescription = "User Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(22.dp))
                            )
                        }
                        selectedPresetIndex != null -> {
                            AvatarArchetypeCanvas(
                                presetIndex = selectedPresetIndex,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(6.dp)
                            )
                        }
                        selectedPresetColor != null -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color(selectedPresetColor.toULong()), RoundedCornerShape(22.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: ""
                                Text(
                                    text = userInitial,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = Color.White
                                )
                            }
                        }
                        else -> {
                            val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                ?: ""
                            if (userInitial.isNotEmpty()) {
                                Text(
                                    text = userInitial,
                                    fontFamily = PlusJakartaSansFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 26.sp,
                                    color = colors.terracottaAccent
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = "User",
                                    tint = colors.terracottaAccent,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = user?.name?.ifBlank { user.username } ?: "Nura User",
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colorScheme.onSurface
                )

                if (!user?.username.isNullOrBlank()) {
                    Text(
                        text = "@${user?.username}",
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 13.sp,
                        color = colors.subtitleText
                    )
                }

                if (!user?.email.isNullOrBlank()) {
                    Text(
                        text = user.email,
                        fontFamily = PlusJakartaSansFamily,
                        fontSize = 12.sp,
                        color = colors.subtitleText
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Account Button
                Surface(
                    onClick = {
                        showAccountSheet = false
                        onNavigateToAccount()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.badgeBackground,
                    border = BorderStroke(1.dp, colors.badgeBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = colors.brandLogoText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Account",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.subtitleText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Settings Button
                Surface(
                    onClick = {
                        showAccountSheet = false
                        onNavigateToSettings()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    color = colors.badgeBackground,
                    border = BorderStroke(1.dp, colors.badgeBorder)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = colors.brandLogoText,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Settings",
                                fontFamily = PlusJakartaSansFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colorScheme.onSurface
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = colors.subtitleText,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Sign Out Button
                Button(
                    onClick = {
                        showAccountSheet = false
                        onSignOut()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorScheme.error.copy(alpha = 0.12f),
                        contentColor = colorScheme.error
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Sign Out",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Full-Screen / Expanded Avatar Preview Dialog
    if (showFullAvatarPreview) {
        Dialog(
            onDismissRequest = { showFullAvatarPreview = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable { showFullAvatarPreview = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(260.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(colors.badgeBackground)
                            .border(2.dp, colors.badgeBorder, RoundedCornerShape(32.dp))
                            .clickable(enabled = false) {},
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            !profilePictureUri.isNullOrBlank() -> {
                                AsyncImage(
                                    model = profilePictureUri,
                                    contentDescription = "User Avatar Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(32.dp))
                                )
                            }
                            selectedPresetIndex != null -> {
                                AvatarArchetypeCanvas(
                                    presetIndex = selectedPresetIndex,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(24.dp)
                                )
                            }
                            selectedPresetColor != null -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(selectedPresetColor.toULong()), RoundedCornerShape(32.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                        ?: ""
                                    Text(
                                        text = userInitial,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 72.sp,
                                        color = Color.White
                                    )
                                }
                            }
                            else -> {
                                val userInitial = user?.name?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: user?.username?.firstOrNull()?.uppercaseChar()?.toString()
                                    ?: ""
                                if (userInitial.isNotEmpty()) {
                                    Text(
                                        text = userInitial,
                                        fontFamily = PlusJakartaSansFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 72.sp,
                                        color = colors.terracottaAccent
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Outlined.Person,
                                        contentDescription = "User",
                                        tint = colors.terracottaAccent,
                                        modifier = Modifier.size(96.dp)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = user?.name?.ifBlank { user.username } ?: "Profile Picture",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    IconButton(
                        onClick = { showFullAvatarPreview = false },
                        modifier = Modifier
                            .size(44.dp)
                            .background(colors.badgeBackground, CircleShape)
                            .border(1.dp, colors.badgeBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close Preview",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    // EXPANDING CONNECT SCREEN OVERLAY FROM PLUS ICON
    if (connectContent != null) {
        AnimatedVisibility(
            visible = isConnectExpanded,
            enter = scaleIn(
                initialScale = 0.08f,
                transformOrigin = TransformOrigin(0.92f, 0.94f),
                animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)
            ) + fadeIn(animationSpec = tween(220)),
            exit = scaleOut(
                targetScale = 0.08f,
                transformOrigin = TransformOrigin(0.92f, 0.94f),
                animationSpec = spring(dampingRatio = 0.88f, stiffness = Spring.StiffnessMedium)
            ) + fadeOut(animationSpec = tween(180)),
            modifier = Modifier
                .fillMaxSize()
                .zIndex(25f)
        ) {
            connectContent {
                isConnectExpanded = false
            }
        }
    }
    }
}

@Composable
private fun ConversationRowItem(
    conversation: HomeConversationItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = NuraTheme.colors
    val colorScheme = MaterialTheme.colorScheme

    val isUnread = conversation.unreadCount > 0
    val rowBackground = if (isUnread) colorScheme.surfaceContainerLow.copy(alpha = 0.45f) else Color.Transparent

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar with rounded squircle
        Box(
            modifier = Modifier.size(48.dp)
        ) {
            if (conversation.isProfileShared && !conversation.avatarUrl.isNullOrBlank()) {
                AsyncImage(
                    model = conversation.avatarUrl,
                    contentDescription = conversation.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(colorScheme.surfaceContainerHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = conversation.initials,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }

            // Online status indicator
            if (conversation.isOnline) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .align(Alignment.BottomEnd)
                        .background(colorScheme.surface, CircleShape)
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(colors.terracottaAccent, CircleShape)
                    )
                }
            }
        }

        // Conversation details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 4.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            // Line 1: Name, Pin icon, Timestamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = conversation.name,
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (conversation.isPinned) {
                        Icon(
                            imageVector = Icons.Outlined.PushPin,
                            contentDescription = "Pinned",
                            tint = colors.terracottaAccent,
                            modifier = Modifier
                                .size(13.dp)
                                .rotate(45f)
                        )
                    }
                }

                Text(
                    text = conversation.timestamp,
                    fontFamily = PlusJakartaSansFamily,
                    fontWeight = if (isUnread) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 12.sp,
                    color = if (isUnread) colors.terracottaAccent else colors.subtitleText
                )
            }

            // Line 2: Message preview, Unread message count badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (conversation.isVoiceNote) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.GraphicEq,
                            contentDescription = null,
                            tint = colors.terracottaAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Voice note (${conversation.voiceNoteDuration ?: "0:30"})",
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                            fontSize = 13.sp,
                            color = if (isUnread) colorScheme.onSurface else colors.subtitleText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                } else {
                    val previewPrefix = if (conversation.isOutgoing) "You: " else ""
                    Text(
                        text = "$previewPrefix${conversation.lastMessage}",
                        fontFamily = PlusJakartaSansFamily,
                        fontWeight = if (isUnread) FontWeight.Medium else FontWeight.Normal,
                        fontSize = 13.sp,
                        color = if (isUnread) colorScheme.onSurface else colors.subtitleText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }

                // Unread message count badge for this particular conversation (displays 9+ if > 9)
                if (conversation.unreadCount > 0) {
                    val badgeText = if (conversation.unreadCount > 9) "9+" else "${conversation.unreadCount}"
                    Box(
                        modifier = Modifier
                            .height(18.dp)
                            .widthIn(min = 18.dp)
                            .clip(CircleShape)
                            .background(colors.terracottaAccent)
                            .padding(horizontal = if (conversation.unreadCount > 9) 5.dp else 0.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = badgeText,
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            lineHeight = 10.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
