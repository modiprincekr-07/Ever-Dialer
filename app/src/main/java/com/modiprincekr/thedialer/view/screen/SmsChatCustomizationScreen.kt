package com.modiprincekr.thedialer.view.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.controller.util.SmsChatCustomizationManager
import com.modiprincekr.thedialer.view.components.ExpressiveAvatar
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.expressive.PfpShapeRegistry
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun SmsChatCustomizationScreen(
    navigator: DestinationsNavigator,
    address: String = "",
    threadId: Long = -1L,
    contactName: String? = null
) {
    val context = LocalContext.current
    val prefs = koinInject<PreferenceManager>()
    val settingsVer by prefs.settingsChanged.collectAsState()

    val isChatSpecific = address.isNotBlank()
    var applyGlobally by remember { mutableStateOf(!isChatSpecific) }
    val effectiveAddress = if (applyGlobally) "" else address

    // Customization states
    var wallpaperUri by remember(settingsVer, effectiveAddress) {
        mutableStateOf(SmsChatCustomizationManager.getWallpaperUri(prefs, effectiveAddress))
    }
    var bgColorInt by remember(settingsVer, effectiveAddress) {
        mutableStateOf(SmsChatCustomizationManager.getBackgroundColor(prefs, effectiveAddress))
    }
    var outgoingColorInt by remember(settingsVer, effectiveAddress) {
        mutableStateOf(SmsChatCustomizationManager.getOutgoingBubbleColor(prefs, effectiveAddress))
    }
    var incomingColorInt by remember(settingsVer, effectiveAddress) {
        mutableStateOf(SmsChatCustomizationManager.getIncomingBubbleColor(prefs, effectiveAddress))
    }
    var textSizeSp by remember(settingsVer, effectiveAddress) {
        mutableFloatStateOf(SmsChatCustomizationManager.getTextSize(prefs, effectiveAddress))
    }
    var pfpShapeKey by remember(settingsVer, effectiveAddress) {
        mutableStateOf(SmsChatCustomizationManager.getPfpShape(prefs, effectiveAddress))
    }

    // Photo picker launcher
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            val uriStr = uri.toString()
            wallpaperUri = uriStr
            SmsChatCustomizationManager.setWallpaperUri(prefs, effectiveAddress, uriStr)
            Toast.makeText(context, "Wallpaper updated", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isChatSpecific && !applyGlobally) "Chat Customization" else "SMS Chat Customization",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isChatSpecific && !applyGlobally) (contactName ?: address) else "Global Default",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigator.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            SmsChatCustomizationManager.resetChatCustomization(prefs, effectiveAddress)
                            wallpaperUri = null
                            bgColorInt = null
                            outgoingColorInt = null
                            incomingColorInt = null
                            textSizeSp = PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE
                            pfpShapeKey = prefs.getSelectedPfpShape()
                            Toast.makeText(context, "Customization reset to default", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset all")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Scope Switcher (only shown when entered from a specific chat)
            if (isChatSpecific) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !applyGlobally,
                            onClick = { applyGlobally = false },
                            label = { Text("This chat only", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (!applyGlobally) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = applyGlobally,
                            onClick = { applyGlobally = true },
                            label = { Text("All chats (Global)", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (applyGlobally) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // ── Live Chat Preview Card ──────────────────────────────────────────
            Text(
                text = "Live Chat Preview",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )

            LiveChatPreviewCard(
                wallpaperUri = wallpaperUri,
                bgColorInt = bgColorInt,
                outgoingColorInt = outgoingColorInt,
                incomingColorInt = incomingColorInt,
                textSizeSp = textSizeSp,
                pfpShapeKey = pfpShapeKey,
                contactName = if (isChatSpecific) (contactName ?: address) else "Alex Rivera"
            )

            // ── Section 1: Chat Wallpaper / Background ───────────────────────────
            CustomizationSectionHeader(
                title = "Chat Wallpaper & Background",
                icon = Icons.Outlined.Wallpaper
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Pick photo button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { photoLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Pick photo wallpaper", style = MaterialTheme.typography.labelMedium)
                        }

                        if (wallpaperUri != null || bgColorInt != null) {
                            IconButton(
                                onClick = {
                                    wallpaperUri = null
                                    bgColorInt = null
                                    SmsChatCustomizationManager.setWallpaperUri(prefs, effectiveAddress, null)
                                    SmsChatCustomizationManager.setBackgroundColor(prefs, effectiveAddress, null)
                                    Toast.makeText(context, "Background reset", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Icon(Icons.Outlined.Clear, contentDescription = "Clear background", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                    Text(
                        text = "Or choose solid color:",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Quick solid background swatches
                    val bgPresets = listOf(
                        0xFF121212.toInt(),
                        0xFF1A1C1E.toInt(),
                        0xFF1E1E2E.toInt(),
                        0xFF0F172A.toInt(),
                        0xFF162521.toInt(),
                        0xFF2A1B29.toInt(),
                        0xFFF8FAFC.toInt(),
                        0xFFF1F5F9.toInt(),
                        0xFFECEFF1.toInt(),
                        0xFFF5EBE1.toInt()
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bgPresets) { cInt ->
                            ColorSwatchCircle(
                                color = Color(cInt),
                                isSelected = bgColorInt == cInt && wallpaperUri == null,
                                onClick = {
                                    wallpaperUri = null
                                    bgColorInt = cInt
                                    SmsChatCustomizationManager.setWallpaperUri(prefs, effectiveAddress, null)
                                    SmsChatCustomizationManager.setBackgroundColor(prefs, effectiveAddress, cInt)
                                }
                            )
                        }
                    }

                    // RGB Color Picker Box for Background
                    RgbColorPickerBox(
                        initialColor = bgColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.surface,
                        onColorSelected = { selectedColor ->
                            wallpaperUri = null
                            val argb = selectedColor.toArgb()
                            bgColorInt = argb
                            SmsChatCustomizationManager.setWallpaperUri(prefs, effectiveAddress, null)
                            SmsChatCustomizationManager.setBackgroundColor(prefs, effectiveAddress, argb)
                        }
                    )
                }
            }

            // ── Section 2: Message Bubble Color ───────────────────────────────────
            CustomizationSectionHeader(
                title = "Message Bubble Colors",
                icon = Icons.Outlined.ColorLens
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                var bubbleTab by remember { mutableIntStateOf(0) } // 0 = Outgoing, 1 = Incoming

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Outgoing / Incoming Tab Selector
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = bubbleTab == 0,
                            onClick = { bubbleTab = 0 },
                            label = { Text("Outgoing bubble", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (bubbleTab == 0) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = bubbleTab == 1,
                            onClick = { bubbleTab = 1 },
                            label = { Text("Incoming bubble", style = MaterialTheme.typography.labelMedium) },
                            leadingIcon = if (bubbleTab == 1) {
                                { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    val currentColor = if (bubbleTab == 0) {
                        outgoingColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer
                    } else {
                        incomingColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant
                    }

                    // Reset button for current bubble
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (bubbleTab == 0) "Outgoing color palette" else "Incoming color palette",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        TextButton(
                            onClick = {
                                if (bubbleTab == 0) {
                                    outgoingColorInt = null
                                    SmsChatCustomizationManager.setOutgoingBubbleColor(prefs, effectiveAddress, null)
                                } else {
                                    incomingColorInt = null
                                    SmsChatCustomizationManager.setIncomingBubbleColor(prefs, effectiveAddress, null)
                                }
                                Toast.makeText(context, "Restored default theme color", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // Preset palette for bubbles
                    val bubblePresets = if (bubbleTab == 0) {
                        listOf(
                            0xFF1976D2.toInt(), 0xFF0288D1.toInt(), 0xFF00897B.toInt(),
                            0xFF388E3C.toInt(), 0xFF673AB7.toInt(), 0xFFE91E63.toInt(),
                            0xFFD32F2F.toInt(), 0xFFFF6F00.toInt(), 0xFF455A64.toInt()
                        )
                    } else {
                        listOf(
                            0xFF2C2C2E.toInt(), 0xFF3A3A3C.toInt(), 0xFF263238.toInt(),
                            0xFF374151.toInt(), 0xFFE2E8F0.toInt(), 0xFFECEFF1.toInt(),
                            0xFFF1F3F5.toInt(), 0xFFE5E7EB.toInt(), 0xFFEDE7F6.toInt()
                        )
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(bubblePresets) { cInt ->
                            val isSel = if (bubbleTab == 0) outgoingColorInt == cInt else incomingColorInt == cInt
                            ColorSwatchCircle(
                                color = Color(cInt),
                                isSelected = isSel,
                                onClick = {
                                    if (bubbleTab == 0) {
                                        outgoingColorInt = cInt
                                        SmsChatCustomizationManager.setOutgoingBubbleColor(prefs, effectiveAddress, cInt)
                                    } else {
                                        incomingColorInt = cInt
                                        SmsChatCustomizationManager.setIncomingBubbleColor(prefs, effectiveAddress, cInt)
                                    }
                                }
                            )
                        }
                    }

                    // RGB Color Picker Box for Bubbles
                    RgbColorPickerBox(
                        initialColor = currentColor,
                        onColorSelected = { selColor ->
                            val argb = selColor.toArgb()
                            if (bubbleTab == 0) {
                                outgoingColorInt = argb
                                SmsChatCustomizationManager.setOutgoingBubbleColor(prefs, effectiveAddress, argb)
                            } else {
                                incomingColorInt = argb
                                SmsChatCustomizationManager.setIncomingBubbleColor(prefs, effectiveAddress, argb)
                            }
                        }
                    )
                }
            }

            // ── Section 3: Chat Text Size ─────────────────────────────────────────
            CustomizationSectionHeader(
                title = "Chat Text Size",
                icon = Icons.Outlined.FormatSize
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Font Size: ${textSizeSp.roundToInt()} sp",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = {
                                textSizeSp = PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE
                                SmsChatCustomizationManager.setTextSize(prefs, effectiveAddress, null)
                            }
                        ) {
                            Text("Reset (${PreferenceManager.DEFAULT_SMS_CHAT_TEXT_SIZE.roundToInt()} sp)", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    Slider(
                        value = textSizeSp,
                        onValueChange = {
                            textSizeSp = it
                            SmsChatCustomizationManager.setTextSize(prefs, effectiveAddress, it)
                        },
                        valueRange = 12f..26f,
                        steps = 13,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("12 sp (Small)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("16 sp (Default)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("26 sp (Large)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Section 4: Animated PFP Shapes ────────────────────────────────────
            CustomizationSectionHeader(
                title = "Animated Contact PFP Shapes",
                icon = Icons.Outlined.Animation
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose from 32 animated shapes:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = {
                                pfpShapeKey = prefs.getSelectedPfpShape()
                                SmsChatCustomizationManager.setPfpShape(prefs, effectiveAddress, null)
                                Toast.makeText(context, "PFP shape reset", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Reset", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    // PFP Shapes Horizontal Flow / List
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(PfpShapeRegistry.allShapes) { shapeItem ->
                            val isSelected = pfpShapeKey == shapeItem.key
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable {
                                        pfpShapeKey = shapeItem.key
                                        SmsChatCustomizationManager.setPfpShape(prefs, effectiveAddress, shapeItem.key)
                                    }
                                    .padding(6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                            else MaterialTheme.colorScheme.surfaceVariant
                                        )
                                        .border(
                                            width = if (isSelected) 2.dp else 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ExpressiveAvatar(
                                        name = "A",
                                        shapeKey = shapeItem.key,
                                        size = 38.dp,
                                        obeySolidIcons = false
                                    )
                                }
                                Text(
                                    text = shapeItem.label,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

// ── Components ──────────────────────────────────────────────────────────────────

@Composable
private fun CustomizationSectionHeader(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun LiveChatPreviewCard(
    wallpaperUri: String?,
    bgColorInt: Int?,
    outgoingColorInt: Int?,
    incomingColorInt: Int?,
    textSizeSp: Float,
    pfpShapeKey: String,
    contactName: String
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        color = bgColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Wallpaper background if set
            if (!wallpaperUri.isNullOrBlank()) {
                AsyncImage(
                    model = Uri.parse(wallpaperUri),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Mini Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.85f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExpressiveAvatar(
                        name = contactName,
                        shapeKey = pfpShapeKey,
                        size = 32.dp,
                        obeySolidIcons = false
                    )
                    Column {
                        Text(
                            text = contactName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Mobile • Active now",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Chat Bubbles
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Incoming Bubble
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 4.dp,
                                bottomEnd = 16.dp
                            ),
                            color = incomingColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.widthIn(max = 240.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Hey! How does this chat style look?",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = textSizeSp.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "10:30 AM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }

                    // Outgoing Bubble
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Surface(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 4.dp
                            ),
                            color = outgoingColorInt?.let { Color(it) } ?: MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.widthIn(max = 240.dp)
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                Text(
                                    text = "Looks sleek, modern & readable! ✨",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = textSizeSp.sp),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "10:31 AM • Sent",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.65f),
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorSwatchCircle(
    color: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                shape = CircleShape
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}

@Composable
private fun RgbColorPickerBox(
    initialColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var red by remember(initialColor) { mutableFloatStateOf(initialColor.red * 255f) }
    var green by remember(initialColor) { mutableFloatStateOf(initialColor.green * 255f) }
    var blue by remember(initialColor) { mutableFloatStateOf(initialColor.blue * 255f) }

    val activeColor = remember(red, green, blue) {
        Color(red.roundToInt() / 255f, green.roundToInt() / 255f, blue.roundToInt() / 255f)
    }

    val hexString = remember(red, green, blue) {
        String.format("#%02X%02X%02X", red.roundToInt(), green.roundToInt(), blue.roundToInt())
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Swatch preview
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(activeColor)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(8.dp))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "RGB Sliders ($hexString)",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "R: ${red.roundToInt()}  G: ${green.roundToInt()}  B: ${blue.roundToInt()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Button(
                    onClick = { onColorSelected(activeColor) },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Apply", style = MaterialTheme.typography.labelSmall)
                }
            }

            // Red Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("R", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFFEF5350), modifier = Modifier.width(20.dp))
                Slider(
                    value = red,
                    onValueChange = { red = it },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Green Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("G", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF66BB6A), modifier = Modifier.width(20.dp))
                Slider(
                    value = green,
                    onValueChange = { green = it },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
            }

            // Blue Slider
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("B", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF42A5F5), modifier = Modifier.width(20.dp))
                Slider(
                    value = blue,
                    onValueChange = { blue = it },
                    valueRange = 0f..255f,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
