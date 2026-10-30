package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ai.AiCategoryDraft
import com.example.data.BudgetHealth
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import com.example.viewmodel.AiPlannerUiState
import kotlin.math.abs

val SurplusPriorityChoices = listOf(
    "Emergency fund",
    "Future savings",
    "Family",
    "New phone or gadget",
    "Bike or car",
    "Travel",
    "Personal spending"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiBudgetPlannerModal(
    monthlyIncome: Double,
    paydayDay: Int,
    aiOptInEnabled: Boolean,
    currencySymbol: String,
    showDecimals: Boolean = false,
    plannerState: AiPlannerUiState,
    onToggleAiOptIn: (Boolean) -> Unit,
    onUpdateSalaryIfZero: (Double) -> Unit,
    onGenerateProposal: (
        income: Double,
        rent: Double,
        family: Double,
        emi: Double,
        bills: Double,
        surplusPriority: String,
        useGeminiAi: Boolean
    ) -> Unit,
    onUpdateDraft: (categoryId: String, newAmount: Double?, isIncluded: Boolean?) -> Unit,
    onAcceptAllSuggestions: () -> Unit,
    onConfirmSaveBudget: (List<AiCategoryDraft>) -> Unit,
    onDismiss: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val currencyLabel = FinanceUtils.currencyLabel(currencySymbol)

    var incomeInput by remember(monthlyIncome) {
        mutableStateOf(if (monthlyIncome > 0) monthlyIncome.toLong().toString() else "")
    }
    var rentInput by remember { mutableStateOf("") }
    var familyInput by remember { mutableStateOf("") }
    var emiInput by remember { mutableStateOf("") }
    var billsInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf(SurplusPriorityChoices.first()) }
    var showSetupInputs by remember(plannerState) {
        mutableStateOf(plannerState !is AiPlannerUiState.Ready)
    }

    val effectiveIncome = incomeInput.replace(",", "").toDoubleOrNull() ?: monthlyIncome

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = MaterialTheme.colorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .imePadding()
            ) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .widthIn(max = 640.dp)
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    item {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Budget Planner",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Payday: Day $paydayDay",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = ext.textSecondary
                                )
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("close_budget_planner_button")
                            ) {
                                Icon(Icons.Outlined.Close, contentDescription = "Close")
                            }
                        }
                    }

                    // Step 1: Obligations & Surplus Priority Card
                    item {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Income & Fixed Bills",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (plannerState is AiPlannerUiState.Ready) {
                                        TextButton(onClick = { showSetupInputs = !showSetupInputs }) {
                                            Text(if (showSetupInputs) "Hide" else "Edit")
                                        }
                                    }
                                }

                                if (showSetupInputs) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    OutlinedTextField(
                                        value = incomeInput,
                                        onValueChange = { incomeInput = it.filter { c -> c.isDigit() } },
                                        label = { Text("Salary + Incentives ($currencyLabel)") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("planner_income_input")
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = rentInput,
                                            onValueChange = { rentInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Rent / PG ($currencyLabel)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = familyInput,
                                            onValueChange = { familyInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Family Support ($currencyLabel)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                        OutlinedTextField(
                                            value = emiInput,
                                            onValueChange = { emiInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Existing EMI ($currencyLabel)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                        OutlinedTextField(
                                            value = billsInput,
                                            onValueChange = { billsInput = it.filter { c -> c.isDigit() } },
                                            label = { Text("Fixed Bills ($currencyLabel)") },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            shape = RoundedCornerShape(14.dp),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Text(
                                        text = "Surplus Priority",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SurplusPriorityChoices.forEach { option ->
                                            FilterChip(
                                                selected = selectedPriority == option,
                                                onClick = { selectedPriority = option },
                                                shape = RoundedCornerShape(50),
                                                label = { Text(option, style = MaterialTheme.typography.labelMedium) },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                                )
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(ext.surfaceSubtle)
                                            .padding(12.dp)
                                    ) {
                                        Text(
                                            text = "AI Personalization",
                                            style = MaterialTheme.typography.labelLarge,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Switch(
                                            checked = aiOptInEnabled,
                                            onCheckedChange = onToggleAiOptIn,
                                            modifier = Modifier.testTag("planner_ai_opt_in_switch")
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Button(
                                            onClick = {
                                                if (effectiveIncome > 0 && monthlyIncome <= 0) {
                                                    onUpdateSalaryIfZero(effectiveIncome)
                                                }
                                                if (!aiOptInEnabled) {
                                                    onToggleAiOptIn(true)
                                                }
                                                onGenerateProposal(
                                                    effectiveIncome,
                                                    rentInput.toDoubleOrNull() ?: 0.0,
                                                    familyInput.toDoubleOrNull() ?: 0.0,
                                                    emiInput.toDoubleOrNull() ?: 0.0,
                                                    billsInput.toDoubleOrNull() ?: 0.0,
                                                    selectedPriority,
                                                    true
                                                )
                                                showSetupInputs = false
                                            },
                                            enabled = effectiveIncome > 0 && plannerState !is AiPlannerUiState.Generating,
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("generate_ai_budget_button")
                                        ) {
                                            Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("AI Suggest", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                if (effectiveIncome > 0 && monthlyIncome <= 0) {
                                                    onUpdateSalaryIfZero(effectiveIncome)
                                                }
                                                onGenerateProposal(
                                                    effectiveIncome,
                                                    rentInput.toDoubleOrNull() ?: 0.0,
                                                    familyInput.toDoubleOrNull() ?: 0.0,
                                                    emiInput.toDoubleOrNull() ?: 0.0,
                                                    billsInput.toDoubleOrNull() ?: 0.0,
                                                    selectedPriority,
                                                    false
                                                )
                                                showSetupInputs = false
                                            },
                                            enabled = effectiveIncome > 0 && plannerState !is AiPlannerUiState.Generating,
                                            shape = RoundedCornerShape(14.dp),
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(48.dp)
                                                .testTag("generate_offline_budget_button")
                                        ) {
                                            Text("Smart Formula", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Loading or Error State
                    when (plannerState) {
                        is AiPlannerUiState.Generating -> {
                            item {
                                SaathiCard(
                                    containerColor = ext.surfaceElevated,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(20.dp)
                                    ) {
                                        CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.5.dp)
                                        Spacer(modifier = Modifier.width(14.dp))
                                        Text(
                                            text = "Building plan...",
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                }
                            }
                        }

                        is AiPlannerUiState.Error -> {
                            item {
                                SaathiCard(
                                    containerColor = ext.warningContainer,
                                    borderColor = ext.warning.copy(alpha = 0.4f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = ext.warning)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "AI Unavailable",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = ext.warning
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = plannerState.message,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Spacer(modifier = Modifier.height(10.dp))
                                        OutlinedButton(
                                            onClick = {
                                                onGenerateProposal(
                                                    effectiveIncome,
                                                    rentInput.toDoubleOrNull() ?: 0.0,
                                                    familyInput.toDoubleOrNull() ?: 0.0,
                                                    emiInput.toDoubleOrNull() ?: 0.0,
                                                    billsInput.toDoubleOrNull() ?: 0.0,
                                                    selectedPriority,
                                                    false
                                                )
                                            }
                                        ) {
                                            Text("Use Smart Formula")
                                        }
                                    }
                                }
                            }
                        }

                        is AiPlannerUiState.Ready -> {
                            val proposal = plannerState.proposal
                            val totalPlanned = proposal.drafts
                                .filter { it.isIncluded }
                                .sumOf { it.editedAmount }
                            val diff = effectiveIncome - totalPlanned
                            val isOverIncome = effectiveIncome > 0 && diff < -0.5
                            val isUnderAllocated = effectiveIncome > 0 && diff > 0.5

                            item {
                                SaathiCard(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(14.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = proposal.summaryNote,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }

                            // Visual Reconciliation Banner
                            item {
                                val bannerBg = when {
                                    isOverIncome -> ext.dangerContainer
                                    isUnderAllocated -> ext.surfaceElevated
                                    else -> ext.healthyContainer
                                }
                                val bannerAccent = when {
                                    isOverIncome -> ext.danger
                                    isUnderAllocated -> MaterialTheme.colorScheme.primary
                                    else -> ext.healthy
                                }
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(18.dp))
                                        .background(bannerBg)
                                        .border(1.dp, bannerAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
                                        .padding(14.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isOverIncome) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            tint = bannerAccent,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = "Planned: ${FinanceUtils.formatRupee(totalPlanned, showDecimals, currencySymbol)} of ${FinanceUtils.formatRupee(effectiveIncome, showDecimals, currencySymbol)}",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = when {
                                                    isOverIncome -> "Over by ${FinanceUtils.formatRupee(abs(diff), showDecimals, currencySymbol)}"
                                                    isUnderAllocated -> "Unallocated: ${FinanceUtils.formatRupee(diff, showDecimals, currencySymbol)}"
                                                    else -> "100% allocated"
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                color = if (isOverIncome) ext.danger else ext.textSecondary
                                            )
                                        }
                                    }
                                    if (effectiveIncome > 0) {
                                        Spacer(modifier = Modifier.height(10.dp))
                                        SmoothBudgetProgressBar(
                                            progressFraction = (totalPlanned / effectiveIncome).toFloat(),
                                            health = if (isOverIncome) BudgetHealth.OVERSPENT else BudgetHealth.HEALTHY,
                                            height = 8.dp
                                        )
                                    }
                                }
                            }

                            // Action row: Accept All & Regenerate
                            item {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedButton(
                                        onClick = onAcceptAllSuggestions,
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reset", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                    OutlinedButton(
                                        onClick = {
                                            onGenerateProposal(
                                                effectiveIncome,
                                                rentInput.toDoubleOrNull() ?: 0.0,
                                                familyInput.toDoubleOrNull() ?: 0.0,
                                                emiInput.toDoubleOrNull() ?: 0.0,
                                                billsInput.toDoubleOrNull() ?: 0.0,
                                                selectedPriority,
                                                aiOptInEnabled
                                            )
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Regenerate", maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }

                            // Editable Category Rows
                            items(proposal.drafts, key = { it.categoryId }) { draft ->
                                BudgetDraftRowCard(
                                    draft = draft,
                                    monthlyIncome = effectiveIncome,
                                    currencySymbol = currencySymbol,
                                    showDecimals = showDecimals,
                                    onAmountChange = { amt -> onUpdateDraft(draft.categoryId, amt, null) },
                                    onIncludedChange = { inc -> onUpdateDraft(draft.categoryId, null, inc) }
                                )
                            }

                            item {
                                Button(
                                    onClick = {
                                        if (effectiveIncome > 0 && monthlyIncome <= 0) {
                                            onUpdateSalaryIfZero(effectiveIncome)
                                        }
                                        onConfirmSaveBudget(proposal.drafts)
                                        onDismiss()
                                    },
                                    shape = RoundedCornerShape(16.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("confirm_save_budget_button")
                                ) {
                                    Text(
                                        text = "Save Budget",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(24.dp))
                            }
                        }

                        AiPlannerUiState.Idle -> Unit
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetDraftRowCard(
    draft: AiCategoryDraft,
    monthlyIncome: Double,
    currencySymbol: String,
    showDecimals: Boolean = false,
    onAmountChange: (Double) -> Unit,
    onIncludedChange: (Boolean) -> Unit
) {
    val ext = LocalSaathiColors.current
    val currencyLabel = FinanceUtils.currencyLabel(currencySymbol)
    var textValue by remember(draft.categoryId, draft.suggestedAmount) {
        mutableStateOf(
            if (draft.editedAmount > 0) draft.editedAmount.toLong().toString() else "0"
        )
    }

    if ((textValue.toDoubleOrNull() ?: 0.0) != draft.editedAmount && draft.editedAmount == draft.suggestedAmount) {
        textValue = draft.editedAmount.toLong().toString()
    }

    val pctOfIncome = if (monthlyIncome > 0 && draft.isIncluded) {
        (draft.editedAmount / monthlyIncome) * 100.0
    } else 0.0

    SaathiCard(
        containerColor = ext.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = draft.categoryName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Suggested: ${FinanceUtils.formatRupee(draft.suggestedAmount, showDecimals, currencySymbol)} • ${FinanceUtils.formatPercentage(pctOfIncome)} of income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Switch(
                    checked = draft.isIncluded,
                    onCheckedChange = onIncludedChange
                )
            }

            if (draft.reason.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = draft.reason,
                    style = MaterialTheme.typography.bodySmall,
                    color = ext.textSecondary
                )
            }

            if (draft.isIncluded) {
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = textValue,
                    onValueChange = { raw ->
                        val digits = raw.filter { it.isDigit() }
                        textValue = digits
                        onAmountChange(digits.toDoubleOrNull() ?: 0.0)
                    },
                    label = { Text("Planned Monthly Amount ($currencyLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
