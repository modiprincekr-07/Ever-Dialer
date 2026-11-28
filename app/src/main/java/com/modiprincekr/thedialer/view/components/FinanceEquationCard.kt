package com.modiprincekr.thedialer.view.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.modiprincekr.thedialer.controller.util.FinanceLedgerRepository
import com.modiprincekr.thedialer.modal.data.DailyHistoryRecord
import com.modiprincekr.thedialer.modal.data.DailyLedgerState
import com.modiprincekr.thedialer.modal.data.FinanceTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val ColorDebitRed = Color(0xFFE53935)
private val ColorCreditGreen = Color(0xFF43A047)

@Composable
fun FinanceEquationCard(
    modifier: Modifier = Modifier,
    onTransactionClick: ((FinanceTransaction) -> Unit)? = null
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val ledgerState by FinanceLedgerRepository.dailyLedgerState.collectAsState()
    val isMasked by FinanceLedgerRepository.privacyMaskFlow.collectAsState()
    val selectedAccount by FinanceLedgerRepository.selectedAccountFlow.collectAsState()
    val historyList by FinanceLedgerRepository.historyFlow.collectAsState()

    var showSetOpeningDialog by remember { mutableStateOf(false) }
    var showCashEntryDialog by remember { mutableStateOf(false) }
    var showHistoryDialog by remember { mutableStateOf(false) }
    var selectedTransactionForDetails by remember { mutableStateOf<FinanceTransaction?>(null) }

    val isDark = androidx.core.graphics.ColorUtils.calculateLuminance(MaterialTheme.colorScheme.surface.toArgb()) < 0.5
    val cardBackground = if (isDark) Color(0xFF16181D) else Color(0xFFF4F6FA)
    val cardBorder = if (isDark) Color(0xFF262A33) else Color(0xFFE3E7EF)

    val openingFormatted = remember(ledgerState.openingBalance, isMasked) {
        if (isMasked) "••••••" else FinanceLedgerRepository.formatBalance(ledgerState.openingBalance)
    }
    val closingFormatted = remember(ledgerState.closingBalance, isMasked) {
        if (isMasked) "••••••" else FinanceLedgerRepository.formatBalance(ledgerState.closingBalance)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .border(1.dp, cardBorder, RoundedCornerShape(20.dp))
            .background(cardBackground, RoundedCornerShape(20.dp))
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Card Top Header ──────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left: Today (00:00 am) subtitle
                Column {
                    Text(
                        text = "Today (00:00 am)",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Right: Action buttons (Privacy, Manual Cash, History)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Privacy Mask Toggle
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            FinanceLedgerRepository.togglePrivacyMask(context)
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isMasked) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (isMasked) "Unmask amounts" else "Mask amounts",
                            tint = if (isMasked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Manual Cash Button (+ / -)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showCashEntryDialog = true
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Payments,
                                contentDescription = "Manual Cash",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Cash ±",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    // History Button
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            showHistoryDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.History,
                            contentDescription = "Daily Balance History",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // ── Account Filter Chips ─────────────────────────────────────────
            if (ledgerState.accounts.size > 1) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ledgerState.accounts.forEach { account ->
                        val isSelected = selectedAccount == account
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else if (isDark) Color(0xFF232730)
                                    else Color(0xFFE9EDF5)
                                )
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    FinanceLedgerRepository.selectAccount(context, account)
                                }
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = account,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ── Live Equation Tape Display ───────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color(0xFF0F1115) else Color(0xFFECEFF5))
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                if (ledgerState.entries.isEmpty()) {
                    // When no transactions today yet, show opening balance
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                showSetOpeningDialog = true
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "$openingFormatted ${ledgerState.currency}",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Tap to set opening",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                } else {
                    // Running Equation Tape
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Opening Balance
                        Text(
                            text = openingFormatted,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    showSetOpeningDialog = true
                                }
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )

                        // Delta Terms
                        ledgerState.entries.forEach { entry ->
                            val deltaFormatted = remember(entry.amount, isMasked) {
                                if (isMasked) "•••" else FinanceLedgerRepository.formatDelta(entry.amount)
                            }
                            val sign = if (entry.isDebit) "-" else "+"
                            val color = if (entry.isDebit) ColorDebitRed else ColorCreditGreen

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        onTransactionClick?.invoke(entry)
                                        selectedTransactionForDetails = entry
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "$sign $deltaFormatted",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = color
                                )
                            }
                        }
                    }
                }
            }

            // ── Live Result Line ─────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "= $closingFormatted",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = ledgerState.currency,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                Text(
                    text = "Till 00:00 am",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
        }
    }

    // ── Dialog: Set Opening Balance ──────────────────────────────────────────
    if (showSetOpeningDialog) {
        SetOpeningBalanceDialog(
            currentBalance = ledgerState.openingBalance,
            currency = ledgerState.currency,
            account = selectedAccount,
            onDismiss = { showSetOpeningDialog = false },
            onSave = { newBalance ->
                FinanceLedgerRepository.setOpeningBalance(context, newBalance, selectedAccount)
                showSetOpeningDialog = false
            }
        )
    }

    // ── Dialog: Manual Cash Entry ────────────────────────────────────────────
    if (showCashEntryDialog) {
        ManualCashEntryDialog(
            currency = ledgerState.currency,
            onDismiss = { showCashEntryDialog = false },
            onSave = { amount, isDebit, note ->
                FinanceLedgerRepository.addManualCashEntry(context, amount, isDebit, note, ledgerState.currency)
                showCashEntryDialog = false
            }
        )
    }

    // ── Dialog: Daily Balance History ────────────────────────────────────────
    if (showHistoryDialog) {
        DailyHistoryDialog(
            historyList = historyList,
            currency = ledgerState.currency,
            isMasked = isMasked,
            onDismiss = { showHistoryDialog = false }
        )
    }

    // ── Dialog: Transaction Details ──────────────────────────────────────────
    selectedTransactionForDetails?.let { tx ->
        TransactionDetailsDialog(
            transaction = tx,
            onDismiss = { selectedTransactionForDetails = null },
            onDelete = if (tx.isManualCash) {
                {
                    FinanceLedgerRepository.deleteManualCashEntry(context, tx.id)
                    selectedTransactionForDetails = null
                }
            } else null
        )
    }
}

