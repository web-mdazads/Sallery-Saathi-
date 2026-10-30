package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.CategoryEntity
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserPreferences
import com.example.ui.components.AnimatedRupeeText
import com.example.ui.components.SaathiCard
import com.example.ui.components.StaggeredEntrance
import com.example.ui.components.SwipeableTransactionCard
import com.example.ui.components.TransactionReceiptSheet
import com.example.ui.components.groupTransactionsByDate
import com.example.ui.theme.LocalSaathiColors
import com.example.util.FinanceUtils
import com.example.util.SalaryCycleInfo
import kotlin.math.abs

private enum class TxTypeFilter(val label: String) {
    ALL("All"),
    EXPENSES("Expenses"),
    INCOME("Income")
}

private enum class DateRangeFilter(val label: String) {
    THIS_CYCLE("This Cycle"),
    LAST_7_DAYS("Last 7 Days"),
    LAST_30_DAYS("Last 30 Days"),
    ALL_TIME("All Time")
}

@Composable
fun TransactionsScreen(
    preferences: UserPreferences,
    allTransactions: List<TransactionEntity>,
    categories: List<CategoryEntity>,
    currentCycleInfo: SalaryCycleInfo?,
    onAddExpense: () -> Unit,
    onEditTransaction: (TransactionEntity) -> Unit,
    onDeleteTransaction: (Long) -> Unit
) {
    val ext = LocalSaathiColors.current
    val currency = preferences.currencySymbol

    var searchQuery by remember { mutableStateOf("") }
    var typeFilter by remember { mutableStateOf(TxTypeFilter.ALL) }
    var dateFilter by remember { mutableStateOf(DateRangeFilter.THIS_CYCLE) }
    var selectedCategoryId by remember { mutableStateOf<String?>(null) }
    var pendingDeleteTx by remember { mutableStateOf<TransactionEntity?>(null) }
    var selectedReceiptTxId by remember { mutableStateOf<Long?>(null) }

    val categoryMap = remember(categories) { categories.associateBy { it.id } }
    val activeReceiptTx = remember(allTransactions, selectedReceiptTxId) {
        allTransactions.find { it.id == selectedReceiptTxId }
    }

    val filteredTransactions = remember(
        allTransactions,
        searchQuery,
        typeFilter,
        dateFilter,
        selectedCategoryId,
        currentCycleInfo
    ) {
        val now = System.currentTimeMillis()
        val dayMs = 24L * 60L * 60L * 1000L
        val q = searchQuery.trim().lowercase()

        allTransactions
            .filter { tx ->
                val txType = runCatching { TransactionType.valueOf(tx.type) }.getOrDefault(TransactionType.EXPENSE)
                when (typeFilter) {
                    TxTypeFilter.ALL -> true
                    TxTypeFilter.EXPENSES -> txType == TransactionType.EXPENSE
                    TxTypeFilter.INCOME -> txType.isIncome
                }
            }
            .filter { tx ->
                when (dateFilter) {
                    DateRangeFilter.ALL_TIME -> true
                    DateRangeFilter.LAST_7_DAYS -> tx.dateMillis >= now - 7L * dayMs
                    DateRangeFilter.LAST_30_DAYS -> tx.dateMillis >= now - 30L * dayMs
                    DateRangeFilter.THIS_CYCLE -> {
                        if (currentCycleInfo != null) {
                            tx.cycleKey == currentCycleInfo.cycleKey ||
                                tx.dateMillis in currentCycleInfo.startMillis..currentCycleInfo.endMillis
                        } else true
                    }
                }
            }
            .filter { tx ->
                selectedCategoryId == null || tx.categoryId == selectedCategoryId
            }
            .filter { tx ->
                if (q.isEmpty()) true
                else {
                    val catName = categoryMap[tx.categoryId]?.name?.lowercase().orEmpty()
                    tx.title.lowercase().contains(q) ||
                        tx.note.lowercase().contains(q) ||
                        catName.contains(q) ||
                        tx.paymentMethod.lowercase().contains(q)
                }
            }
            .sortedWith(compareByDescending<TransactionEntity> { it.dateMillis }.thenByDescending { it.id })
    }

    val dayGroups = remember(filteredTransactions, preferences.dateFormatPattern) {
        groupTransactionsByDate(filteredTransactions, preferences.dateFormatPattern)
    }

    val (filteredIncomeSum, filteredExpenseSum) = remember(filteredTransactions) {
        var inc = 0.0
        var exp = 0.0
        filteredTransactions.forEach { tx ->
            val t = runCatching { TransactionType.valueOf(tx.type) }.getOrDefault(TransactionType.EXPENSE)
            if (t.isIncome) inc += tx.amount
            else if (t == TransactionType.EXPENSE) exp += tx.amount
        }
        inc to exp
    }

    val showDecimals = preferences.showDecimalsInLists

    Box(
        contentAlignment = Alignment.TopCenter,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 132.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .widthIn(max = 780.dp)
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
                                text = "Transactions",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            Text(
                                text = "${filteredTransactions.size} entries",
                                style = MaterialTheme.typography.labelMedium,
                                color = ext.textSecondary,
                                maxLines = 1
                            )
                        }
                        Button(
                            onClick = onAddExpense,
                            shape = RoundedCornerShape(16.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier.testTag("transactions_add_expense_button")
                        ) {
                            Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add", fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }

            // Cashflow Summary Strip for Active Filter
            if (filteredTransactions.isNotEmpty()) {
                item {
                    StaggeredEntrance(index = 1) {
                        SaathiCard(
                            containerColor = ext.surfaceElevated,
                            cornerRadius = 20.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 18.dp, vertical = 14.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Spent",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary
                                    )
                                    AnimatedRupeeText(
                                        amount = filteredExpenseSum,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ext.expenseRed,
                                        showDecimals = showDecimals,
                                        currencySymbol = currency
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    horizontalAlignment = Alignment.End,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = "Income",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = ext.textSecondary
                                    )
                                    AnimatedRupeeText(
                                        amount = filteredIncomeSum,
                                        style = MaterialTheme.typography.titleLarge,
                                        color = ext.incomeGreen,
                                        showDecimals = showDecimals,
                                        currencySymbol = currency
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Field
            item {
                StaggeredEntrance(index = 2) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search...") },
                        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Outlined.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("transactions_search_input")
                    )
                }
            }

            // Type, Date & Category Filters
            item {
                StaggeredEntrance(index = 3) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            TxTypeFilter.entries.forEach { tf ->
                                FilterChip(
                                    selected = typeFilter == tf,
                                    onClick = { typeFilter = tf },
                                    label = { Text(tf.label, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                            DateRangeFilter.entries.forEach { df ->
                                FilterChip(
                                    selected = dateFilter == df,
                                    onClick = { dateFilter = df },
                                    label = { Text(df.label, maxLines = 1, softWrap = false) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.horizontalScroll(rememberScrollState())
                        ) {
                            FilterChip(
                                selected = selectedCategoryId == null,
                                onClick = { selectedCategoryId = null },
                                label = { Text("All Categories", maxLines = 1, softWrap = false) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                            categories.forEach { cat ->
                                FilterChip(
                                    selected = selectedCategoryId == cat.id,
                                    onClick = {
                                        selectedCategoryId = if (selectedCategoryId == cat.id) null else cat.id
                                    },
                                    label = { Text(cat.name, maxLines = 1, softWrap = false) },
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

            if (dayGroups.isEmpty()) {
                item {
                    SaathiCard(
                        containerColor = ext.surfaceElevated,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ReceiptLong,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(42.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (allTransactions.isEmpty()) "No transactions yet" else "No matches",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            Button(
                                onClick = onAddExpense,
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Expense")
                            }
                        }
                    }
                }
            } else {
                dayGroups.forEach { group ->
                    item(key = "header_${group.dayKey}") {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp, bottom = 2.dp, start = 4.dp, end = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = group.headerLabel,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            val netPrefix = if (group.netAmount >= 0) "+" else "-"
                            Text(
                                text = if (group.totalSpent > 0 && group.netAmount < 0) {
                                    "-${FinanceUtils.formatRupee(group.totalSpent, showDecimals, currency)}"
                                } else {
                                    "$netPrefix${FinanceUtils.formatRupee(abs(group.netAmount), showDecimals, currency)}"
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = when {
                                    group.netAmount > 0 -> ext.incomeGreen
                                    group.totalSpent > 0 -> ext.expenseRed
                                    else -> ext.textSecondary
                                },
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    items(group.transactions, key = { it.id }) { tx ->
                        SwipeableTransactionCard(
                            transaction = tx,
                            category = categoryMap[tx.categoryId],
                            currencySymbol = currency,
                            dateFormatPattern = preferences.dateFormatPattern,
                            showDecimals = preferences.showDecimalsInLists,
                            showDateInSubtitle = false,
                            onClick = { selectedReceiptTxId = tx.id },
                            onEdit = { onEditTransaction(tx) },
                            onDeleteRequest = { pendingDeleteTx = tx },
                            modifier = Modifier.testTag("transaction_card_${tx.id}")
                        )
                    }
                }
            }
        }
    }

    if (activeReceiptTx != null) {
        TransactionReceiptSheet(
            transaction = activeReceiptTx,
            category = categoryMap[activeReceiptTx.categoryId],
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

    if (pendingDeleteTx != null) {
        val target = pendingDeleteTx!!
        AlertDialog(
            onDismissRequest = { pendingDeleteTx = null },
            title = { Text("Delete transaction?") },
            text = {
                Text(
                    "Are you sure you want to delete \"${target.title}\" (${FinanceUtils.formatRupee(target.amount, preferences.showDecimalsInLists, currency)})?"
                )
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
