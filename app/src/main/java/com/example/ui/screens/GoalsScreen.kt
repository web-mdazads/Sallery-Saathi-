package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BudgetHealth
import com.example.data.GoalEntity
import com.example.data.UserPreferences
import com.example.ui.components.AnimatedRupeeText
import com.example.ui.components.CircularGoalProgressRing
import com.example.ui.components.SaathiCard
import com.example.ui.components.SmoothBudgetProgressBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils

@Composable
fun GoalsScreen(
    preferences: UserPreferences,
    goals: List<GoalEntity>,
    onSaveGoal: (GoalEntity) -> Unit
) {
    val ext = LocalSaathiColors.current
    val haptic = LocalHapticFeedback.current
    val currency = preferences.currencySymbol

    val currencyLabel = FinanceUtils.currencyLabel(currency)
    val showDecimals = preferences.showDecimalsInLists

    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var recordSavingsGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var showNewGoalDialog by remember { mutableStateOf(false) }

    val totalSaved = goals.sumOf { it.savedAmount }
    val totalTargets = goals.sumOf { it.targetAmount }
    val completedCount = goals.count { it.targetAmount > 0 && it.savedAmount >= it.targetAmount }

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier
                .widthIn(max = 760.dp)
                .fillMaxWidth()
        ) {
            item {
                StaggeredEntrance(index = 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Goals",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
                        Button(
                            onClick = { showNewGoalDialog = true },
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("add_custom_goal_button")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Goal", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Overall Luxury Goals Hero Card
            item {
                StaggeredEntrance(index = 1) {
                    SaathiCard(
                        containerColor = Color.Transparent,
                        borderColor = ext.heroGlowAccent.copy(alpha = 0.28f),
                        cornerRadius = 26.dp,
                        shadowElevation = 6.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                                    )
                                )
                                .drawBehind {
                                    drawCircle(
                                        brush = Brush.radialGradient(
                                            colors = listOf(ext.heroGlowAccent.copy(alpha = 0.24f), Color.Transparent),
                                            center = Offset(size.width * 0.85f, size.height * 0.2f),
                                            radius = size.minDimension * 0.72f
                                        )
                                    )
                                }
                                .padding(20.dp)
                        ) {
                            Column {
                                Row(
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "Total Saved",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = Color.White.copy(alpha = 0.80f),
                                            maxLines = 1
                                        )
                                        AnimatedRupeeText(
                                            amount = totalSaved,
                                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                            showDecimals = showDecimals,
                                            currencySymbol = currency
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(horizontalAlignment = Alignment.End) {
                                        Text(
                                            text = "Target",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color.White.copy(alpha = 0.76f),
                                            maxLines = 1
                                        )
                                        Text(
                                            text = FinanceUtils.formatRupee(totalTargets, showDecimals, currency),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$completedCount/${goals.size} done",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = Color(0xFF6EE7B7),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            softWrap = false
                                        )
                                    }
                                }
                                if (totalTargets > 0) {
                                    Spacer(modifier = Modifier.height(14.dp))
                                    SmoothBudgetProgressBar(
                                        progressFraction = (totalSaved / totalTargets).toFloat(),
                                        health = BudgetHealth.HEALTHY,
                                        height = 8.dp
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Individual Goal Cards with Circular Progress Rings & Quick Chips
            itemsIndexed(goals, key = { _, it -> it.id }) { idx, goal ->
                StaggeredEntrance(index = (idx + 2).coerceAtMost(8)) {
                    GoalItemCard(
                        goal = goal,
                        currencySymbol = currency,
                        showDecimals = showDecimals,
                        onEditClick = { editingGoal = goal },
                        onAddSavingsClick = { recordSavingsGoal = goal },
                        onQuickAddDelta = { delta ->
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSaveGoal(goal.copy(savedAmount = goal.savedAmount + delta))
                        }
                    )
                }
            }
        }
    }

    // Edit Goal Modal
    if (editingGoal != null) {
        val g = editingGoal!!
        var titleStr by remember(g) { mutableStateOf(g.title) }
        var targetStr by remember(g) { mutableStateOf(if (g.targetAmount > 0) g.targetAmount.toLong().toString() else "") }
        var savedStr by remember(g) { mutableStateOf(if (g.savedAmount > 0) g.savedAmount.toLong().toString() else "0") }

        AlertDialog(
            onDismissRequest = { editingGoal = null },
            title = { Text("Edit ${g.title}") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = titleStr,
                        onValueChange = { titleStr = it },
                        label = { Text("Goal Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = targetStr,
                        onValueChange = { targetStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Amount ($currencyLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_goal_target_input")
                    )
                    OutlinedTextField(
                        value = savedStr,
                        onValueChange = { savedStr = it.filter { c -> c.isDigit() } },
                        label = { Text("Saved ($currencyLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("edit_goal_saved_input")
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveGoal(
                            g.copy(
                                title = titleStr.trim().ifBlank { g.title },
                                targetAmount = targetStr.toDoubleOrNull() ?: 0.0,
                                savedAmount = savedStr.toDoubleOrNull() ?: 0.0
                            )
                        )
                        editingGoal = null
                    }
                ) {
                    Text("Save Goal")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Record Manual Savings Addition Dialog
    if (recordSavingsGoal != null) {
        val g = recordSavingsGoal!!
        var addStr by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { recordSavingsGoal = null },
            title = { Text("Add Savings • ${g.title}") },
            text = {
                OutlinedTextField(
                    value = addStr,
                    onValueChange = { addStr = it.filter { c -> c.isDigit() } },
                    label = { Text("Amount ($currencyLabel)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val delta = addStr.toDoubleOrNull() ?: 0.0
                        if (delta > 0.0) {
                            onSaveGoal(g.copy(savedAmount = g.savedAmount + delta))
                        }
                        recordSavingsGoal = null
                    }
                ) {
                    Text("Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { recordSavingsGoal = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Add Custom Goal Dialog
    if (showNewGoalDialog) {
        var newTitle by remember { mutableStateOf("") }
        var newTarget by remember { mutableStateOf("") }
        var newSaved by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewGoalDialog = false },
            title = { Text("Create Savings Goal") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("Goal Title (e.g., Sister's Wedding, Laptop)") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newTarget,
                        onValueChange = { newTarget = it.filter { c -> c.isDigit() } },
                        label = { Text("Target Amount ($currencyLabel)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newSaved,
                        onValueChange = { newSaved = it.filter { c -> c.isDigit() } },
                        label = { Text("Already Saved ($currencyLabel, optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newTitle.isNotBlank()) {
                            onSaveGoal(
                                GoalEntity(
                                    id = "goal_custom_${System.currentTimeMillis()}",
                                    title = newTitle.trim(),
                                    typeKey = "future_savings",
                                    targetAmount = newTarget.toDoubleOrNull() ?: 0.0,
                                    savedAmount = newSaved.toDoubleOrNull() ?: 0.0,
                                    iconKey = "savings",
                                    subtitle = "Personal goal"
                                )
                            )
                            showNewGoalDialog = false
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GoalItemCard(
    goal: GoalEntity,
    currencySymbol: String,
    showDecimals: Boolean = false,
    onEditClick: () -> Unit,
    onAddSavingsClick: () -> Unit,
    onQuickAddDelta: (Double) -> Unit
) {
    val ext = LocalSaathiColors.current
    val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    val progressPct = if (goal.targetAmount > 0.0) {
        (goal.savedAmount / goal.targetAmount) * 100.0
    } else 0.0
    val progressFraction = (progressPct / 100.0).toFloat().coerceIn(0f, 1f)

    val (milestoneLabel, milestoneIcon) = when {
        goal.targetAmount <= 0.0 -> "Set Target" to Icons.Outlined.TrendingUp
        progressPct >= 100.0 -> "Goal Reached!" to Icons.Outlined.CheckCircle
        progressPct >= 75.0 -> "Almost There" to Icons.Outlined.AutoAwesome
        progressPct >= 50.0 -> "Halfway+" to Icons.Outlined.TrendingUp
        progressPct >= 25.0 -> "25% Milestone" to Icons.Outlined.TrendingUp
        else -> "Building" to Icons.Outlined.TrendingUp
    }

    SaathiCard(
        containerColor = ext.surfaceElevated,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    CircularGoalProgressRing(
                        progressFraction = progressFraction,
                        ringSize = 62.dp,
                        strokeWidth = 5.5.dp,
                        accentColor = if (progressPct >= 100.0) ext.healthy else MaterialTheme.colorScheme.primary
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f))
                        ) {
                            Icon(
                                imageVector = categoryIconForKey(goal.iconKey),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = goal.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (goal.subtitle.isNotBlank()) {
                            Text(
                                text = goal.subtitle,
                                style = MaterialTheme.typography.bodySmall,
                                color = ext.textSecondary
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (progressPct >= 100.0) ext.healthyContainer else ext.surfaceSubtle
                                )
                                .padding(horizontal = 9.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = milestoneIcon,
                                contentDescription = null,
                                tint = if (progressPct >= 100.0) ext.healthy else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = milestoneLabel,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (progressPct >= 100.0) ext.healthy else MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.testTag("edit_goal_${goal.id}")
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = "Edit ${goal.title}",
                        tint = ext.textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(ext.surfaceSubtle.copy(alpha = 0.65f))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1
                    )
                    AnimatedRupeeText(
                        amount = goal.savedAmount,
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = ext.healthy,
                        showDecimals = showDecimals,
                        currencySymbol = currencySymbol
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Target",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = if (goal.targetAmount > 0) {
                            FinanceUtils.formatRupee(goal.targetAmount, showDecimals, currencySymbol)
                        } else {
                            "Set target"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (goal.targetAmount > 0) "${FinanceUtils.formatPercentage(progressPct)} done" else "Remaining",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                    Text(
                        text = if (goal.targetAmount > 0) {
                            "${FinanceUtils.formatRupee(remaining, showDecimals, currencySymbol)} left"
                        } else "Tap Edit",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick One-Tap Manual Savings Chips
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                listOf(500, 1000, 2500).forEach { quickAmt ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f))
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), RoundedCornerShape(50))
                            .clickable { onQuickAddDelta(quickAmt.toDouble()) }
                            .padding(horizontal = 11.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "+${FinanceUtils.formatRupee(quickAmt.toDouble(), false, currencySymbol)}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(
                    onClick = onEditClick,
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (goal.targetAmount <= 0) "Set Target" else "Edit",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Button(
                    onClick = onAddSavingsClick,
                    shape = RoundedCornerShape(13.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("record_saving_${goal.id}")
                ) {
                    Text(
                        text = "+ Save",
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
