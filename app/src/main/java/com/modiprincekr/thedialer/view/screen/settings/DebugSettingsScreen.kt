package com.modiprincekr.thedialer.view.screen.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Contacts
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.modiprincekr.thedialer.controller.util.PreferenceManager
import com.modiprincekr.thedialer.view.components.RivoAnimatedSection
import com.modiprincekr.thedialer.view.components.RivoExpressiveCard
import com.modiprincekr.thedialer.view.components.RivoSwitchListItem
import com.modiprincekr.thedialer.view.components.SettingsPillTopAppBar
import com.modiprincekr.thedialer.view.components.rivoHeadingColor
import com.modiprincekr.thedialer.view.theme.SettingsTransitionStyle
import com.modiprincekr.thedialer.view.theme.settingsMotionBlur
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.annotation.RootGraph
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import org.koin.compose.koinInject

private val ColorOrange = Color(0xFFFF9800)
private val ColorBlue   = Color(0xFF2196F3)

@OptIn(ExperimentalMaterial3Api::class)
@Destination<RootGraph>(style = SettingsTransitionStyle::class)
@Composable
fun DebugSettingsScreen(
    navigator: DestinationsNavigator
) {
    val prefs = koinInject<PreferenceManager>()
    var newCallLogsLogic by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, false))
    }
    var newContactsLogic by remember {
        mutableStateOf(prefs.getBoolean(PreferenceManager.KEY_NEW_CONTACTS_LOGIC, false))
    }

    Scaffold(
        modifier = Modifier.settingsMotionBlur(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            SettingsPillTopAppBar(
                title = "Debug",
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
                        "Experimental Options",
                        style = MaterialTheme.typography.labelLarge,
                        color = rivoHeadingColor(),
                        modifier = Modifier.padding(start = 12.dp, bottom = 8.dp)
                    )
                    RivoExpressiveCard {
                        RivoSwitchListItem(
                            headline = "New call logs logic",
                            supporting = "High-performance call log engine from v11.0.0: loads full call history with fast-path cursor reading, item diffing, and zero-lag scrolling",
                            leadingIcon = Icons.Outlined.History,
                            iconContainerColor = ColorOrange,
                            checked = newCallLogsLogic,
                            onCheckedChange = {
                                newCallLogsLogic = it
                                prefs.setBoolean(PreferenceManager.KEY_NEW_CALL_LOGS_LOGIC, it)
                            }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )
                        RivoSwitchListItem(
                            headline = "New contacts logic",
                            supporting = "High-performance contacts engine from v11.0.0: instant startup from disk cache, lightweight group loading, and background sync",
                            leadingIcon = Icons.Outlined.Contacts,
                            iconContainerColor = ColorBlue,
                            checked = newContactsLogic,
                            onCheckedChange = {
                                newContactsLogic = it
                                prefs.setBoolean(PreferenceManager.KEY_NEW_CONTACTS_LOGIC, it)
                            }
                        )
                    }
                }
            }
        }
    }
}
