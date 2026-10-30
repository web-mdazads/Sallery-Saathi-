package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.BudgetHealth
import com.example.data.UserPreferences
import com.example.ui.components.AnimatedRupeeText
import com.example.ui.components.HealthStatusBadge
import com.example.ui.components.InteractiveAllocationDonutChart
import com.example.ui.components.SaathiCard
import com.example.ui.components.SmoothBudgetProgressBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalSaathiColors
import com.example.util.CategoryAllocationSummary
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import kotlin.math.abs

@Composable
fun BudgetScreen(
    preferences: UserPreferences,
    summary: MonthlyFinancialSummary?,
    availableCycleKeys: List<String>,
    selectedCycleKey: String,
    onSelectCycleKey: (String) -> Unit,
    onOpenAiBudgetSetup: () -> Unit,
    onEditCurrentBudget: () -> Unit,
    onAddExpense: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val currency = preferences.currencySymbol

    val showDecimals = preferences.showDecimalsInLists

    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var showVisualChart by remember { mutableStateOf(true) }

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
            // Header
            item {
                StaggeredEntrance(index = 0) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Budget",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = summary?.cycleInfo?.monthTitle ?: "Monthly Cycle",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onEditCurrentBudget,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("budget_edit_button")
                            ) {
                                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Edit", maxLines = 1, softWrap = false)
                            }
                            Button(
                                onClick = onOpenAiBudgetSetup,
                                shape = RoundedCornerShape(14.dp),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                modifier = Modifier.testTag("budget_ai_plan_button")
                            ) {
                                Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("AI Plan", maxLines = 1, softWrap = false)
                            }
                        }
                    }
                }
            }

            // Monthly History Selector
            if (availableCycleKeys.isNotEmpty()) {
                item {
                    StaggeredEntrance(index = 1) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(ext.surfaceSubtle)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.History,
                                    contentDescription = "Monthly history",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            availableCycleKeys.forEach { cycle ->
                                FilterChip(
                                    selected = cycle == selectedCycleKey,
                                    onClick = { onSelectCycleKey(cycle) },
                                    label = {
                                        Text(
                                            text = cycle,
                                            fontWeight = if (cycle == selectedCycleKey) FontWeight.Bold else FontWeight.Medium
                                        )
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            if (summary != null) {
                val remainingFromPlanned = summary.totalPlannedAllocated - summary.totalExpenses
                val overallHealth = when {
                    summary.totalPlannedAllocated > 0 && summary.totalExpenses > summary.totalPlannedAllocated -> BudgetHealth.OVERSPENT
                    summary.totalPlannedAllocated > 0 && (summary.totalExpenses / summary.totalPlannedAllocated) >= 0.85 -> BudgetHealth.APPROACHING
                    else -> BudgetHealth.HEALTHY
                }
                val overallProgress = if (summary.totalPlannedAllocated > 0) {
                    (summary.totalExpenses / summary.totalPlannedAllocated).toFloat()
                } else 0f

                // Luxury Layered Planned vs Spent Hero Card
                item {
                    StaggeredEntrance(index = 2) {
                        PlannedVsSpentHeroCard(
                            summary = summary,
                            remainingFromPlanned = remainingFromPlanned,
                            overallHealth = overallHealth,
                            overallProgress = overallProgress,
                            currencySymbol = currency,
                            showDecimals = showDecimals
                        )
                    }
                }

                // Interactive Allocation Chart Section (when budget has planned categories)
                if (summary.totalPlannedAllocated > 0.0) {
                    item {
                        StaggeredEntrance(index = 3) {
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
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.DonutLarge,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Allocation",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        TextButton(onClick = { showVisualChart = !showVisualChart }) {
                                            Icon(
                                                imageVector = if (showVisualChart) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(if (showVisualChart) "Hide" else "Show", maxLines = 1, softWrap = false)
                                        }
                                    }

                                    AnimatedVisibility(
                                        visible = showVisualChart,
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Column {
                                            Spacer(modifier = Modifier.height(10.dp))
                                            InteractiveAllocationDonutChart(
                                                totalIncome = summary.totalReceivedIncome,
                                                totalAllocated = summary.totalPlannedAllocated,
                                                unallocatedAmount = summary.unallocatedAmount,
                                                categories = summary.categorySummaries,
                                                currencySymbol = currency,
                                                showDecimals = showDecimals,
                                                selectedCategoryId = selectedCategoryId,
                                                onSelectCategoryId = { selectedCategoryId = it }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Overspending Warnings Banner (if any)
                val overspentList = summary.categorySummaries.filter { it.health == BudgetHealth.OVERSPENT }
                if (overspentList.isNotEmpty()) {
                    item {
                        SaathiCard(
                            containerColor = ext.dangerContainer,
                            borderColor = ext.danger.copy(alpha = 0.35f),
                            onClick = onEditCurrentBudget,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(14.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.WarningAmber,
                                    contentDescription = "Overspending warning",
                                    tint = ext.danger,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${overspentList.size} over budget",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = ext.danger
                                    )
                                    Text(
                                        text = "Tap to adjust",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }

                // Empty Budget Call-to-action if nothing allocated yet
                if (summary.totalPlannedAllocated <= 0.0) {
                    item {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(22.dp)
                            ) {
                                Text(
                                    text = "No budget set",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = onOpenAiBudgetSetup,
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Plan Budget")
                                }
                            }
                        }
                    }
                }

                // Category Progress Cards
                itemsIndexed(summary.categorySummaries, key = { _, it -> it.category.id }) { idx, catItem ->
                    StaggeredEntrance(index = (idx + 4).coerceAtMost(8)) {
                        BudgetCategoryDetailCard(
                            item = catItem,
                            isHighlighted = selectedCategoryId == catItem.category.id,
                            currencySymbol = currency,
                            showDecimals = showDecimals,
                            onClick = {
                                selectedCategoryId = if (selectedCategoryId == catItem.category.id) null else catItem.category.id
                            }
                        )
                    }
                }

                item {
                    OutlinedButton(
                        onClick = onAddExpense,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Expense", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun PlannedVsSpentHeroCard(
    summary: MonthlyFinancialSummary,
    remainingFromPlanned: Double,
    overallHealth: BudgetHealth,
    overallProgress: Float,
    currencySymbol: String,
    showDecimals: Boolean = false
) {
    val ext = LocalSaathiColors.current
    val plannedSample = FinanceUtils.formatRupee(summary.totalPlannedAllocated, showDecimals, currencySymbol)
    val metricStyle = if (plannedSample.length > 9) {
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
    } else {
        MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
    }
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
                        colors = listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
                    )
                )
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(ext.heroGlowAccent.copy(alpha = 0.25f), Color.Transparent),
                            center = Offset(size.width * 0.88f, size.height * 0.18f),
                            radius = size.minDimension * 0.75f
                        )
                    )
                }
                .padding(20.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Overview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (summary.totalPlannedAllocated > 0) {
                        HealthStatusBadge(health = overallHealth)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Planned",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1
                        )
                        AnimatedRupeeText(
                            amount = summary.totalPlannedAllocated,
                            style = metricStyle,
                            color = Color.White,
                            showDecimals = showDecimals,
                            currencySymbol = currencySymbol
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1
                        )
                        AnimatedRupeeText(
                            amount = summary.totalExpenses,
                            style = metricStyle,
                            color = Color.White,
                            showDecimals = showDecimals,
                            currencySymbol = currencySymbol
                        )
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = if (remainingFromPlanned >= 0) "Left" else "Over",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.76f),
                            maxLines = 1
                        )
                        AnimatedRupeeText(
                            amount = abs(remainingFromPlanned),
                            style = metricStyle,
                            color = if (remainingFromPlanned < 0) Color(0xFFFCA5A5) else Color(0xFF6EE7B7),
                            showDecimals = showDecimals,
                            currencySymbol = currencySymbol
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                SmoothBudgetProgressBar(
                    progressFraction = overallProgress,
                    health = overallHealth,
                    height = 9.dp
                )

                if (summary.unallocatedAmount > 0.5 && summary.totalReceivedIncome > 0) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.12f))
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF6EE7B7),
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Unallocated: ${FinanceUtils.formatRupee(summary.unallocatedAmount, showDecimals, currencySymbol)}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.92f),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetCategoryDetailCard(
    item: CategoryAllocationSummary,
    isHighlighted: Boolean,
    currencySymbol: String,
    showDecimals: Boolean = false,
    onClick: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val catColor = ChartPalette[item.category.colorIndex % ChartPalette.size]
    val progress = if (item.plannedAmount > 0.0) {
        (item.actualSpent / item.plannedAmount).toFloat()
    } else if (item.actualSpent > 0.0) 1f else 0f

    SaathiCard(
        containerColor = if (isHighlighted) catColor.copy(alpha = 0.08f) else ext.surfaceElevated,
        borderColor = if (isHighlighted) catColor else ext.cardBorder,
        shadowElevation = if (isHighlighted) 5.dp else 2.dp,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(13.dp))
                            .background(catColor.copy(alpha = 0.16f))
                    ) {
                        Icon(
                            imageVector = categoryIconForKey(item.category.iconKey),
                            contentDescription = null,
                            tint = catColor,
                            modifier = Modifier.size(21.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = item.category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (item.plannedAmount > 0) {
                                "${FinanceUtils.formatPercentage(item.percentOfCategoryBudget)} used"
                            } else {
                                "No limit"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = ext.textSecondary
                        )
                    }
                }

                HealthStatusBadge(health = item.health)
            }

            Spacer(modifier = Modifier.height(10.dp))

            SmoothBudgetProgressBar(
                progressFraction = progress,
                health = item.health,
                height = 7.dp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Plan: ${FinanceUtils.formatRupee(item.plannedAmount, showDecimals, currencySymbol)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = ext.textSecondary,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                Text(
                    text = "Spent: ${FinanceUtils.formatRupee(item.actualSpent, showDecimals, currencySymbol)}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis
                )

                val isNeg = item.remainingCategoryBudget < 0
                Text(
                    text = if (isNeg) {
                        "Over ${FinanceUtils.formatRupee(abs(item.remainingCategoryBudget), showDecimals, currencySymbol)}"
                    } else {
                        "${FinanceUtils.formatRupee(item.remainingCategoryBudget, showDecimals, currencySymbol)} left"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isNeg) ext.danger else ext.healthy,
                    maxLines = 1,
                    softWrap = false
                )
            }

            if (isHighlighted && item.aiReason.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = ext.cardBorder.copy(alpha = 0.6f))
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.aiReason,
                    style = MaterialTheme.typography.labelSmall,
                    color = ext.textSecondary
                )
            }
        }
    }
}
