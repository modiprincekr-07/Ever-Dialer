package com.modiprincekr.thedialer.view.screen.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.outlined.Dialpad
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Lens
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.modiprincekr.thedialer.controller.ContactsViewModel
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.modal.data.Contact
import com.modiprincekr.thedialer.view.components.RivoAnimatedSection
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.RivoListItem
import com.modiprincekr.thedialer.view.components.RivoSwitchListItem
import com.modiprincekr.thedialer.view.components.SelectExistingContactDialog
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.components.rivoHeadingColor
import com.modiprincekr.thedialer.view.components.settingsSearchHighlight
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel

private val ColorBlue   = Color(0xFF2196F3)
private val ColorTeal   = Color(0xFF009688)
private val ColorAmber  = Color(0xFFFFC107)
private val ColorPurple = Color(0xFF9C27B0)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun DialpadSettingsScreen(
    navigator: DestinationsNavigator,
    highlightKey: String? = null
) {
    val prefs = koinInject<PreferenceManager>()
    val contactsVM: ContactsViewModel = koinActivityViewModel()
    val allContacts by contactsVM.allContacts.collectAsState()
    val settingsState by prefs.settingsChanged.collectAsState()
    var highlightedKey by remember { mutableStateOf(highlightKey) }

    var dialpadMemory by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, true)) }
    var showCallLogsInDialpadSearchList by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, true)) }
    var openDialpadDefault by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, false)) }
    var circleButtons by remember { mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, false)) }

    var showSpeedDialManagerDialog by remember { mutableStateOf(false) }
    var selectedKeyToAssign by remember { mutableStateOf<String?>(null) }
    var showContactPicker by remember { mutableStateOf(false) }
    var pendingContactForNumberChoice by remember { mutableStateOf<Contact?>(null) }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Dialpad",
                onBackClick = { navigator.navigateUp() }
            )
        }
    ) { padding ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            RivoAnimatedSection(delayMs = 0L) {
                Column {
                    Text(
                        "Dialpad Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Dialpad Memory",
                            supporting = "Keep the typed number in the Dialpad after closing it or calling",
                            leadingIcon = Icons.Outlined.Dialpad,
                            iconContainerColor = ColorBlue,
                            checked = dialpadMemory,
                            modifier = Modifier.settingsSearchHighlight("dialpad_memory", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                dialpadMemory = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_MEMORY, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Show call logs in the dialpad search list",
                            supporting = "Show recent call logs in the search list when nothing is typed or searched",
                            leadingIcon = Icons.Outlined.History,
                            iconContainerColor = ColorTeal,
                            checked = showCallLogsInDialpadSearchList,
                            modifier = Modifier.settingsSearchHighlight("show_call_logs_in_dialpad_search_list", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                showCallLogsInDialpadSearchList = it
                                prefs.setBoolean(PreferenceManager.KEY_SHOW_CALL_LOGS_IN_DIALPAD_SEARCH_LIST, it)
                            }
                        )
                        HorizontalDivider(
                            Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "Open Dialpad by Default",
                            supporting = "Show dialpad automatically when app starts",
                            leadingIcon = Icons.Outlined.Dialpad,
                            iconContainerColor = ColorAmber,
                            checked = openDialpadDefault,
                            modifier = Modifier.settingsSearchHighlight("open_dialpad_default", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                openDialpadDefault = it
                                prefs.setBoolean(PreferenceManager.KEY_OPEN_DIALPAD_DEFAULT, it)
                            }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 20L) {
                Column {
                    Text(
                        "Speed Dial",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoListItem(
                            headline = "Speed Dial (Keys 2–9)",
                            supporting = "Assign contacts to keys 2–9 for quick calling via long press",
                            leadingIcon = Icons.Outlined.Speed,
                            iconContainerColor = ColorTeal,
                            trailingIcon = Icons.Default.ChevronRight,
                            modifier = Modifier.settingsSearchHighlight("speed_dial", highlightedKey) { highlightedKey = null },
                            onClick = { showSpeedDialManagerDialog = true }
                        )
                    }
                }
            }

            RivoAnimatedSection(delayMs = 40L) {
                Column {
                    Text(
                        "Buttons Style",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "Circle buttons",
                            supporting = "Use circular buttons on the dialpad",
                            leadingIcon = Icons.Outlined.Lens,
                            iconContainerColor = ColorPurple,
                            checked = circleButtons,
                            modifier = Modifier.settingsSearchHighlight("circle_buttons", highlightedKey) { highlightedKey = null },
                            onCheckedChange = {
                                circleButtons = it
                                prefs.setBoolean(PreferenceManager.KEY_DIALPAD_CIRCLE_BUTTONS, it)
                            }
                        )
                    }
                }
            }
        }
    }

    if (showSpeedDialManagerDialog) {
        AlertDialog(
            onDismissRequest = { showSpeedDialManagerDialog = false },
            title = { Text("Speed Dial (Keys 2–9)") },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    (2..9).forEach { digit ->
                        val key = digit.toString()
                        val number = prefs.getSpeedDialNumber(key)
                        val name = prefs.getSpeedDialName(key)
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedKeyToAssign = key
                                    showContactPicker = true
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = key,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimary
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name ?: (number ?: "Not assigned"),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (number != null) FontWeight.Medium else FontWeight.Normal,
                                        color = if (number != null) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (number != null && name != null) {
                                        Text(
                                            text = number,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (number != null) {
                                    IconButton(
                                        onClick = { prefs.removeSpeedDial(key) }
                                    ) {
                                        Icon(
                                            Icons.Default.Clear,
                                            contentDescription = "Clear",
                                            tint = MaterialTheme.colorScheme.error
                                        )
                                    }
                                } else {
                                    IconButton(
                                        onClick = {
                                            selectedKeyToAssign = key
                                            showContactPicker = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.PersonAdd,
                                            contentDescription = "Assign",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showSpeedDialManagerDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    if (showContactPicker && selectedKeyToAssign != null) {
        val digit = selectedKeyToAssign!!
        SelectExistingContactDialog(
            contacts = allContacts,
            onSelectContact = { contact ->
                showContactPicker = false
                if (contact.phoneNumbers.size > 1) {
                    pendingContactForNumberChoice = contact
                } else {
                    val primaryNum = prefs.getContactDefaultNumber(contact.id)?.takeIf { it in contact.phoneNumbers }
                    val phone = primaryNum ?: contact.phoneNumbers.firstOrNull { it.isNotBlank() }
                    if (!phone.isNullOrBlank()) {
                        prefs.setSpeedDial(digit, phone, contact.name)
                    }
                    selectedKeyToAssign = null
                }
            },
            onDismiss = {
                showContactPicker = false
                selectedKeyToAssign = null
            }
        )
    }

    if (pendingContactForNumberChoice != null && selectedKeyToAssign != null) {
        val contact = pendingContactForNumberChoice!!
        val digit = selectedKeyToAssign!!
        AlertDialog(
            onDismissRequest = {
                pendingContactForNumberChoice = null
                selectedKeyToAssign = null
            },
            title = { Text("Choose Number for ${contact.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    contact.phoneNumbers.forEach { numOption ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pendingContactForNumberChoice = null
                                    prefs.setSpeedDial(digit, numOption, contact.name)
                                    selectedKeyToAssign = null
                                }
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = numOption,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.padding(14.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = {
                        pendingContactForNumberChoice = null
                        selectedKeyToAssign = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}
