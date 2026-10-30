package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.BudgetHealth
import com.example.data.CategoryEntity
import com.example.data.PaymentMethod
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionModal(
    isWideScreen: Boolean,
    initialType: TransactionType = TransactionType.EXPENSE,
    editingTransaction: TransactionEntity? = null,
    categories: List<CategoryEntity>,
    summary: MonthlyFinancialSummary?,
    learnedKeywords: Map<String, String>,
    currencySymbol: String = "₹",
    dateFormatPattern: String = "dd MMM yyyy",
    showDecimals: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        title: String,
        note: String,
        dateMillis: Long,
        paymentMethod: PaymentMethod
    ) -> Unit
) {
    if (isWideScreen) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 3.dp,
                shadowElevation = 12.dp,
                modifier = Modifier
                    .widthIn(max = 540.dp)
                    .fillMaxWidth(0.92f)
                    .padding(vertical = 24.dp)
            ) {
                AddTransactionFormContent(
                    initialType = initialType,
                    editingTransaction = editingTransaction,
                    categories = categories,
                    summary = summary,
                    learnedKeywords = learnedKeywords,
                    currencySymbol = currencySymbol,
                    dateFormatPattern = dateFormatPattern,
                    showDecimals = showDecimals,
                    onDismiss = onDismiss,
                    onSave = onSave
                )
            }
        }
    } else {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            AddTransactionFormContent(
                initialType = initialType,
                editingTransaction = editingTransaction,
                categories = categories,
                summary = summary,
                learnedKeywords = learnedKeywords,
                currencySymbol = currencySymbol,
                dateFormatPattern = dateFormatPattern,
                showDecimals = showDecimals,
                onDismiss = onDismiss,
                onSave = onSave
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AddTransactionFormContent(
    initialType: TransactionType,
    editingTransaction: TransactionEntity?,
    categories: List<CategoryEntity>,
    summary: MonthlyFinancialSummary?,
    learnedKeywords: Map<String, String>,
    currencySymbol: String,
    dateFormatPattern: String,
    showDecimals: Boolean,
    onDismiss: () -> Unit,
    onSave: (
        existingId: Long,
        amount: Double,
        type: TransactionType,
        categoryId: String,
        title: String,
        note: String,
        dateMillis: Long,
        paymentMethod: PaymentMethod
    ) -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val ext = LocalSaathiColors.current

    var selectedType by remember(editingTransaction, initialType) {
        mutableStateOf(
            editingTransaction?.let {
                runCatching { TransactionType.valueOf(it.type) }.getOrDefault(TransactionType.EXPENSE)
            } ?: initialType
        )
    }

    var quickMode by remember(editingTransaction) {
        mutableStateOf(editingTransaction == null && initialType == TransactionType.EXPENSE)
    }

    var amountText by remember(editingTransaction) {
        mutableStateOf(
            if (editingTransaction != null && editingTransaction.amount > 0) {
                if (editingTransaction.amount % 1.0 == 0.0) {
                    editingTransaction.amount.toLong().toString()
                } else {
                    editingTransaction.amount.toString()
                }
            } else ""
        )
    }

    var titleText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.title ?: "")
    }

    var noteText by remember(editingTransaction) {
        mutableStateOf(editingTransaction?.note ?: "")
    }

    val defaultCatId = categories.firstOrNull { it.id == "groceries" }?.id
        ?: categories.firstOrNull()?.id
        ?: "groceries"

    var selectedCategoryId by remember(editingTransaction, categories) {
        mutableStateOf(editingTransaction?.categoryId ?: defaultCatId)
    }

    var userManuallyPickedCategory by remember(editingTransaction) {
        mutableStateOf(editingTransaction != null)
    }

    var selectedDateMillis by remember(editingTransaction) {
        mutableLongStateOf(editingTransaction?.dateMillis ?: System.currentTimeMillis())
    }

    var selectedPayment by remember(editingTransaction) {
        mutableStateOf(
            editingTransaction?.let {
                runCatching { PaymentMethod.valueOf(it.paymentMethod) }.getOrDefault(PaymentMethod.UPI)
            } ?: PaymentMethod.UPI
        )
    }

    var showDatePicker by remember { mutableStateOf(false) }
    var showSavedFeedback by remember { mutableStateOf(false) }

    val availableIds = remember(categories) { categories.map { it.id }.toSet() }
    val suggestedCatId = remember(titleText, noteText, learnedKeywords, availableIds) {
        FinanceUtils.suggestCategoryForInput(
            titleOrNote = "$titleText $noteText",
            learnedKeywordMap = learnedKeywords,
            availableCategoryIds = availableIds
        )
    }

    LaunchedEffect(suggestedCatId) {
        if (suggestedCatId != null && !userManuallyPickedCategory && selectedType == TransactionType.EXPENSE) {
            selectedCategoryId = suggestedCatId
        }
    }

    val parsedAmount = amountText.replace(",", "").trim().toDoubleOrNull() ?: 0.0

    val categorySummary = remember(summary, selectedCategoryId) {
        summary?.categorySummaries?.find { it.category.id == selectedCategoryId }
    }

    val previousSameCatAmount = if (
        editingTransaction != null &&
        editingTransaction.type == TransactionType.EXPENSE.name &&
        editingTransaction.categoryId == selectedCategoryId
    ) {
        editingTransaction.amount
    } else 0.0

    val remainingBeforeSave = (categorySummary?.remainingCategoryBudget ?: 0.0) + previousSameCatAmount
    val remainingAfterSave = remainingBeforeSave - parsedAmount
    val hasCategoryBudget = (categorySummary?.plannedAmount ?: 0.0) > 0.0
    val exceedsCategoryBudget = selectedType == TransactionType.EXPENSE &&
        hasCategoryBudget &&
        parsedAmount > 0.0 &&
        remainingAfterSave < -0.01

    LaunchedEffect(showSavedFeedback) {
        if (showSavedFeedback) {
            delay(360)
            onDismiss()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = when {
                    editingTransaction != null -> "Edit Entry"
                    selectedType == TransactionType.EXPENSE -> "Add Expense"
                    else -> "Add Income"
                },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (editingTransaction == null && selectedType == TransactionType.EXPENSE) {
                    Text(
                        text = "Quick",
                        style = MaterialTheme.typography.labelMedium,
                        color = ext.textSecondary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = quickMode,
                        onCheckedChange = { quickMode = it },
                        modifier = Modifier.testTag("quick_add_mode_switch")
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.testTag("close_transaction_sheet_button")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close"
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        val targetAmountColor = when {
            selectedType == TransactionType.EXPENSE -> ext.expenseRed
            selectedType == TransactionType.INCENTIVE_EXPECTED -> ext.warning
            else -> ext.incomeGreen
        }
        val targetAmountContainer = when {
            selectedType == TransactionType.EXPENSE -> ext.expenseRedContainer
            selectedType == TransactionType.INCENTIVE_EXPECTED -> ext.warningContainer
            else -> ext.incomeGreenContainer
        }
        val smoothAmountAccent by animateColorAsState(
            targetValue = targetAmountColor,
            animationSpec = tween(260),
            label = "smooth_tx_amount_accent"
        )
        val smoothAmountContainer by animateColorAsState(
            targetValue = targetAmountContainer,
            animationSpec = tween(260),
            label = "smooth_tx_amount_container"
        )

        // Always-Accessible Smooth Expense (Red) vs Income (Green) Switcher
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(ext.surfaceSubtle)
                .padding(5.dp)
        ) {
            val isExpenseSelected = selectedType == TransactionType.EXPENSE
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    selectedType = TransactionType.EXPENSE
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isExpenseSelected) ext.expenseRedContainer else Color.Transparent,
                border = if (isExpenseSelected) BorderStroke(1.dp, ext.expenseRed.copy(alpha = 0.45f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("mode_pill_expense")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        tint = if (isExpenseSelected) ext.expenseRed else ext.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Expense",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isExpenseSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isExpenseSelected) ext.expenseRed else ext.textSecondary
                    )
                }
            }

            val isIncomeSelected = selectedType != TransactionType.EXPENSE
            Surface(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (selectedType == TransactionType.EXPENSE) {
                        selectedType = TransactionType.SALARY
                    }
                },
                shape = RoundedCornerShape(12.dp),
                color = if (isIncomeSelected) ext.incomeGreenContainer else Color.Transparent,
                border = if (isIncomeSelected) BorderStroke(1.dp, ext.incomeGreen.copy(alpha = 0.45f)) else null,
                modifier = Modifier
                    .weight(1f)
                    .defaultMinSize(minHeight = 44.dp)
                    .testTag("mode_pill_income")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = null,
                        tint = if (isIncomeSelected) ext.incomeGreen else ext.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Income",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isIncomeSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isIncomeSelected) ext.incomeGreen else ext.textSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Income Sub-Type Selector (or Full Mode Type Chips)
        if (!quickMode || selectedType != TransactionType.EXPENSE) {
            val subTypes = if (selectedType != TransactionType.EXPENSE) {
                TransactionType.entries.filter { it != TransactionType.EXPENSE }
            } else {
                TransactionType.entries
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                subTypes.forEach { typeOption ->
                    FilterChip(
                        selected = selectedType == typeOption,
                        onClick = { selectedType = typeOption },
                        label = { Text(typeOption.label, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = smoothAmountContainer,
                            selectedLabelColor = smoothAmountAccent
                        ),
                        modifier = Modifier.testTag("tx_type_${typeOption.name.lowercase()}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Prominent Smooth Red / Smooth Green Amount Input + Quick Increment Pills
        val currencyLabel = FinanceUtils.currencyLabel(currencySymbol)
        val amountSign = when {
            selectedType == TransactionType.EXPENSE -> "-"
            selectedType == TransactionType.INCENTIVE_EXPECTED -> "~"
            else -> "+"
        }
        OutlinedTextField(
            value = amountText,
            onValueChange = { raw ->
                val filtered = raw.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    amountText = filtered
                }
            },
            label = {
                Text(
                    if (selectedType == TransactionType.EXPENSE) {
                        "Expense Amount ($currencyLabel)"
                    } else {
                        "Income Amount ($currencyLabel)"
                    }
                )
            },
            placeholder = {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                    color = smoothAmountAccent.copy(alpha = 0.40f)
                )
            },
            leadingIcon = {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(start = 6.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(smoothAmountAccent.copy(alpha = 0.14f))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$amountSign$currencyLabel",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = smoothAmountAccent,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            },
            textStyle = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                color = smoothAmountAccent
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = smoothAmountContainer.copy(alpha = 0.50f),
                unfocusedContainerColor = smoothAmountContainer.copy(alpha = 0.32f),
                focusedBorderColor = smoothAmountAccent,
                unfocusedBorderColor = smoothAmountAccent.copy(alpha = 0.45f),
                focusedLabelColor = smoothAmountAccent,
                unfocusedLabelColor = smoothAmountAccent.copy(alpha = 0.85f),
                cursorColor = smoothAmountAccent
            ),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_amount_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val increments = if (selectedType.isIncome || selectedType == TransactionType.INCENTIVE_EXPECTED) {
                listOf(1000, 5000, 10000, 25000)
            } else {
                listOf(100, 250, 500, 1000)
            }
            increments.forEach { stepVal ->
                AssistChip(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        val updated = parsedAmount + stepVal
                        amountText = if (updated % 1.0 == 0.0) {
                            updated.toLong().toString()
                        } else {
                            updated.toString()
                        }
                    },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = smoothAmountContainer.copy(alpha = 0.55f),
                        labelColor = smoothAmountAccent
                    ),
                    border = BorderStroke(1.dp, smoothAmountAccent.copy(alpha = 0.25f)),
                    label = {
                        Text(
                            text = "+${FinanceUtils.formatRupee(stepVal.toDouble(), false, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = titleText,
            onValueChange = { titleText = it },
            label = {
                Text(
                    if (selectedType == TransactionType.EXPENSE) "Title (optional)" else "Source (optional)"
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transaction_title_input")
        )

        if (selectedType == TransactionType.EXPENSE && suggestedCatId != null) {
            val suggestedName = categories.find { it.id == suggestedCatId }?.name
            if (suggestedName != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.65f))
                        .clickable {
                            selectedCategoryId = suggestedCatId
                            userManuallyPickedCategory = true
                        }
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Matched: $suggestedName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        if (selectedType == TransactionType.EXPENSE) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Category",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategoryId == cat.id
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            selectedCategoryId = cat.id
                            userManuallyPickedCategory = true
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = categoryIconForKey(cat.iconKey),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(cat.name, style = MaterialTheme.typography.labelMedium) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("category_chip_${cat.id}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (hasCategoryBudget && categorySummary != null) {
                val boxBg = if (exceedsCategoryBudget) ext.warningContainer else ext.surfaceSubtle
                val accentColor = if (exceedsCategoryBudget) ext.warning else MaterialTheme.colorScheme.primary
                val plannedCat = categorySummary.plannedAmount.coerceAtLeast(1.0)
                val postSpent = (categorySummary.actualSpent - previousSameCatAmount + parsedAmount).coerceAtLeast(0.0)
                val postHealth = when {
                    exceedsCategoryBudget -> BudgetHealth.OVERSPENT
                    (postSpent / plannedCat) >= 0.80 -> BudgetHealth.APPROACHING
                    else -> BudgetHealth.HEALTHY
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(boxBg)
                        .border(1.dp, accentColor.copy(alpha = 0.28f), RoundedCornerShape(16.dp))
                        .padding(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (exceedsCategoryBudget) Icons.Outlined.WarningAmber else Icons.Outlined.Info,
                            contentDescription = if (exceedsCategoryBudget) "Budget limit notice" else "Budget info",
                            tint = accentColor,
                            modifier = Modifier.size(19.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${categorySummary.category.name}: ${FinanceUtils.formatRupee(remainingBeforeSave, showDecimals, currencySymbol)} left",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    SmoothBudgetProgressBar(
                        progressFraction = (postSpent / plannedCat).toFloat(),
                        health = postHealth,
                        height = 6.dp
                    )
                    if (exceedsCategoryBudget) {
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Over budget by ${FinanceUtils.formatRupee(-remainingAfterSave, showDecimals, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.warning
                        )
                    } else if (parsedAmount > 0.0) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "After save: ${FinanceUtils.formatRupee(remainingAfterSave, showDecimals, currencySymbol)} left",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                    }
                }
            }
        } else if (selectedType == TransactionType.INCENTIVE_EXPECTED) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(ext.surfaceSubtle)
                    .padding(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Tracked separately until received.",
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary
                )
            }
        }

        if (!quickMode) {
            Spacer(modifier = Modifier.height(14.dp))

            SaathiCard(
                onClick = { showDatePicker = true },
                cornerRadius = 16.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_date_selector")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = "Select Date",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Transaction Date",
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                        Text(
                            text = FinanceUtils.formatDate(selectedDateMillis, dateFormatPattern),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(6.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                PaymentMethod.entries.forEach { method ->
                    FilterChip(
                        selected = selectedPayment == method,
                        onClick = { selectedPayment = method },
                        label = { Text(method.label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.testTag("payment_method_${method.name.lowercase()}")
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = { Text("Optional Note") },
                singleLine = false,
                maxLines = 2,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_note_input")
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        AnimatedVisibility(
            visible = showSavedFeedback,
            enter = fadeIn() + scaleIn(
                initialScale = 0.92f,
                animationSpec = spring(dampingRatio = 0.7f, stiffness = Spring.StiffnessMedium)
            ),
            exit = fadeOut()
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(ext.healthyContainer)
                    .padding(vertical = 15.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(ext.healthy)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "Saved",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Saved!",
                    style = MaterialTheme.typography.titleMedium,
                    color = ext.healthy,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        if (!showSavedFeedback) {
            Button(
                onClick = {
                    if (parsedAmount > 0.0) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSave(
                            editingTransaction?.id ?: 0L,
                            parsedAmount,
                            selectedType,
                            if (selectedType == TransactionType.EXPENSE) selectedCategoryId else "income",
                            titleText,
                            noteText,
                            selectedDateMillis,
                            selectedPayment
                        )
                        showSavedFeedback = true
                    }
                },
                enabled = parsedAmount > 0.0,
                colors = ButtonDefaults.buttonColors(
                    containerColor = smoothAmountAccent,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("save_transaction_button")
            ) {
                Text(
                    text = when {
                        editingTransaction != null -> "Update Entry"
                        selectedType == TransactionType.EXPENSE -> "Save Expense"
                        else -> "Save Income"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) {
                    Text("Select")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}
