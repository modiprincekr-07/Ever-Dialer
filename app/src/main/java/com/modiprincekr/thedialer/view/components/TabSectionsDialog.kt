package com.modiprincekr.thedialer.view.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Tab
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

data class TabSectionItem(val key: String, val label: String, val icon: ImageVector)

@Composable
fun TabSectionsDialog(
    onDismissRequest: () -> Unit
) {
    val prefs = koinInject<PreferenceManager>()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val rowHeightDp = 52.dp
    val rowHeightPx = with(density) { rowHeightDp.toPx() }
    var draggedKey by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableStateOf(0f) }

    var tabShowFavorites  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)) }
    var tabShowCalls      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)) }
    var tabShowContacts   by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)) }
    var tabShowSms        by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)) }
    var tabShowGroups     by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)) }
    var tabShowRecordings by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)) }
    var tabShowNotes      by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)) }
    var tabShowAlternate  by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_ALTERNATE,  true)) }
    var tabShowDialpad    by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)) }

    val tabOptions = remember {
        buildList {
            add(TabSectionItem("favorites",  "Favourites", Icons.Outlined.FavoriteBorder))
            add(TabSectionItem("calls",      "Calls",      Icons.Outlined.History))
            add(TabSectionItem("contacts",   "Contacts",   Icons.Outlined.Person))
            add(TabSectionItem("sms",        "SMS",        Icons.Outlined.Chat))
            add(TabSectionItem("groups",     "Groups",     Icons.Outlined.Group))
            if (com.coolappstore.everdialer.by.svhp.feature.AppFeatures.isCallRecorderAvailable) {
                add(TabSectionItem("recordings", "Recordings", Icons.Outlined.FiberManualRecord))
            }
            add(TabSectionItem("notes",      "Note",       Icons.Outlined.Note))
            add(TabSectionItem("alternate",  "Alternate",  Icons.Outlined.ContactPhone))
            add(TabSectionItem("dialpad",    "Dialpad",    Icons.Outlined.Dialpad))
        }
    }

    val tabOrder = remember {
        mutableStateListOf<String>().apply {
            val saved = prefs.getString(PreferenceManager.KEY_TAB_ORDER, null)
            val savedKeys = saved?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: emptyList()
            val validKeys = tabOptions.map { it.key }
            addAll(savedKeys.filter { it in validKeys })
            validKeys.forEach { key -> if (key !in this) add(key) }
        }
    }

    fun persistTabOrder() {
        prefs.setString(PreferenceManager.KEY_TAB_ORDER, tabOrder.joinToString(","))
    }

    fun tabChecked(key: String): Boolean = when (key) {
        "favorites"  -> tabShowFavorites
        "calls"      -> tabShowCalls
        "contacts"   -> tabShowContacts
        "sms"        -> tabShowSms
        "groups"     -> tabShowGroups
        "recordings" -> tabShowRecordings
        "notes"      -> tabShowNotes
        "alternate"  -> tabShowAlternate
        "dialpad"    -> tabShowDialpad
        else         -> true
    }

    fun setTabChecked(key: String, value: Boolean) {
        when (key) {
            "favorites"  -> { tabShowFavorites = value;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  value) }
            "calls"      -> { tabShowCalls = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      value) }
            "contacts"   -> { tabShowContacts = value;   prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   value) }
            "sms"        -> { tabShowSms = value;        prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        value) }
            "groups"     -> { tabShowGroups = value;     prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     value) }
            "recordings" -> { tabShowRecordings = value; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, value) }
            "notes"      -> { tabShowNotes = value;      prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      value) }
            "alternate"  -> { tabShowAlternate = value;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_ALTERNATE,  value) }
            "dialpad"    -> { tabShowDialpad = value;    prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    value) }
        }
    }

    fun resetTabSectionsToDefault() {
        val defaults = PreferenceManager.DEFAULT_TAB_ORDER.split(",").map { it.trim() }.filter { it.isNotBlank() }
        tabOrder.clear()
        tabOrder.addAll(defaults)
        persistTabOrder()
        tabShowFavorites  = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_FAVORITES,  true)
        tabShowCalls      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CALLS,      true)
        tabShowContacts   = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_CONTACTS,   true)
        tabShowSms        = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_SMS,        true)
        tabShowGroups     = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_GROUPS,     false)
        tabShowRecordings = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_RECORDINGS, true)
        tabShowNotes      = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_NOTES,      true)
        tabShowAlternate  = true;  prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_ALTERNATE,  true)
        tabShowDialpad    = false; prefs.setBoolean(PreferenceManager.KEY_TAB_SHOW_DIALPAD,    false)
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(Icons.Default.ViewWeek, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text("Tab Sections") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "Choose which tabs are visible, and drag the handle to reorder them in the navigation bar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                val maxListHeight = (LocalConfiguration.current.screenHeightDp.dp * 0.55f).coerceAtLeast(240.dp)
                val scrollState = rememberScrollState()
                val canScroll = scrollState.maxValue > 0

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = maxListHeight)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = if (canScroll) 10.dp else 0.dp)
                            .verticalScroll(scrollState)
                    ) {
                        tabOrder.forEach { tabKey ->
                            val option = tabOptions.firstOrNull { it.key == tabKey } ?: return@forEach
                            val isDragging = draggedKey == tabKey
                            key(tabKey) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (isDragging) MaterialTheme.colorScheme.surfaceContainerHighest else MaterialTheme.colorScheme.surfaceVariant,
                                    tonalElevation = if (isDragging) 8.dp else 0.dp,
                                    shadowElevation = if (isDragging) 6.dp else 0.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .zIndex(if (isDragging) 10f else 0f)
                                        .graphicsLayer {
                                            translationY = if (isDragging) dragOffsetY else 0f
                                            scaleX = if (isDragging) 1.02f else 1f
                                            scaleY = if (isDragging) 1.02f else 1f
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(rowHeightDp)
                                            .padding(horizontal = 16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = option.icon,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Text(option.label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                                        Checkbox(
                                            checked = tabChecked(tabKey),
                                            onCheckedChange = { setTabChecked(tabKey, it) },
                                            colors = CheckboxDefaults.colors(
                                                checkedColor = MaterialTheme.colorScheme.primary,
                                                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Icon(
                                            imageVector = Icons.Filled.DragHandle,
                                            contentDescription = "Reorder ${option.label}",
                                            tint = if (isDragging) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier
                                                .padding(start = 4.dp)
                                                .pointerInput(tabKey) {
                                                    detectDragGestures(
                                                        onDragStart = {
                                                            draggedKey = tabKey
                                                            dragOffsetY = 0f
                                                        },
                                                        onDragEnd = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                            persistTabOrder()
                                                        },
                                                        onDragCancel = {
                                                            draggedKey = null
                                                            dragOffsetY = 0f
                                                            persistTabOrder()
                                                        },
                                                        onDrag = { change, dragAmount ->
                                                            change.consume()
                                                            dragOffsetY += dragAmount.y
                                                            val currentIdx = tabOrder.indexOf(tabKey)
                                                            if (currentIdx != -1) {
                                                                val threshold = rowHeightPx * 0.6f
                                                                if (dragOffsetY > threshold && currentIdx < tabOrder.lastIndex) {
                                                                    val item = tabOrder.removeAt(currentIdx)
                                                                    tabOrder.add(currentIdx + 1, item)
                                                                    dragOffsetY -= rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                } else if (dragOffsetY < -threshold && currentIdx > 0) {
                                                                    val item = tabOrder.removeAt(currentIdx)
                                                                    tabOrder.add(currentIdx - 1, item)
                                                                    dragOffsetY += rowHeightPx
                                                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                                                }
                                                            }
                                                        }
                                                    )
                                                }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (canScroll) {
                        VerticalScrollIndicator(
                            scrollState = scrollState,
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text("Done") }
        },
        dismissButton = {
            TextButton(onClick = { resetTabSectionsToDefault() }) { Text("Default") }
        }
    )
}

@Composable
private fun VerticalScrollIndicator(
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    BoxWithConstraints(
        modifier = modifier.width(10.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        val totalHeight = maxHeight
        val viewport = scrollState.viewportSize.toFloat()
        val total = (scrollState.maxValue + scrollState.viewportSize).toFloat()
        val visibleRatio = if (total > 0f) (viewport / total).coerceIn(0.12f, 0.7f) else 0.3f
        val thumbHeight = (totalHeight * visibleRatio).coerceAtLeast(24.dp)
        val maxTravel = (totalHeight - thumbHeight).coerceAtLeast(0.dp)
        val scrollFraction = if (scrollState.maxValue > 0) {
            (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
        } else 0f

        val thumbOffset = maxTravel * scrollFraction
        val animatedOffset by animateDpAsState(
            targetValue = thumbOffset,
            animationSpec = spring(stiffness = Spring.StiffnessHigh),
            label = "vScrollThumbOffset"
        )

        val isDark = isSystemInDarkTheme()
        val trackColor = if (isDark) {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
        } else {
            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
        }
        val indicatorColor = MaterialTheme.colorScheme.primary

        // Track in pill style
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(4.dp)
                .clip(RoundedCornerShape(50))
                .background(trackColor)
                .pointerInput(scrollState.maxValue) {
                    detectTapGestures { tapOffset ->
                        if (scrollState.maxValue > 0 && size.height > 0) {
                            val targetFraction = (tapOffset.y / size.height.toFloat()).coerceIn(0f, 1f)
                            coroutineScope.launch {
                                scrollState.animateScrollTo((targetFraction * scrollState.maxValue).toInt())
                            }
                        }
                    }
                }
        ) {
            // Pill thumb indicator
            Box(
                modifier = Modifier
                    .offset(y = animatedOffset)
                    .width(4.dp)
                    .height(thumbHeight)
                    .clip(RoundedCornerShape(50))
                    .background(indicatorColor)
            )
        }
    }
}