@Composable
private fun SetOpeningBalanceDialog(
    currentBalance: Double,
    currency: String,
    account: String,
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit
) {
    var textValue by remember {
        mutableStateOf(if (currentBalance > 0.0) FinanceLedgerRepository.formatBalance(currentBalance) else "")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Set Opening Balance")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Calibrate starting balance for today (00:00 am) for $account:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { textValue = it },
                    label = { Text("Opening Balance ($currency)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = textValue.trim().replace(",", "")
                    val parsed = clean.toDoubleOrNull() ?: 0.0
                    onSave(parsed)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun ManualCashEntryDialog(
    currency: String,
    onDismiss: () -> Unit,
    onSave: (amount: Double, isDebit: Boolean, note: String) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var isDebit by remember { mutableStateOf(true) }
    var noteText by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = "Log Cash Transaction")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Direction Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = isDebit,
                        onClick = { isDebit = true },
                        label = { Text("- Debit (Spent)") },
                        leadingIcon = {
                            Icon(Icons.Default.Remove, contentDescription = null, tint = ColorDebitRed)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorDebitRed.copy(alpha = 0.15f),
                            selectedLabelColor = ColorDebitRed
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !isDebit,
                        onClick = { isDebit = false },
                        label = { Text("+ Credit (Recv)") },
                        leadingIcon = {
                            Icon(Icons.Default.Add, contentDescription = null, tint = ColorCreditGreen)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ColorCreditGreen.copy(alpha = 0.15f),
                            selectedLabelColor = ColorCreditGreen
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (isError) isError = false
                    },
                    label = { Text("Amount ($currency) *") },
                    singleLine = true,
                    isError = isError,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                // Quick Note
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note / Merchant (e.g. Groceries)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.trim().replace(",", "").toDoubleOrNull()
                    if (amount == null || amount <= 0.0) {
                        isError = true
                    } else {
                        onSave(amount, isDebit, noteText.trim())
                    }
                }
            ) {
                Text("Log Entry")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun DailyHistoryDialog(
    historyList: List<DailyHistoryRecord>,
    currency: String,
    isMasked: Boolean,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text("Daily Balance History")
            }
        },
        text = {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "No history yet",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Past daily ledgers automatically archive here every midnight (00:00 am).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(historyList, key = { it.dateKey }) { record ->
                        val openingStr = if (isMasked) "••••••" else FinanceLedgerRepository.formatBalance(record.openingBalance)
                        val closingStr = if (isMasked) "••••••" else FinanceLedgerRepository.formatBalance(record.closingBalance)
                        val tapeStr = if (isMasked) "•••••• = ••••••" else record.equationTape

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(12.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = record.displayDate,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "= $closingStr ${record.currency}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    text = "Opening: $openingStr ${record.currency} • ${record.entriesCount} entries",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = tapeStr,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun TransactionDetailsDialog(
    transaction: FinanceTransaction,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val dateStr = remember(transaction.timestamp) {
        SimpleDateFormat("MMM d, yyyy • hh:mm a", Locale.getDefault()).format(Date(transaction.timestamp))
    }
    val amountFormatted = remember(transaction.amount) {
        FinanceLedgerRepository.formatBalance(transaction.amount)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (transaction.isDebit) "Debit Transaction" else "Credit Transaction")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "${if (transaction.isDebit) "-" else "+"} $amountFormatted ${transaction.currency}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (transaction.isDebit) ColorDebitRed else ColorCreditGreen
                )
                if (transaction.merchantOrNote.isNotBlank()) {
                    Text(
                        text = "Merchant / Note: ${transaction.merchantOrNote}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Text(
                    text = "Account: ${transaction.accountLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Time: $dateStr",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!transaction.utrOrRef.isNullOrBlank()) {
                    Text(
                        text = "Ref / UTR: ${transaction.utrOrRef}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.availableBalance != null) {
                    Text(
                        text = "Reported Avl Bal: ${FinanceLedgerRepository.formatBalance(transaction.availableBalance)} ${transaction.currency}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (transaction.isManualCash) {
                    Text(
                        text = "(Manual Offline Cash Entry)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        },
        dismissButton = if (onDelete != null) {
            {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            }
        } else null
    )
}
