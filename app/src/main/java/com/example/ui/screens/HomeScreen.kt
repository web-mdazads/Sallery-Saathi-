package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.PieChartOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import com.example.data.BudgetHealth
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.UserPreferences
import com.example.ui.components.AnimatedRupeeText
import com.example.ui.components.HealthStatusBadge
import com.example.ui.components.InteractiveAllocationDonutChart
import com.example.ui.components.SaathiCard
import com.example.ui.components.SaathiUserAvatar
import com.example.ui.components.SmoothBudgetProgressBar
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.SwipeableTransactionCard
import com.example.ui.components.TransactionReceiptSheet
import com.example.ui.components.categoryIconForKey
import com.example.ui.theme.ChartPalette
import com.example.ui.theme.LocalSaathiColors
import com.example.util.CategoryAllocationSummary
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import java.util.Calendar
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    preferences: UserPreferences,
    summary: MonthlyFinancialSummary?,
    categories: List<CategoryEntity>,
    recentTransactions: List<TransactionEntity>,
    unreadNotificationCount: Int = 0,
    pendingApprovalCount: Int = 0,
    onOpenNotifications: () -> Unit = {},
    onOpenSettings: () -> Unit,
    onOpenAddExpense: () -> Unit,
    onOpenAddIncome: () -> Unit,
    onOpenBudgetTab: () -> Unit,
    onOpenAiTab: () -> Unit,
    onOpenBudgetPlanner: () -> Unit,
    onAdjustPayday: (Int) -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit,
    onViewAllTransactions: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val currency = preferences.currencySymbol

    var showPaydayDialog by remember { mutableStateOf(false) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedReceiptTxId by remember { mutableStateOf<Long?>(null) }
    var highlightedCategoryId by remember { mutableStateOf<String?>(null) }

    val activeReceiptTx = remember(recentTransactions, selectedReceiptTxId) {
        recentTransactions.find { it.id == selectedReceiptTxId }
    }

    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            val current = System.currentTimeMillis()
            nowMillis = current
            val millisUntilNextMinute = (60_000L - (current % 60_000L)).coerceIn(1_000L, 60_000L)
            delay(millisUntilNextMinute)
        }
    }

    val hour = remember(nowMillis) {
        Calendar.getInstance().apply { timeInMillis = nowMillis }.get(Calendar.HOUR_OF_DAY)
    }
    val baseGreeting = remember(hour) { FinanceUtils.getGreetingForHour(hour) }
    val fullGreeting = if (preferences.userName.isNotBlank()) {
        "${baseGreeting.removeSuffix("!")}, ${preferences.userName}!"
    } else {
        baseGreeting
    }

    val isFreshEmptyState = summary != null &&
        summary.totalReceivedIncome <= 0.0 &&
        summary.totalPlannedAllocated <= 0.0 &&
        recentTransactions.isEmpty()

    val hasOverspentAny = summary?.categorySummaries?.any { it.health == BudgetHealth.OVERSPENT } == true

    val showDecimals = preferences.showDecimalsInLists

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier
                .widthIn(max = 780.dp)
                .fillMaxWidth()
        ) {
            // 1. Luxury Editorial Header with Compact Live Cycle + Date + Time Badge
            item {
                StaggeredEntrance(index = 0) {
                    val fontScale = LocalDensity.current.fontScale
                    val compactBadgeText = remember(summary?.cycleInfo?.cycleKey, summary?.cycleInfo?.monthTitle, preferences.dateFormatPattern, nowMillis) {
                        if (summary != null) {
                            FinanceUtils.formatCompactHeaderBadge(
                                cycleKey = summary.cycleInfo.cycleKey,
                                fallbackCycleTitle = summary.cycleInfo.monthTitle,
                                nowMillis = nowMillis,
                                datePattern = preferences.dateFormatPattern
                            )
                        } else {
                            ""
                        }
                    }
                    val badgeDesc = stringResource(R.string.home_live_cycle_badge_desc)

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        // Avatar (44dp) + gap (10dp) + gap (6dp) + 2 action squircles (44dp + 6dp + 44dp) = 154dp
                        val centerColumnWidthDp = (maxWidth.value - 154f).coerceAtLeast(0f)
                        val placeBadgeBelowRow = (centerColumnWidthDp / fontScale) < 176f

                        Column(modifier = Modifier.fillMaxWidth()) {
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
                                            .minimumInteractiveComponentSize()
                                            .clickable(onClick = onOpenSettings)
                                            .testTag("home_avatar_button")
                                    ) {
                                        SaathiUserAvatar(
                                            avatarKey = preferences.avatarKey,
                                            userName = preferences.userName,
                                            size = 44.dp,
                                            showRing = true
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = fullGreeting,
                                            style = if (fullGreeting.length > 16) {
                                                MaterialTheme.typography.titleMedium
                                            } else {
                                                MaterialTheme.typography.titleLarge
                                            },
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.testTag("home_greeting_text")
                                        )
                                        if (compactBadgeText.isNotEmpty() && !placeBadgeBelowRow) {
                                            Spacer(modifier = Modifier.height(3.dp))
                                            CompactLiveCycleDatePill(
                                                badgeText = compactBadgeText,
                                                accessibilityDescription = badgeDesc
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(6.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SaathiCard(
                                        cornerRadius = 13.dp,
                                        shadowElevation = 0.dp,
                                        onClick = onOpenNotifications,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(44.dp)
                                            .testTag("home_notifications_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = if (pendingApprovalCount > 0) {
                                                    Icons.Outlined.NotificationsActive
                                                } else {
                                                    Icons.Outlined.NotificationsNone
                                                },
                                                contentDescription = "Open Notification Center",
                                                modifier = Modifier.size(21.dp)
                                            )
                                            if (unreadNotificationCount > 0) {
                                                val badgeLabel = if (unreadNotificationCount > 9) "9+" else unreadNotificationCount.toString()
                                                val badgeBg = if (pendingApprovalCount > 0 || hasOverspentAny) ext.warning else ext.healthy
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 5.dp, end = 5.dp)
                                                        .defaultMinSize(minWidth = 15.dp, minHeight = 15.dp)
                                                        .clip(CircleShape)
                                                        .background(badgeBg)
                                                        .padding(horizontal = 3.dp)
                                                ) {
                                                    Text(
                                                        text = badgeLabel,
                                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                                        color = Color.White,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        softWrap = false
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.TopEnd)
                                                        .padding(top = 8.dp, end = 8.dp)
                                                        .size(7.dp)
                                                        .clip(CircleShape)
                                                        .background(if (hasOverspentAny) ext.warning else ext.healthy)
                                                )
                                            }
                                        }
                                    }
                                    SaathiCard(
                                        cornerRadius = 13.dp,
                                        shadowElevation = 0.dp,
                                        onClick = onOpenSettings,
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .size(44.dp)
                                            .testTag("home_settings_button")
                                    ) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier.fillMaxSize()
                                        ) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = "Settings and Profile",
                                                modifier = Modifier.size(21.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            if (compactBadgeText.isNotEmpty() && placeBadgeBelowRow) {
                                Spacer(modifier = Modifier.height(6.dp))
                                CompactLiveCycleDatePill(
                                    badgeText = compactBadgeText,
                                    accessibilityDescription = badgeDesc,
                                    modifier = Modifier.padding(start = 54.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Pending UPI Approval Quick Banner (shown whenever bank SMS payments await approval)
            if (pendingApprovalCount > 0) {
                item {
                    SaathiCard(
                        containerColor = ext.warningContainer,
                        borderColor = ext.warning.copy(alpha = 0.45f),
                        cornerRadius = 20.dp,
                        onClick = onOpenNotifications,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("home_pending_upi_banner")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.NotificationsActive,
                                    contentDescription = null,
                                    tint = ext.warning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "$pendingApprovalCount UPI payment(s) pending",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Review",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = ext.warning
                            )
                        }
                    }
                }
            }

            // Empty New-User Welcome Spotlight
            if (isFreshEmptyState) {
                item {
                    StaggeredEntrance(index = 1) {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            cornerRadius = 24.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Box {
                                    Image(
                                        painter = painterResource(id = R.drawable.img_onboarding_hero),
                                        contentDescription = "Set up your monthly salary plan",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(168.dp)
                                            .clip(RoundedCornerShape(18.dp))
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(10.dp)
                                            .clip(RoundedCornerShape(50))
                                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.92f))
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            text = "Private & Offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(14.dp))
                                Text(
                                    text = "Plan your month",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(
                                        onClick = onOpenBudgetPlanner,
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                            .testTag("empty_setup_budget_button")
                                    ) {
                                        Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Plan Budget",
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    OutlinedButton(
                                        onClick = onOpenAddIncome,
                                        shape = RoundedCornerShape(16.dp),
                                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                        modifier = Modifier.height(50.dp)
                                    ) {
                                        Text(
                                            text = "Add Income",
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
            }

            // 2. Layered Luxury Monthly Income Hero Card
            if (summary != null) {
                item {
                    StaggeredEntrance(index = 2) {
                        MonthlyIncomeHeroCard(
                            summary = summary,
                            currencySymbol = currency,
                            showDecimals = showDecimals
                        )
                    }
                }

                // 3. Spotlight Daily Allowance Card ("Aaj ka suggested budget")
                item {
                    StaggeredEntrance(index = 3) {
                        DailyAllowanceCard(
                            summary = summary,
                            currencySymbol = currency,
                            showDecimals = showDecimals,
                            onAdjustPaydayClick = { showPaydayDialog = true }
                        )
                    }
                }
            }

            // 4. Tactile Tri-Color Quick Actions Row
            item {
                StaggeredEntrance(index = 4) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        QuickActionTile(
                            icon = Icons.Outlined.Add,
                            label = "Add Expense",
                            isPrimary = true,
                            accentTint = MaterialTheme.colorScheme.primary,
                            onClick = onOpenAddExpense,
                            testTag = "quick_action_add_expense",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Outlined.AccountBalanceWallet,
                            label = "Add Income",
                            isPrimary = false,
                            accentTint = ext.incomeGreen,
                            onClick = onOpenAddIncome,
                            testTag = "quick_action_add_income",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Outlined.PieChartOutline,
                            label = "Budget",
                            isPrimary = false,
                            accentTint = ext.accentPrimary,
                            onClick = onOpenBudgetTab,
                            testTag = "quick_action_view_budget",
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionTile(
                            icon = Icons.Outlined.AutoAwesome,
                            customDrawableRes = R.drawable.ic_ai_saathi_spark,
                            label = "Ask AI",
                            isPrimary = false,
                            accentTint = ext.accentTertiary,
                            onClick = onOpenAiTab,
                            testTag = "quick_action_ask_ai",
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 5. Interactive Salary Allocation Donut + Synchronized Category Rows
            if (summary != null) {
                item {
                    StaggeredEntrance(index = 5) {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            cornerRadius = 24.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Salary Allocation",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    TextButton(
                                        onClick = onOpenBudgetPlanner,
                                        modifier = Modifier.testTag("home_edit_allocation_button")
                                    ) {
                                        Text(
                                            text = if (summary.totalPlannedAllocated > 0) "Edit" else "Plan",
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                InteractiveAllocationDonutChart(
                                    totalIncome = summary.totalReceivedIncome,
                                    totalAllocated = summary.totalPlannedAllocated,
                                    unallocatedAmount = summary.unallocatedAmount,
                                    categories = summary.categorySummaries,
                                    currencySymbol = currency,
                                    showDecimals = showDecimals,
                                    selectedCategoryId = highlightedCategoryId,
                                    onSelectCategoryId = { highlightedCategoryId = it }
                                )

                                if (summary.totalReceivedIncome > 0 && summary.unallocatedAmount > 0.5) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ext.surfaceSubtle)
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Unallocated: ${FinanceUtils.formatRupee(summary.unallocatedAmount, showDecimals, currency)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                } else if (summary.overAllocatedAmount > 0.5) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(ext.warningContainer)
                                            .padding(horizontal = 12.dp, vertical = 9.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.WarningAmber,
                                            contentDescription = "Over budget",
                                            tint = ext.warning,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Over income by ${FinanceUtils.formatRupee(summary.overAllocatedAmount, showDecimals, currency)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = ext.warning,
                                            maxLines = 1,
                                            softWrap = false,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                HorizontalDivider(color = ext.cardBorder)
                                Spacer(modifier = Modifier.height(10.dp))

                                val visibleRows = summary.categorySummaries.filter {
                                    it.isIncluded || it.actualSpent > 0.0
                                }
                                visibleRows.forEachIndexed { index, catSummary ->
                                    val isHighlighted = highlightedCategoryId == catSummary.category.id
                                    HomeAllocationCategoryRow(
                                        item = catSummary,
                                        isHighlighted = isHighlighted,
                                        currencySymbol = currency,
                                        showDecimals = showDecimals,
                                        onClick = {
                                            highlightedCategoryId =
                                                if (isHighlighted) null else catSummary.category.id
                                        }
                                    )
                                    if (index < visibleRows.lastIndex) {
                                        HorizontalDivider(
                                            color = ext.cardBorder.copy(alpha = 0.55f),
                                            modifier = Modifier.padding(vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 6. Recent Transactions Section (Swipeable)
            item {
                StaggeredEntrance(index = 6) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Recent",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (recentTransactions.isNotEmpty()) {
                            TextButton(onClick = onViewAllTransactions) {
                                Text("See all")
                            }
                        }
                    }
                }
            }

            if (recentTransactions.isEmpty()) {
                item {
                    SaathiCard(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp)
                        ) {
                            Text(
                                text = "No transactions yet",
                                style = MaterialTheme.typography.bodyMedium,
                                color = ext.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(recentTransactions.take(5), key = { it.id }) { tx ->
                    val cat = categories.find { it.id == tx.categoryId }
                    SwipeableTransactionCard(
                        transaction = tx,
                        category = cat,
                        currencySymbol = currency,
                        dateFormatPattern = preferences.dateFormatPattern,
                        showDecimals = preferences.showDecimalsInLists,
                        onClick = { selectedReceiptTxId = tx.id },
                        onEdit = { onEditTransaction(tx) },
                        onDeleteRequest = { pendingDeleteTx = tx }
                    )
                }
            }
        }
    }

    if (activeReceiptTx != null) {
        val cat = categories.find { it.id == activeReceiptTx.categoryId }
        TransactionReceiptSheet(
            transaction = activeReceiptTx,
            category = cat,
            currencySymbol = currency,
            showDecimals = preferences.showDecimalsInLists,
            dateFormatPattern = preferences.dateFormatPattern,
            onDismiss = { selectedReceiptTxId = null },
            onEdit = {
                val target = activeReceiptTx
                selectedReceiptTxId = null
                onEditTransaction(target)
            },
            onDelete = {
                val target = activeReceiptTx
                selectedReceiptTxId = null
                pendingDeleteTx = target
            }
        )
    }

    // Adjust Payday Dialog
    if (showPaydayDialog && summary != null) {
        var tempPayday by remember { mutableIntStateOf(preferences.paydayDayOfMonth) }
        AlertDialog(
            onDismissRequest = { showPaydayDialog = false },
            title = { Text("Payday") },
            text = {
                Column {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 5, 7, 10, 15, 20, 25, 28, 30, 31).forEach { d ->
                            FilterChip(
                                selected = tempPayday == d,
                                onClick = { tempPayday = d },
                                label = { Text("Day $d", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onAdjustPayday(tempPayday)
                        showPaydayDialog = false
                    }
                ) {
                    Text("Recalculate")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPaydayDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    // Delete Confirmation Dialog
    if (pendingDeleteTx != null) {
        val target = pendingDeleteTx!!
        AlertDialog(
            onDismissRequest = { pendingDeleteTx = null },
            title = { Text("Delete this entry?") },
            text = {
                Text("Remove \"${target.title}\" (${FinanceUtils.formatRupee(target.amount, preferences.showDecimalsInLists, currency)})? This cannot be undone.")
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteTransaction(target.id)
                        pendingDeleteTx = null
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteTx = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MonthlyIncomeHeroCard(
    summary: MonthlyFinancialSummary,
    currencySymbol: String,
    showDecimals: Boolean = false
) {
    val ext = LocalSaathiColors.current
    val gradient = Brush.linearGradient(
        colors = listOf(ext.heroGradientStart, ext.heroGradientMid, ext.heroGradientEnd)
    )
    val glowAccent = ext.heroGlowAccent
    val heroBalanceFormatted = FinanceUtils.formatRupee(summary.remainingAvailableBalance, showDecimals, currencySymbol)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(gradient)
            .border(1.dp, Color.White.copy(alpha = 0.14f), RoundedCornerShape(26.dp))
            .drawBehind {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(glowAccent.copy(alpha = 0.26f), Color.Transparent),
                        center = Offset(size.width * 0.88f, size.height * 0.15f),
                        radius = size.width * 0.55f
                    ),
                    center = Offset(size.width * 0.88f, size.height * 0.15f),
                    radius = size.width * 0.55f
                )
            }
            .padding(22.dp)
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Available Balance",
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White.copy(alpha = 0.86f),
                    maxLines = 1,
                    softWrap = false
                )
                if (summary.expectedUnpaidIncentives > 0.0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color(0x33FBBF24))
                            .border(1.dp, Color(0x66FBBF24), RoundedCornerShape(50))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Unpaid: ${FinanceUtils.formatRupee(summary.expectedUnpaidIncentives, showDecimals, currencySymbol)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            AnimatedRupeeText(
                amount = summary.remainingAvailableBalance,
                style = if (heroBalanceFormatted.length > 13) MaterialTheme.typography.displaySmall else MaterialTheme.typography.displayMedium,
                color = Color.White,
                showDecimals = showDecimals,
                currencySymbol = currencySymbol
            )

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.Black.copy(alpha = 0.22f))
                    .border(1.dp, Color.White.copy(alpha = 0.09f), RoundedCornerShape(18.dp))
                    .padding(14.dp)
            ) {
                HeroMetricColumn(
                    label = "Income",
                    value = FinanceUtils.formatRupee(summary.totalReceivedIncome, showDecimals, currencySymbol),
                    subtext = "Salary ${FinanceUtils.formatRupee(summary.fixedSalaryReceived, showDecimals, currencySymbol)}",
                    modifier = Modifier.weight(1f)
                )
                HeroMetricColumn(
                    label = "Incentives",
                    value = FinanceUtils.formatRupee(summary.incentivesReceived, showDecimals, currencySymbol),
                    subtext = if (summary.expectedUnpaidIncentives > 0) "Excl. unpaid" else "Credited",
                    modifier = Modifier.weight(1f)
                )
                HeroMetricColumn(
                    label = "Spent",
                    value = FinanceUtils.formatRupee(summary.totalExpenses, showDecimals, currencySymbol),
                    subtext = if (summary.totalReceivedIncome > 0) {
                        "${FinanceUtils.formatPercentage((summary.totalExpenses / summary.totalReceivedIncome) * 100.0)} used"
                    } else "This cycle",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun HeroMetricColumn(
    label: String,
    value: String,
    subtext: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.76f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = if (value.length > 10) MaterialTheme.typography.titleSmall else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtext,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.72f),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DailyAllowanceCard(
    summary: MonthlyFinancialSummary,
    currencySymbol: String,
    showDecimals: Boolean = false,
    onAdjustPaydayClick: () -> Unit
) {
    val ext = LocalSaathiColors.current
    val allowance = summary.dailyAllowance
    val cycleProgress = ((31 - allowance.daysUntilPayday.coerceIn(1, 30)) / 30f).coerceIn(0.08f, 1f)

    SaathiCard(
        containerColor = ext.allowanceSurface,
        borderColor = ext.allowanceBorder,
        cornerRadius = 24.dp,
        shadowElevation = 1.dp,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("home_daily_allowance_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's Budget",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${allowance.daysUntilPayday}d to payday",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Surface(
                    onClick = onAdjustPaydayClick,
                    shape = RoundedCornerShape(14.dp),
                    color = ext.accentTertiaryContainer.copy(alpha = 0.55f),
                    border = BorderStroke(1.dp, ext.accentTertiary.copy(alpha = 0.32f)),
                    shadowElevation = 0.dp,
                    tonalElevation = 0.dp,
                    modifier = Modifier
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("adjust_payday_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = "Adjust Payday",
                            tint = ext.accentTertiary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Day ${summary.cycleInfo.paydayDayOfMonth}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = ext.accentTertiary,
                            maxLines = 1
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    AnimatedRupeeText(
                        amount = allowance.suggestedTodayAmount,
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        showDecimals = showDecimals,
                        currencySymbol = currencySymbol
                    )
                    Text(
                        text = "Safe today",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = FinanceUtils.formatRupee(allowance.usableMoneyUntilPayday, showDecimals, currencySymbol),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Till payday",
                        style = MaterialTheme.typography.labelSmall,
                        color = ext.textSecondary,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(cycleProgress)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                listOf(MaterialTheme.colorScheme.primary, ext.accentSecondary)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = allowance.explanation,
                style = MaterialTheme.typography.labelSmall,
                color = ext.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun QuickActionTile(
    icon: ImageVector,
    label: String,
    isPrimary: Boolean,
    accentTint: Color,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier,
    customDrawableRes: Int? = null
) {
    val ext = LocalSaathiColors.current
    val bg = if (isPrimary) MaterialTheme.colorScheme.primary else ext.surfaceElevated
    val fg = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

    SaathiCard(
        containerColor = bg,
        borderColor = if (isPrimary) MaterialTheme.colorScheme.primary else accentTint.copy(alpha = 0.28f),
        cornerRadius = 18.dp,
        onClick = onClick,
        modifier = modifier.testTag(testTag)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 3.dp)
        ) {
            if (customDrawableRes != null) {
                Icon(
                    painter = painterResource(id = customDrawableRes),
                    contentDescription = label,
                    tint = if (isPrimary) fg else accentTint,
                    modifier = Modifier.size(21.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = if (isPrimary) fg else accentTint,
                    modifier = Modifier.size(21.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 11.sp,
                    letterSpacing = (-0.1).sp
                ),
                fontWeight = FontWeight.SemiBold,
                color = fg,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun HomeAllocationCategoryRow(
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

    val rowBg by animateColorAsState(
        targetValue = if (isHighlighted) catColor.copy(alpha = 0.09f) else Color.Transparent,
        label = "cat_row_highlight"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.15f))
            ) {
                Icon(
                    imageVector = categoryIconForKey(item.category.iconKey),
                    contentDescription = null,
                    tint = catColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = item.category.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.plannedAmount > 0.0 || item.actualSpent > 0.0) {
                        HealthStatusBadge(health = item.health)
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
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
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Spent: ${FinanceUtils.formatRupee(item.actualSpent, showDecimals, currencySymbol)}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        softWrap = false
                    )
                }
            }
        }

        if (item.plannedAmount > 0.0 || item.actualSpent > 0.0) {
            Spacer(modifier = Modifier.height(6.dp))
            SmoothBudgetProgressBar(
                progressFraction = progress,
                health = item.health,
                height = 6.dp
            )
        }
    }
}

@Composable
private fun CompactLiveCycleDatePill(
    badgeText: String,
    accessibilityDescription: String,
    modifier: Modifier = Modifier
) {
    val ext = LocalSaathiColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = ext.surfaceSubtle,
        border = BorderStroke(0.8.dp, ext.cardBorder),
        modifier = modifier
            .testTag("home_cycle_date_badge")
            .semantics { contentDescription = accessibilityDescription }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(ext.accentSecondary)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = badgeText,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    letterSpacing = (-0.1).sp
                ),
                fontWeight = FontWeight.Medium,
                color = ext.textSecondary,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

