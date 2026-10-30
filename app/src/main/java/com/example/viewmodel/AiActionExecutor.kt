package com.example.viewmodel

import com.example.ai.AiActionCommand
import com.example.ai.AiActionOp
import com.example.ai.AiAppliedReceipt
import com.example.ai.AiInverseOp
import com.example.ai.AiLocalCommandParser
import com.example.ai.AiReceiptBadgeKind
import com.example.data.BudgetAllocationEntity
import com.example.data.CategoryEntity
import com.example.data.FinanceRepository
import com.example.data.GoalEntity
import com.example.data.PaymentMethod
import com.example.data.TransactionEntity
import com.example.data.TransactionType
import com.example.data.UserPreferencesRepository
import com.example.util.FinanceUtils
import com.example.util.MonthlyFinancialSummary
import java.util.Locale
import kotlin.math.abs

data class AiExecutionBatchResult(
    val receipts: List<AiAppliedReceipt>,
    val inverseOps: List<AiInverseOp>,
    val updatedSummary: MonthlyFinancialSummary,
    val notices: List<String> = emptyList()
)

object AiActionExecutor {

    suspend fun executeActions(
        commands: List<AiActionCommand>,
        initialState: SaathiAppState,
        repository: FinanceRepository,
        prefsRepo: UserPreferencesRepository
    ): AiExecutionBatchResult {
        val prefs = initialState.preferences
        val currency = prefs.currencySymbol
        val showDecimals = prefs.showDecimalsInLists

        var workingSalary = prefs.fixedMonthlySalary
        val workingCategories = initialState.categories.toMutableList()
        val workingTransactions = initialState.allTransactions.toMutableList()
        val workingAllocations = initialState.allBudgetAllocations.toMutableList()
        val workingGoals = initialState.goals.toMutableList()

        val currentCycle = FinanceUtils.computeSalaryCycle(
            paydayDayOfMonth = prefs.paydayDayOfMonth,
            dateFormatPattern = prefs.dateFormatPattern
        )
        val activeCycleKey = initialState.selectedCycleKey.ifBlank { currentCycle.cycleKey }

        val receipts = mutableListOf<AiAppliedReceipt>()
        val inverseOps = mutableListOf<AiInverseOp>()
        val notices = mutableListOf<String>()

        for (cmd in commands) {
            when (cmd.op) {
                AiActionOp.ADD_TRANSACTION -> {
                    val amt = cmd.amount
                    if (amt <= 0.0) continue

                    val txType = runCatching {
                        TransactionType.valueOf(cmd.transactionType.trim().uppercase(Locale.ENGLISH))
                    }.getOrDefault(TransactionType.EXPENSE)

                    val resolvedCat = AiLocalCommandParser.resolveCategory(
                        rawCategoryHint = cmd.categoryId,
                        rawTitleOrPrompt = "${cmd.title} ${cmd.matchQuery}",
                        categories = workingCategories,
                        learnedKeywords = prefs.learnedCategoryKeywords
                    )

                    val dayShiftMs = cmd.dayOffset.coerceIn(-365, 30) * 86_400_000L
                    val txDateMillis = System.currentTimeMillis() + dayShiftMs
                    val txCycleKey = if (cmd.dayOffset == 0) {
                        activeCycleKey
                    } else {
                        FinanceUtils.computeSalaryCycle(
                            paydayDayOfMonth = prefs.paydayDayOfMonth,
                            referenceTimeMillis = txDateMillis,
                            dateFormatPattern = prefs.dateFormatPattern
                        ).cycleKey
                    }

                    val paymentMethod = runCatching {
                        PaymentMethod.valueOf(cmd.paymentMethod.trim().uppercase(Locale.ENGLISH))
                    }.getOrDefault(PaymentMethod.UPI)

                    val cleanTitle = cmd.title.trim().ifBlank {
                        if (txType == TransactionType.EXPENSE) resolvedCat.name else txType.label
                    }

                    val entity = TransactionEntity(
                        id = 0L,
                        amount = amt,
                        type = txType.name,
                        categoryId = resolvedCat.id,
                        title = cleanTitle,
                        note = cmd.note.trim().ifBlank { "Added via AI Saathi" },
                        dateMillis = txDateMillis,
                        paymentMethod = paymentMethod.name,
                        cycleKey = txCycleKey
                    )

                    val insertedId = repository.saveTransaction(entity)
                    val savedEntity = entity.copy(id = insertedId)
                    workingTransactions.add(0, savedEntity)

                    val fmtAmt = FinanceUtils.formatRupee(amt, showDecimals, currency)
                    val labelDetail = if (txType == TransactionType.EXPENSE) {
                        if (cleanTitle.equals(resolvedCat.name, ignoreCase = true)) {
                            resolvedCat.name
                        } else {
                            "$cleanTitle (${resolvedCat.name})"
                        }
                    } else {
                        "$cleanTitle (${txType.label})"
                    }

                    inverseOps.add(AiInverseOp.DeleteTransaction(insertedId))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.ADDED,
                            refTag = "tx:$insertedId",
                            summaryLabel = "Added $fmtAmt • $labelDetail"
                        )
                    )
                }

                AiActionOp.EDIT_TRANSACTION -> {
                    val target = findTargetTransaction(
                        targetIdRaw = cmd.targetId,
                        matchQuery = cmd.matchQuery.ifBlank { cmd.title },
                        amountHint = cmd.secondaryAmount,
                        transactions = workingTransactions,
                        categories = workingCategories,
                        activeCycleKey = activeCycleKey
                    )
                    if (target == null) {
                        notices.add("Matching transaction not found to edit.")
                        continue
                    }

                    val previous = target.copy()
                    val nextAmount = if (cmd.amount > 0.0) cmd.amount else target.amount
                    val nextType = cmd.transactionType.trim().takeIf { it.isNotBlank() }?.let { raw ->
                        runCatching { TransactionType.valueOf(raw.uppercase(Locale.ENGLISH)).name }.getOrNull()
                    } ?: target.type

                    val nextCategoryId = if (cmd.categoryId.isNotBlank()) {
                        AiLocalCommandParser.resolveCategory(
                            rawCategoryHint = cmd.categoryId,
                            rawTitleOrPrompt = cmd.title,
                            categories = workingCategories,
                            learnedKeywords = prefs.learnedCategoryKeywords
                        ).id
                    } else {
                        target.categoryId
                    }

                    val nextTitle = cmd.title.trim().takeIf {
                        it.isNotBlank() &&
                            !it.equals("last", ignoreCase = true) &&
                            !it.equals("latest", ignoreCase = true)
                    } ?: target.title

                    val nextPaymentMethod = cmd.paymentMethod.trim().takeIf { it.isNotBlank() }?.let { raw ->
                        runCatching { PaymentMethod.valueOf(raw.uppercase(Locale.ENGLISH)).name }.getOrNull()
                    } ?: target.paymentMethod

                    val nextDateMillis = if (cmd.dayOffset != 0) {
                        System.currentTimeMillis() + (cmd.dayOffset.coerceIn(-365, 30) * 86_400_000L)
                    } else {
                        target.dateMillis
                    }

                    val updated = target.copy(
                        amount = nextAmount,
                        type = nextType,
                        categoryId = nextCategoryId,
                        title = nextTitle,
                        note = if (cmd.note.isNotBlank()) cmd.note.trim() else target.note,
                        paymentMethod = nextPaymentMethod,
                        dateMillis = nextDateMillis
                    )

                    repository.saveTransaction(updated)
                    val idx = workingTransactions.indexOfFirst { it.id == target.id }
                    if (idx >= 0) workingTransactions[idx] = updated

                    val fmtAmt = FinanceUtils.formatRupee(nextAmount, showDecimals, currency)
                    inverseOps.add(AiInverseOp.RestoreTransaction(previous))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.UPDATED,
                            refTag = "tx:${target.id}",
                            summaryLabel = "Updated ${updated.title} • $fmtAmt"
                        )
                    )
                }

                AiActionOp.DELETE_TRANSACTION -> {
                    val target = findTargetTransaction(
                        targetIdRaw = cmd.targetId,
                        matchQuery = cmd.matchQuery.ifBlank { cmd.title },
                        amountHint = cmd.amount,
                        transactions = workingTransactions,
                        categories = workingCategories,
                        activeCycleKey = activeCycleKey
                    )
                    if (target == null) {
                        notices.add("Matching transaction not found to delete.")
                        continue
                    }

                    val previous = target.copy()
                    repository.deleteTransaction(target.id)
                    workingTransactions.removeAll { it.id == target.id }

                    val fmtAmt = FinanceUtils.formatRupee(previous.amount, showDecimals, currency)
                    inverseOps.add(AiInverseOp.RestoreTransaction(previous))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.DELETED,
                            refTag = "tx:${previous.id}",
                            summaryLabel = "Deleted ${previous.title} • $fmtAmt"
                        )
                    )
                }

                AiActionOp.SET_BUDGET -> {
                    val resolvedCat = AiLocalCommandParser.resolveCategory(
                        rawCategoryHint = cmd.categoryId.ifBlank { cmd.targetId },
                        rawTitleOrPrompt = "${cmd.title} ${cmd.matchQuery}",
                        categories = workingCategories,
                        learnedKeywords = prefs.learnedCategoryKeywords
                    )
                    val planned = cmd.amount.coerceAtLeast(0.0)
                    val allocId = "${activeCycleKey}_${resolvedCat.id}"
                    val existing = workingAllocations.find { it.id == allocId }

                    val updatedAlloc = BudgetAllocationEntity(
                        id = allocId,
                        cycleKey = activeCycleKey,
                        categoryId = resolvedCat.id,
                        plannedAmount = planned,
                        aiReason = cmd.note.trim().ifBlank { existing?.aiReason.orEmpty().ifBlank { "Updated via AI Saathi" } },
                        isIncluded = planned > 0.0
                    )
                    repository.saveBudgetAllocation(updatedAlloc)

                    val idx = workingAllocations.indexOfFirst { it.id == allocId }
                    if (idx >= 0) workingAllocations[idx] = updatedAlloc else workingAllocations.add(updatedAlloc)

                    if (existing != null) {
                        inverseOps.add(AiInverseOp.RestoreBudgetAllocation(existing))
                    } else {
                        inverseOps.add(AiInverseOp.DeleteBudgetAllocation(allocId))
                    }

                    val fmtAmt = FinanceUtils.formatRupee(planned, showDecimals, currency)
                    val badgeKind = if (planned <= 0.0) AiReceiptBadgeKind.DELETED else AiReceiptBadgeKind.UPDATED
                    val label = if (planned <= 0.0) {
                        "Cleared ${resolvedCat.name} budget"
                    } else {
                        "Updated ${resolvedCat.name} budget • $fmtAmt"
                    }
                    receipts.add(
                        AiAppliedReceipt(
                            kind = badgeKind,
                            refTag = "budget:${resolvedCat.id}",
                            summaryLabel = label
                        )
                    )
                }

                AiActionOp.UPSERT_GOAL -> {
                    val existing = AiLocalCommandParser.resolveGoal(
                        targetId = cmd.targetId,
                        queryOrTitle = cmd.title.ifBlank { cmd.matchQuery },
                        goals = workingGoals
                    )

                    if (existing != null) {
                        val previous = existing.copy()
                        val updatedSaved = when {
                            cmd.isDelta && cmd.secondaryAmount >= 0.0 ->
                                (existing.savedAmount + cmd.secondaryAmount).coerceAtLeast(0.0)
                            cmd.isDelta && cmd.amount >= 0.0 ->
                                (existing.savedAmount + cmd.amount).coerceAtLeast(0.0)
                            !cmd.isDelta && cmd.secondaryAmount >= 0.0 ->
                                cmd.secondaryAmount
                            else -> existing.savedAmount
                        }
                        val updatedTarget = when {
                            !cmd.isDelta && cmd.amount >= 0.0 -> cmd.amount
                            existing.targetAmount <= 0.0 && updatedSaved > 0.0 ->
                                (updatedSaved * 4.0).coerceAtLeast(10_000.0)
                            else -> existing.targetAmount
                        }
                        val updatedGoal = existing.copy(
                            targetAmount = updatedTarget,
                            savedAmount = updatedSaved
                        )
                        repository.saveGoal(updatedGoal)
                        val idx = workingGoals.indexOfFirst { it.id == existing.id }
                        if (idx >= 0) workingGoals[idx] = updatedGoal

                        inverseOps.add(AiInverseOp.RestoreGoal(previous))
                        val fmtSaved = FinanceUtils.formatRupee(updatedSaved, showDecimals, currency)
                        val fmtTarget = FinanceUtils.formatRupee(updatedTarget, showDecimals, currency)
                        receipts.add(
                            AiAppliedReceipt(
                                kind = AiReceiptBadgeKind.UPDATED,
                                refTag = "goal:${existing.id}",
                                summaryLabel = "Updated ${existing.title} • $fmtSaved / $fmtTarget"
                            )
                        )
                    } else {
                        val cleanTitle = cmd.title.trim().ifBlank { cmd.matchQuery.trim() }.ifBlank { "Savings Goal" }
                        val slug = "goal_custom_" + cleanTitle.lowercase(Locale.ENGLISH)
                            .replace(Regex("[^a-z0-9]+"), "_")
                            .trim('_') + "_" + (System.currentTimeMillis() % 10000)
                        val targetAmt = when {
                            cmd.amount > 0.0 -> cmd.amount
                            cmd.secondaryAmount > 0.0 -> (cmd.secondaryAmount * 4.0).coerceAtLeast(10_000.0)
                            else -> 25_000.0
                        }
                        val savedAmt = when {
                            cmd.secondaryAmount >= 0.0 -> cmd.secondaryAmount
                            cmd.isDelta && cmd.amount > 0.0 -> cmd.amount
                            else -> 0.0
                        }
                        val newGoal = GoalEntity(
                            id = slug,
                            title = cleanTitle,
                            typeKey = "future_savings",
                            targetAmount = targetAmt,
                            savedAmount = savedAmt,
                            monthlyPlanned = 0.0,
                            iconKey = "savings",
                            subtitle = "Created via AI Saathi"
                        )
                        repository.saveGoal(newGoal)
                        workingGoals.add(newGoal)

                        inverseOps.add(AiInverseOp.DeleteGoal(slug))
                        val fmtTarget = FinanceUtils.formatRupee(targetAmt, showDecimals, currency)
                        receipts.add(
                            AiAppliedReceipt(
                                kind = AiReceiptBadgeKind.ADDED,
                                refTag = "goal:$slug",
                                summaryLabel = "Added goal $cleanTitle • Target $fmtTarget"
                            )
                        )
                    }
                }

                AiActionOp.DELETE_GOAL -> {
                    val existing = AiLocalCommandParser.resolveGoal(
                        targetId = cmd.targetId,
                        queryOrTitle = cmd.title.ifBlank { cmd.matchQuery },
                        goals = workingGoals
                    )
                    if (existing == null) {
                        notices.add("Matching goal not found to delete.")
                        continue
                    }
                    val previous = existing.copy()
                    if (existing.id.startsWith("goal_custom_")) {
                        repository.deleteGoal(existing.id)
                        workingGoals.removeAll { it.id == existing.id }
                    } else {
                        val cleared = existing.copy(targetAmount = 0.0, savedAmount = 0.0, monthlyPlanned = 0.0)
                        repository.saveGoal(cleared)
                        val idx = workingGoals.indexOfFirst { it.id == existing.id }
                        if (idx >= 0) workingGoals[idx] = cleared
                    }
                    inverseOps.add(AiInverseOp.RestoreGoal(previous))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.DELETED,
                            refTag = "goal:${existing.id}",
                            summaryLabel = "Deleted goal ${existing.title}"
                        )
                    )
                }

                AiActionOp.ADD_CATEGORY -> {
                    val cleanName = cmd.title.trim().ifBlank { cmd.matchQuery.trim() }
                    if (cleanName.isBlank()) continue
                    val alreadyExists = workingCategories.find { it.name.equals(cleanName, ignoreCase = true) }
                    if (alreadyExists != null) {
                        notices.add("Category \"${alreadyExists.name}\" already exists.")
                        continue
                    }
                    val slug = "custom_" + cleanName.lowercase(Locale.ENGLISH)
                        .replace(Regex("[^a-z0-9]+"), "_")
                        .trim('_') + "_" + (System.currentTimeMillis() % 10000)
                    val newCat = CategoryEntity(
                        id = slug,
                        name = cleanName,
                        iconKey = "custom",
                        colorIndex = workingCategories.size % 9,
                        isEssential = cmd.isEssential,
                        isSavingsOrDebt = false,
                        isCustom = true,
                        sortOrder = workingCategories.size + 1
                    )
                    repository.saveCategory(newCat)
                    workingCategories.add(newCat)

                    inverseOps.add(AiInverseOp.DeleteCustomCategory(slug))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.ADDED,
                            refTag = "cat:$slug",
                            summaryLabel = "Added category $cleanName"
                        )
                    )
                }

                AiActionOp.EDIT_CATEGORY -> {
                    val targetCat = workingCategories.find {
                        it.id.equals(cmd.targetId.trim(), ignoreCase = true) ||
                            (cmd.matchQuery.isNotBlank() && it.name.equals(cmd.matchQuery.trim(), ignoreCase = true)) ||
                            (cmd.categoryId.isNotBlank() && it.id.equals(cmd.categoryId.trim(), ignoreCase = true))
                    }
                    if (targetCat == null) {
                        notices.add("Matching category not found to edit.")
                        continue
                    }
                    val previous = targetCat.copy()
                    val nextName = cmd.title.trim().ifBlank { targetCat.name }
                    val updated = targetCat.copy(name = nextName)
                    repository.saveCategory(updated)
                    val idx = workingCategories.indexOfFirst { it.id == targetCat.id }
                    if (idx >= 0) workingCategories[idx] = updated

                    inverseOps.add(AiInverseOp.RestoreCategory(previous))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.UPDATED,
                            refTag = "cat:${targetCat.id}",
                            summaryLabel = "Updated category $nextName"
                        )
                    )
                }

                AiActionOp.DELETE_CATEGORY -> {
                    val query = cmd.matchQuery.ifBlank { cmd.title }.ifBlank { cmd.categoryId }.trim()
                    val targetCat = workingCategories.find { cat ->
                        cat.isCustom && (
                            cat.id.equals(cmd.targetId.trim(), ignoreCase = true) ||
                                cat.id.equals(query, ignoreCase = true) ||
                                cat.name.equals(query, ignoreCase = true) ||
                                (query.isNotBlank() && cat.name.lowercase(Locale.ENGLISH).contains(query.lowercase(Locale.ENGLISH)))
                            )
                    }
                    if (targetCat == null) {
                        notices.add("Custom category \"$query\" not found (default categories cannot be removed).")
                        continue
                    }
                    val previous = targetCat.copy()
                    repository.deleteCustomCategory(targetCat.id)
                    workingCategories.removeAll { it.id == targetCat.id }

                    inverseOps.add(AiInverseOp.RestoreCategory(previous))
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.DELETED,
                            refTag = "cat:${targetCat.id}",
                            summaryLabel = "Deleted category ${targetCat.name}"
                        )
                    )
                }

                AiActionOp.UPDATE_SALARY -> {
                    if (cmd.amount < 0.0) continue
                    val prevSalary = workingSalary
                    workingSalary = cmd.amount
                    prefsRepo.updateFixedSalary(cmd.amount)

                    inverseOps.add(AiInverseOp.RestoreSalary(prevSalary))
                    val fmtAmt = FinanceUtils.formatRupee(cmd.amount, showDecimals, currency)
                    receipts.add(
                        AiAppliedReceipt(
                            kind = AiReceiptBadgeKind.UPDATED,
                            refTag = "salary",
                            summaryLabel = "Updated monthly salary • $fmtAmt"
                        )
                    )
                }
            }
        }

        val cycleTx = workingTransactions.filter { tx ->
            if (tx.cycleKey.isNotBlank()) tx.cycleKey == activeCycleKey
            else tx.dateMillis in currentCycle.startMillis..currentCycle.endMillis
        }
        val cycleAlloc = workingAllocations.filter { it.cycleKey == activeCycleKey }
        val effectiveCycleInfo = initialState.summary?.cycleInfo ?: currentCycle

        val recomputedSummary = FinanceUtils.calculateMonthlySummary(
            configuredFixedSalary = workingSalary,
            configuredExpectedIncentive = prefs.expectedIncentiveAmount,
            categories = workingCategories,
            cycleTransactions = cycleTx,
            cycleAllocations = cycleAlloc,
            cycleInfo = effectiveCycleInfo,
            currencySymbol = currency,
            showDecimals = showDecimals
        )

        return AiExecutionBatchResult(
            receipts = receipts,
            inverseOps = inverseOps,
            updatedSummary = recomputedSummary,
            notices = notices
        )
    }

    suspend fun executeUndo(
        inverseOps: List<AiInverseOp>,
        repository: FinanceRepository,
        prefsRepo: UserPreferencesRepository
    ) {
        // Apply inverse operations in reverse order for exact state rollback
        for (op in inverseOps.asReversed()) {
            when (op) {
                is AiInverseOp.DeleteTransaction -> repository.deleteTransaction(op.transactionId)
                is AiInverseOp.RestoreTransaction -> repository.saveTransaction(op.previous)
                is AiInverseOp.DeleteBudgetAllocation -> repository.deleteBudgetAllocation(op.allocationId)
                is AiInverseOp.RestoreBudgetAllocation -> repository.saveBudgetAllocation(op.previous)
                is AiInverseOp.DeleteGoal -> repository.deleteGoal(op.goalId)
                is AiInverseOp.RestoreGoal -> repository.saveGoal(op.previous)
                is AiInverseOp.DeleteCustomCategory -> repository.deleteCustomCategory(op.categoryId)
                is AiInverseOp.RestoreCategory -> repository.saveCategory(op.previous)
                is AiInverseOp.RestoreSalary -> prefsRepo.updateFixedSalary(op.previousMonthlySalary)
            }
        }
    }

    private fun findTargetTransaction(
        targetIdRaw: String,
        matchQuery: String,
        amountHint: Double,
        transactions: List<TransactionEntity>,
        categories: List<CategoryEntity>,
        activeCycleKey: String
    ): TransactionEntity? {
        if (transactions.isEmpty()) return null

        val cleanId = targetIdRaw.trim().removePrefix("tx:").toLongOrNull()
        if (cleanId != null && cleanId > 0L) {
            transactions.find { it.id == cleanId }?.let { return it }
        }

        val cycleFirst = transactions.sortedWith(
            compareByDescending<TransactionEntity> { it.cycleKey == activeCycleKey }
                .thenByDescending { it.dateMillis }
                .thenByDescending { it.id }
        )

        val q = matchQuery.trim().lowercase(Locale.ENGLISH)
        val isGenericLatest = q.isEmpty() || q == "last" || q == "latest" || q == "recent" || q == "isko" || q == "ye"

        val matchedCatIds = if (!isGenericLatest) {
            categories.filter {
                it.name.lowercase(Locale.ENGLISH).contains(q) ||
                    q.contains(it.name.lowercase(Locale.ENGLISH)) ||
                    it.id.lowercase(Locale.ENGLISH).contains(q)
            }.map { it.id }.toSet()
        } else {
            emptySet()
        }

        if (amountHint > 0.0 && !isGenericLatest) {
            cycleFirst.firstOrNull { tx ->
                abs(tx.amount - amountHint) < 0.01 &&
                    (tx.title.lowercase(Locale.ENGLISH).contains(q) || tx.categoryId in matchedCatIds)
            }?.let { return it }
        }

        if (amountHint > 0.0) {
            cycleFirst.firstOrNull { tx -> abs(tx.amount - amountHint) < 0.01 }?.let { return it }
        }

        if (!isGenericLatest) {
            cycleFirst.firstOrNull { tx ->
                tx.title.lowercase(Locale.ENGLISH).contains(q) ||
                    q.contains(tx.title.lowercase(Locale.ENGLISH)) ||
                    tx.categoryId in matchedCatIds
            }?.let { return it }
        }

        return cycleFirst.firstOrNull()
    }
}
