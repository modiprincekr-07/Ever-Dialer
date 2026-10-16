package com.coolappstore.everdialer.by.svhp.view.screen

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.automirrored.outlined.SpeakerNotes
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.coolappstore.everdialer.by.svhp.controller.sms.SmsEventBus
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import android.widget.Toast
import com.coolappstore.everdialer.by.svhp.controller.ContactsViewModel
import com.coolappstore.everdialer.by.svhp.controller.SmsViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.BlockedNumbersManager
import com.coolappstore.everdialer.by.svhp.controller.util.DefaultSmsManager
import com.coolappstore.everdialer.by.svhp.controller.util.PreferenceManager
import com.coolappstore.everdialer.by.svhp.controller.util.makeCall
import com.coolappstore.evercallrecorder.by.svhp.ui.common.SwipeableItemContainer
import com.coolappstore.evercallrecorder.by.svhp.ui.common.SwipeActionItem
import com.coolappstore.everdialer.by.svhp.controller.FloatingSmsService
import com.coolappstore.everdialer.by.svhp.controller.util.SwipeActionHelper
import com.coolappstore.everdialer.by.svhp.modal.data.Contact
import com.coolappstore.everdialer.by.svhp.modal.data.SmsConversation
import com.coolappstore.everdialer.by.svhp.modal.data.SmsFilterTab
import com.coolappstore.everdialer.by.svhp.view.components.ReorderSmsTabsDialog
import com.coolappstore.everdialer.by.svhp.view.components.RivoAvatar
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenu
import com.coolappstore.everdialer.by.svhp.view.components.RivoDropdownMenuItem
import com.coolappstore.everdialer.by.svhp.view.components.ScrollHapticsEffect
import com.coolappstore.everdialer.by.svhp.view.components.TopBar
import com.coolappstore.everdialer.by.svhp.view.theme.TabTransitionStyle
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.generated.destinations.ContactDetailsScreenDestination
import com.ramcosta.composedestinations.generated.destinations.NewMessageScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsChatScreenDestination
import com.ramcosta.composedestinations.generated.destinations.SmsSettingsScreenDestination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import com.coolappstore.everdialer.by.svhp.controller.util.formatDateHeader
import com.coolappstore.everdialer.by.svhp.controller.util.formatTimeOnly
import com.coolappstore.everdialer.by.svhp.controller.util.numbersLikelyMatch
import com.coolappstore.everdialer.by.svhp.view.components.RivoSectionHeader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Destination<RootGraph>(style = TabTransitionStyle::class)
@Composable
fun SmsScreen(
    navController: NavController,
    navigator: DestinationsNavigator,
    initialSharedText: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val smsVM: SmsViewModel = koinActivityViewModel()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsVer by prefs.settingsChanged.collectAsState()

    var isDefaultSms by remember { mutableStateOf(DefaultSmsManager.isDefaultSms(context)) }
    var hasSmsPermissions by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED
        )
    }

    val defaultSmsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasSmsPermissions = permissions[Manifest.permission.READ_SMS] == true &&
                            permissions[Manifest.permission.SEND_SMS] == true
        if (hasSmsPermissions) {
            smsVM.refreshConversations()
        }
    }

    val rawConversations by smsVM.filteredConversations.collectAsState()
    val isLoading by smsVM.isLoading.collectAsState()
    val searchQuery by smsVM.searchQuery.collectAsState()

    // Quik preference: unread at top
    val unreadAtTop = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_UNREAD_AT_TOP, true)
    }
    val autoColor = remember(settingsVer) {
        prefs.getBoolean(PreferenceManager.KEY_SMS_AUTO_COLOR_AVATARS, true)
    }
    val swipeRightAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_RIGHT_ACTION, "none") ?: "none"
    }
    val swipeLeftAction = remember(settingsVer) {
        prefs.getString(PreferenceManager.KEY_SMS_SWIPE_LEFT_ACTION, "none") ?: "none"
    }

    val colorScheme = MaterialTheme.colorScheme
    val leftSwipeActionItem = remember(swipeLeftAction, colorScheme) {
        when (swipeLeftAction) {
            "delete" -> SwipeActionItem("delete", "Delete", Icons.Default.Delete, colorScheme.error, colorScheme.onError, isDestructive = true)
            "read" -> SwipeActionItem("read", "Mark Read", Icons.Outlined.MarkChatRead, colorScheme.secondary, colorScheme.onSecondary, isDestructive = false)
            else -> null
        }
    }
    val rightSwipeActionItem = remember(swipeRightAction, colorScheme) {
        when (swipeRightAction) {
            "call" -> SwipeActionItem("call", "Call", Icons.Default.Call, colorScheme.primary, colorScheme.onPrimary, isDestructive = false)
            "read" -> SwipeActionItem("read", "Mark Read", Icons.Outlined.MarkChatRead, colorScheme.secondary, colorScheme.onSecondary, isDestructive = false)
            "delete" -> SwipeActionItem("delete", "Delete", Icons.Default.Delete, colorScheme.error, colorScheme.onError, isDestructive = true)
            else -> null
        }
    }

    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listStates = remember { mutableMapOf<SmsFilterTab, LazyListState>() }
    val chipRowState = rememberLazyListState()

    val selectedFilter by smsVM.selectedFilter.collectAsState()
    val filterOrder by smsVM.filterOrder.collectAsState()
    var menuExpandedForTab by remember { mutableStateOf<SmsFilterTab?>(null) }
    var showReorderDialog by remember { mutableStateOf(false) }

    val favoriteNumbers = remember(allContacts) {
        allContacts.filter { it.isFavorite }.flatMap { it.phoneNumbers }.toSet()
    }
    val favoriteNames = remember(allContacts) {
        allContacts.filter { it.isFavorite }.map { it.name.trim().lowercase() }.toSet()
    }

    val groupConversations: (List<SmsConversation>) -> Map<String, List<SmsConversation>> = remember(unreadAtTop) {
        { convList ->
            convList.groupBy { conv ->
                if (conv.date <= 0) "Older" else formatDateHeader(conv.date)
            }.mapValues { (_, list) ->
                if (unreadAtTop) {
                    val unread = list.filter { !it.isRead }
                    val read = list.filter { it.isRead }
                    unread + read
                } else {
                    list
                }
            }
        }
    }

    val conversations = remember(rawConversations, selectedFilter, favoriteNumbers, favoriteNames) {
        smsVM.filterConversations(rawConversations, selectedFilter, favoriteNumbers, favoriteNames)
    }

    val use24HourTime = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_CALL_TIME_FORMAT_24H, false) }

    LaunchedEffect(selectedFilter) {
        listStates[selectedFilter]?.scrollToItem(0)
    }

    // Selection mode (Quik multi-select action mode)
    val selectedThreadIds = remember { mutableStateListOf<Long>() }
    val isSelectionMode = selectedThreadIds.isNotEmpty()

    BackHandler(enabled = isSelectionMode) {
        selectedThreadIds.clear()
    }

    val cleanSharedText = remember(initialSharedText) {
        initialSharedText?.takeIf {
            it.isNotBlank() && it != "{initialSharedText}" && it != "null"
        }
    }
    var threadToDelete by remember { mutableStateOf<SmsConversation?>(null) }
    var showDeleteMultipleDialog by remember { mutableStateOf(false) }
    var showBlockMultipleDialog by remember { mutableStateOf(false) }

    LaunchedEffect(cleanSharedText) {
        if (!cleanSharedText.isNullOrBlank()) {
            navigator.navigate(NewMessageScreenDestination(initialText = cleanSharedText))
        }
    }

    LaunchedEffect(Unit) {
        isDefaultSms = DefaultSmsManager.isDefaultSms(context)
        smsVM.refreshConversations()
    }

    LaunchedEffect(Unit) {
        SmsEventBus.newSmsEvent.collect {
            smsVM.refreshConversations()
        }
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            delay(2500)
            smsVM.refreshConversations()
        }
    }

    Scaffold(
        topBar = {
            AnimatedContent(
                targetState = isSelectionMode,
                transitionSpec = {
                    fadeIn(animationSpec = androidx.compose.animation.core.tween(400)) +
                    slideInVertically(animationSpec = androidx.compose.animation.core.tween(400)) { -it / 3 } togetherWith
                    fadeOut(animationSpec = androidx.compose.animation.core.tween(400)) +
                    slideOutVertically(animationSpec = androidx.compose.animation.core.tween(400)) { -it / 3 }
                },
                label = "SmsTopBarAnim"
            ) { selectionMode ->
                if (selectionMode) {
                    // Quik Multi-Select Top Bar
                    TopAppBar(
                        title = {
                            Text(
                                text = "${selectedThreadIds.size} selected",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { selectedThreadIds.clear() }) {
                                Icon(Icons.Default.Close, contentDescription = "Cancel")
                            }
                        },
                        actions = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                SelectionActionButton(
                                    icon = Icons.Default.SelectAll,
                                    label = if (selectedThreadIds.size == conversations.size && conversations.isNotEmpty()) "Deselect" else "All",
                                    onClick = {
                                        if (selectedThreadIds.size == conversations.size) {
                                            selectedThreadIds.clear()
                                        } else {
                                            selectedThreadIds.clear()
                                            selectedThreadIds.addAll(conversations.map { it.threadId })
                                        }
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.MarkChatRead,
                                    label = "Read",
                                    onClick = {
                                        selectedThreadIds.forEach { tid ->
                                            smsVM.markThreadAsRead(tid)
                                        }
                                        selectedThreadIds.clear()
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.AutoMirrored.Outlined.SpeakerNotes,
                                    label = "Float",
                                    onClick = {
                                        if (!android.provider.Settings.canDrawOverlays(context)) {
                                            try {
                                                val intent = android.content.Intent(
                                                    android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                                    android.net.Uri.parse("package:${context.packageName}")
                                                )
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        } else {
                                            selectedThreadIds.firstOrNull()?.let { tid ->
                                                conversations.find { it.threadId == tid }?.let { conv ->
                                                    FloatingSmsService.start(
                                                        context = context,
                                                        threadId = conv.threadId,
                                                        address = conv.address,
                                                        contactName = conv.contactName,
                                                        photoUri = conv.photoUri
                                                    )
                                                }
                                            }
                                            if (selectedThreadIds.size > 1) {
                                                Toast.makeText(context, "Opened floating window for first selected chat", Toast.LENGTH_SHORT).show()
                                            }
                                            selectedThreadIds.clear()
                                        }
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.Block,
                                    label = "Block",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        showBlockMultipleDialog = true
                                    }
                                )
                                SelectionActionButton(
                                    icon = Icons.Outlined.Delete,
                                    label = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    onClick = {
                                        showDeleteMultipleDialog = true
                                    }
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )
                } else {
                    Column {
                        TopBar(navController = navController, navigator = navigator)
                        // Quik Header with Settings shortcut
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Messages",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                // Mark all as read
                                IconButton(onClick = {
                                    conversations.filter { !it.isRead }.forEach { conv ->
                                        smsVM.markThreadAsRead(conv.threadId)
                                    }
                                }) {
                                    Icon(
                                        Icons.Outlined.DoneAll,
                                        contentDescription = "Mark all as read",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                // Quik Settings
                                IconButton(onClick = {
                                    navigator.navigate(SmsSettingsScreenDestination())
                                }) {
                                    Icon(
                                        Icons.Outlined.Settings,
                                        contentDescription = "Quik SMS Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Filter Pills: All, Favourites, Contacts, Unknown, Payments, Recharge, OTP
                        val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                        val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                        val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                        val usePrimary = isSaturatedActive || !isDynamic
                        LazyRow(
                            state = chipRowState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filterOrder, key = { _, tab -> tab.id }) { index, tab ->
                                val isSelected = selectedFilter == tab
                                val chipContainerColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f)
                                    },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "chipBg"
                                )
                                val chipLabelColor by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                                    label = "chipFg"
                                )

                                Box(
                                    modifier = Modifier.animateItem(
                                        fadeInSpec = tween(220, easing = FastOutSlowInEasing),
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                    )
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(50.dp),
                                        color = chipContainerColor,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(50.dp))
                                            .combinedClickable(
                                                onClick = {
                                                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                    }
                                                    if (selectedFilter == tab) {
                                                        coroutineScope.launch {
                                                            listStates[tab]?.animateScrollToItem(0)
                                                        }
                                                    } else {
                                                        smsVM.setFilter(tab)
                                                        coroutineScope.launch {
                                                            listStates.getOrPut(tab) { LazyListState() }.scrollToItem(0)
                                                        }
                                                    }
                                                    coroutineScope.launch {
                                                        chipRowState.animateScrollToItem(index)
                                                    }
                                                },
                                                onLongClick = {
                                                    if (prefs.getBoolean(PreferenceManager.KEY_APP_HAPTICS, true)) {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                    menuExpandedForTab = tab
                                                }
                                            )
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .height(32.dp)
                                                .padding(horizontal = 16.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = tab.displayName,
                                                style = MaterialTheme.typography.labelMedium,
                                                color = chipLabelColor
                                            )
                                        }
                                    }

                                    RivoDropdownMenu(
                                        expanded = menuExpandedForTab == tab,
                                        onDismissRequest = { menuExpandedForTab = null }
                                    ) {
                                        if (index > 0) {
                                            RivoDropdownMenuItem(
                                                text = "Move Left",
                                                icon = Icons.AutoMirrored.Outlined.ArrowBack,
                                                onClick = {
                                                    menuExpandedForTab = null
                                                    smsVM.moveFilterTab(index, index - 1)
                                                    coroutineScope.launch {
                                                        chipRowState.animateScrollToItem((index - 1).coerceAtLeast(0))
                                                    }
                                                }
                                            )
                                        }
                                        if (index < filterOrder.lastIndex) {
                                            RivoDropdownMenuItem(
                                                text = "Move Right",
                                                icon = Icons.AutoMirrored.Outlined.ArrowForward,
                                                onClick = {
                                                    menuExpandedForTab = null
                                                    smsVM.moveFilterTab(index, index + 1)
                                                    coroutineScope.launch {
                                                        chipRowState.animateScrollToItem((index + 1).coerceAtMost(filterOrder.lastIndex))
                                                    }
                                                }
                                            )
                                        }
                                        RivoDropdownMenuItem(
                                            text = "Reorder All Tabs…",
                                            icon = Icons.Outlined.FilterList,
                                            onClick = {
                                                menuExpandedForTab = null
                                                showReorderDialog = true
                                            }
                                        )
                                        RivoDropdownMenuItem(
                                            text = "Reset Order",
                                            icon = Icons.Default.Refresh,
                                            onClick = {
                                                menuExpandedForTab = null
                                                smsVM.resetFilterOrder()
                                                coroutineScope.launch {
                                                    chipRowState.animateScrollToItem(0)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = !isSelectionMode,
                enter = fadeIn(androidx.compose.animation.core.tween(400)) + scaleIn(androidx.compose.animation.core.tween(400), initialScale = 0.8f),
                exit = fadeOut(androidx.compose.animation.core.tween(400)) + scaleOut(androidx.compose.animation.core.tween(400), targetScale = 0.8f)
            ) {
                val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
                val isSaturatedActive = remember(settingsVer, isDark) { prefs.isSaturatedForTheme(isDark) }
                val isDynamic = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_DYNAMIC_COLORS, true) }
                val usePrimary = isSaturatedActive || !isDynamic
                val fabBg = if (usePrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer
                val fabFg = if (usePrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                val fabShape = RoundedCornerShape(17.dp)
                val pillNav = remember(settingsVer) { prefs.getBoolean(PreferenceManager.KEY_PILL_NAV, true) }
                val isLandscape = LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
                val baseModifier = Modifier
                    .then(if (pillNav) Modifier.navigationBarsPadding().padding(bottom = 92.dp) else Modifier)
                    .then(if (isLandscape) Modifier.navigationBarsPadding().padding(bottom = 8.dp) else Modifier)

                FloatingActionButton(
                    onClick = {
                        navigator.navigate(NewMessageScreenDestination(initialText = null))
                    },
                    containerColor = fabBg,
                    contentColor = fabFg,
                    shape = fabShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 6.dp,
                        focusedElevation = 6.dp,
                        hoveredElevation = 6.dp
                    ),
                    modifier = baseModifier
                ) {
                    Icon(Icons.Default.AddComment, contentDescription = "New Message")
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentWindowInsets = WindowInsets(0)
    ) { paddingValues ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        AnimatedContent(
            targetState = selectedFilter,
            transitionSpec = {
                val fromIdx = filterOrder.indexOf(initialState).let { if (it == -1) 0 else it }
                val toIdx = filterOrder.indexOf(targetState).let { if (it == -1) 0 else it }
                val isForward = toIdx >= fromIdx
                val slideOffset = 240

                if (isForward) {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { slideOffset.coerceAtMost(it / 3) }
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(180, easing = FastOutLinearInEasing),
                        targetOffsetX = { -slideOffset.coerceAtMost(it / 3) }
                    ) + fadeOut(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ))
                } else {
                    (slideInHorizontally(
                        animationSpec = tween(220, easing = FastOutSlowInEasing),
                        initialOffsetX = { -slideOffset.coerceAtMost(it / 3) }
                    ) + fadeIn(
                        animationSpec = tween(220, easing = FastOutSlowInEasing)
                    )) togetherWith (slideOutHorizontally(
                        animationSpec = tween(180, easing = FastOutLinearInEasing),
                        targetOffsetX = { slideOffset.coerceAtMost(it / 3) }
                    ) + fadeOut(
                        animationSpec = tween(180, easing = FastOutLinearInEasing)
                    ))
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            label = "SmsTabAnimatedContent"
        ) { currentFilter ->
            val currentListState = listStates.getOrPut(currentFilter) { LazyListState() }
            ScrollHapticsEffect(currentListState)
            val currentConversations = remember(rawConversations, currentFilter, favoriteNumbers, favoriteNames) {
                smsVM.filterConversations(rawConversations, currentFilter, favoriteNumbers, favoriteNames)
            }
            val currentGrouped = remember(currentConversations, unreadAtTop) {
                groupConversations(currentConversations)
            }

            LazyColumn(
                state = currentListState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp + navBarBottom),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Default SMS App Warning Banner
                if (!isDefaultSms) {
                    item(key = "banner_default_sms") {
                        Surface(
                            onClick = {
                                DefaultSmsManager.requestDefaultSms(defaultSmsLauncher, context)
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(42.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.Outlined.Sms,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Set as Default SMS App",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        "Allow Ever Dialer to send, receive, and manage your text messages seamlessly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                                Icon(
                                    Icons.Default.ChevronRight,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Permissions prompt banner
                if (!hasSmsPermissions) {
                    item(key = "banner_permissions") {
                        Surface(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.READ_SMS,
                                        Manifest.permission.SEND_SMS,
                                        Manifest.permission.RECEIVE_SMS
                                    )
                                )
                            },
                            shape = RoundedCornerShape(18.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(28.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "SMS Permission Required",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                    Text(
                                        "Tap to grant SMS permissions so Ever Dialer can display your conversations.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty state
                if (currentConversations.isEmpty() && !isLoading) {
                    item(key = "empty_state_${currentFilter.id}") {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp)
                                .animateItem(
                                    fadeInSpec = tween(260, easing = FastOutSlowInEasing),
                                    placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                    fadeOutSpec = tween(180, easing = FastOutLinearInEasing)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    modifier = Modifier.size(72.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.Chat,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(36.dp)
                                        )
                                    }
                                }
                                Text(
                                    if (searchQuery.isNotBlank()) "No conversations match '$searchQuery'" else "No messages yet",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    "Start a conversation by tapping the button below",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Conversation items grouped by date header
                currentGrouped.forEach { (header, convsInGroup) ->
                    if (header.isNotBlank()) {
                        item(key = "header_${currentFilter.id}_$header", contentType = "sectionHeader") {
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 4.dp, vertical = 4.dp)
                                    .animateItem(
                                        fadeInSpec = null,
                                        placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                        fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                                    )
                            ) {
                                RivoSectionHeader(title = header)
                            }
                        }
                    }

                    items(
                        items = convsInGroup,
                        key = { it.threadId },
                        contentType = { "conversationItem" }
                    ) { conv ->
                        val isSelected = selectedThreadIds.contains(conv.threadId)

                        val handleSwipeAction: (String) -> Unit = { actionKey ->
                            when (actionKey) {
                                "call" -> makeCall(context, conv.address, null)
                                "read" -> smsVM.markThreadAsRead(conv.threadId)
                                "delete" -> threadToDelete = conv
                            }
                        }

                        SwipeableItemContainer(
                            modifier = Modifier.animateItem(
                                fadeInSpec = null,
                                placementSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioLowBouncy),
                                fadeOutSpec = tween(450, easing = FastOutLinearInEasing)
                            ),
                            leftAction = leftSwipeActionItem,
                            rightAction = rightSwipeActionItem,
                            onSwipeLeft = { handleSwipeAction(swipeLeftAction) },
                            onSwipeRight = { handleSwipeAction(swipeRightAction) },
                            enabled = !isSelectionMode,
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            ConversationItemRow(
                                conversation = conv,
                                timeStr = if (conv.date <= 0) "" else formatTimeOnly(conv.date, use24HourTime),
                                isSelected = isSelected,
                                isSelectionMode = isSelectionMode,
                                autoColor = autoColor,
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) selectedThreadIds.remove(conv.threadId)
                                        else selectedThreadIds.add(conv.threadId)
                                    } else {
                                        if (conv.threadId > 0) {
                                            smsVM.loadThreadMessages(conv.threadId)
                                        }
                                        navigator.navigate(
                                            SmsChatScreenDestination(
                                                threadId = conv.threadId,
                                                address = conv.address,
                                                contactName = conv.contactName,
                                                photoUri = conv.photoUri
                                            )
                                        )
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedThreadIds.add(conv.threadId)
                                    }
                                },
                                onAvatarClick = {
                                    if (!isSelectionMode) {
                                        navigator.navigate(
                                            ContactDetailsScreenDestination(
                                                phoneNumber = conv.address
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete single thread confirmation
    threadToDelete?.let { conv ->
        AlertDialog(
            onDismissRequest = { threadToDelete = null },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete conversation?") },
            text = { Text("Delete conversation with ${conv.contactName ?: conv.address}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        smsVM.deleteThread(conv.threadId)
                        threadToDelete = null
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { threadToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete multiple selected threads confirmation
    if (showDeleteMultipleDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteMultipleDialog = false },
            icon = { Icon(Icons.Outlined.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Delete ${selectedThreadIds.size} conversations?") },
            text = { Text("All messages in the selected conversations will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedThreadIds.forEach { tid ->
                            smsVM.deleteThread(tid)
                        }
                        selectedThreadIds.clear()
                        showDeleteMultipleDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete All")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteMultipleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Block multiple selected threads confirmation
    if (showBlockMultipleDialog) {
        AlertDialog(
            onDismissRequest = { showBlockMultipleDialog = false },
            icon = { Icon(Icons.Outlined.Block, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Block ${selectedThreadIds.size} conversations?") },
            text = { Text("Calls and messages from the selected numbers will be blocked.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val toBlock = conversations.filter { it.threadId in selectedThreadIds }
                        toBlock.forEach { conv ->
                            if (conv.address.isNotBlank()) {
                                BlockedNumbersManager.block(context, prefs, conv.address)
                            }
                        }
                        selectedThreadIds.clear()
                        showBlockMultipleDialog = false
                        Toast.makeText(context, "Blocked selected conversations", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Block")
                }
            },
            dismissButton = {
                TextButton(onClick = { showBlockMultipleDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showReorderDialog) {
        ReorderSmsTabsDialog(
            onDismissRequest = { showReorderDialog = false },
            smsVM = smsVM
        )
    }
}

@Composable
private fun SelectionActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    enabled: Boolean = true
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            modifier = Modifier.size(20.dp),
            tint = if (enabled) tint else tint.copy(alpha = 0.38f)
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = if (enabled) tint else tint.copy(alpha = 0.38f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ConversationItemRow(
    conversation: SmsConversation,
    timeStr: String,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    autoColor: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val targetBgColor = when {
        isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
        !conversation.isRead -> MaterialTheme.colorScheme.surfaceContainerHigh
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
    }
    val animatedBgColor by androidx.compose.animation.animateColorAsState(
        targetValue = targetBgColor,
        animationSpec = androidx.compose.animation.core.tween(400),
        label = "ConversationRowBgAnim"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = animatedBgColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AnimatedVisibility(
                visible = isSelectionMode,
                enter = fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) +
                        expandHorizontally(animationSpec = androidx.compose.animation.core.tween(300)),
                exit = fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) +
                        shrinkHorizontally(animationSpec = androidx.compose.animation.core.tween(300))
            ) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onClick() },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { onAvatarClick() }
            ) {
                RivoAvatar(
                    name = conversation.contactName ?: conversation.address,
                    photoUri = conversation.photoUri,
                    autoColorAvatars = autoColor,
                    obeySolidIcons = false,
                    size = 48.dp
                )
            }
            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.contactName ?: conversation.address,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    if (timeStr.isNotBlank()) {
                        Spacer(Modifier.width(8.dp))

                        Text(
                            text = timeStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (!conversation.isRead) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            fontWeight = if (!conversation.isRead) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = conversation.snippet.ifBlank { "No message text" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (!conversation.isRead) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = if (!conversation.isRead) FontWeight.Medium else FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (!conversation.isRead) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(10.dp)
                        ) {}
                    }
                }
            }
        }
    }
}


